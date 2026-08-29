package in.project.main.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import in.project.main.entities.Notification;
import in.project.main.entities.Orders;
import in.project.main.entities.User;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.NotificationService;

@Controller
@RequestMapping("/student")
public class StudentDashboardController {

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String email = userDetails.getUsername();

        User user = userRepository.findByEmail(email);
        model.addAttribute("user", user);

        long totalEnrolled = enrollmentRepository.countByUserEmailAndStatus(email, EnrollmentStatus.ACTIVE);
        model.addAttribute("totalEnrolled", totalEnrolled);

        long completedCount = enrollmentRepository.countByUserEmailAndStatus(email, EnrollmentStatus.COMPLETED);
        model.addAttribute("completedCount", completedCount);

        Pageable recentOrdersPage = PageRequest.of(0, 5);
        Page<Orders> recentOrders = ordersRepository.findByUserEmailOrderByDateOfPurchaseDesc(email, recentOrdersPage);
        model.addAttribute("recentOrders", recentOrders.getContent());

        long unreadNotifications = notificationService.getUnreadCount(email);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student/dashboard";
    }

    @GetMapping("/orders")
    public String orders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        String email = userDetails.getUsername();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Orders> ordersPage = ordersRepository.findByUserEmailOrderByDateOfPurchaseDesc(email, pageable);

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);

        return "student/orders";
    }

    @GetMapping("/notifications")
    public String notifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        String email = userDetails.getUsername();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Notification> notificationsPage = notificationService.getAllNotifications(email, pageable);

        model.addAttribute("notificationsPage", notificationsPage);
        model.addAttribute("currentPage", page);

        long unreadCount = notificationService.getUnreadCount(email);
        model.addAttribute("unreadCount", unreadCount);

        return "student/notifications";
    }
}
