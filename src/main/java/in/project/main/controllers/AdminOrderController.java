package in.project.main.controllers;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Course;
import in.project.main.entities.Enrollment;
import in.project.main.entities.Orders;
import in.project.main.entities.Payment;
import in.project.main.entities.Refund;
import in.project.main.entities.User;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.PaymentRepository;
import in.project.main.repositories.RefundRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.CourseService;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @GetMapping
    public String listOrders(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("id").descending());
        Page<Orders> ordersPage;

        if (search != null && !search.trim().isEmpty()) {
            ordersPage = ordersRepository.searchOrders(search.trim(), pageable);
        } else {
            ordersPage = ordersRepository.findAll(pageable);
        }

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalRevenue", ordersRepository.calculateTotalRevenue());
        model.addAttribute("totalOrders", ordersRepository.count());

        return "admin/commerce/orders/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Order not found");
            return "redirect:/admin/orders";
        }

        Course course = courseService.getCourseDetails(order.getCourseName());
        User user = userRepository.findByEmail(order.getUserEmail());
        
        Enrollment enrollment = null;
        if (course != null && user != null) {
            enrollment = enrollmentRepository.findByUserEmailAndCourseId(user.getEmail(), course.getId()).orElse(null);
        }

        Refund refund = refundRepository.findByOrderId(order.getOrderId());

        model.addAttribute("order", order);
        model.addAttribute("course", course);
        model.addAttribute("user", user);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("refund", refund);

        return "admin/commerce/orders/detail";
    }

    @PostMapping("/refunds/{id}/approve")
    public String approveRefund(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Refund refund = refundRepository.findById(id).orElse(null);
        if (refund == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Refund ticket not found.");
            return "redirect:/admin/orders";
        }

        Orders order = ordersRepository.findByOrderId(refund.getOrderId());
        if (order != null) {
            order.setStatus("REFUNDED");
            ordersRepository.save(order);

            // Set transaction to REFUNDED
            Payment payment = paymentRepository.findByOrderId(order.getOrderId());
            if (payment != null) {
                payment.setStatus("REFUNDED");
                paymentRepository.save(payment);
            }

            // Revoke enrollment access
            Course course = courseService.getCourseDetails(order.getCourseName());
            if (course != null) {
                Enrollment enrollment = enrollmentRepository.findByUserEmailAndCourseId(order.getUserEmail(), course.getId()).orElse(null);
                if (enrollment != null) {
                    enrollment.setStatus(EnrollmentStatus.CANCELLED);
                    enrollmentRepository.save(enrollment);
                }
            }
        }

        refund.setStatus("APPROVED");
        refund.setRefundDate(in.project.main.util.DateTimeUtil.getCurrentDateTimeFormatted());
        refundRepository.save(refund);

        redirectAttributes.addFlashAttribute("successMsg", "Refund approved successfully and course enrollment revoked.");
        return "redirect:/admin/orders" + (order != null ? "/" + order.getId() : "");
    }

    @PostMapping("/refunds/{id}/reject")
    public String rejectRefund(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Refund refund = refundRepository.findById(id).orElse(null);
        if (refund == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Refund ticket not found.");
            return "redirect:/admin/orders";
        }

        refund.setStatus("REJECTED");
        refundRepository.save(refund);

        Orders order = ordersRepository.findByOrderId(refund.getOrderId());
        redirectAttributes.addFlashAttribute("successMsg", "Refund request rejected successfully.");
        return "redirect:/admin/orders" + (order != null ? "/" + order.getId() : "");
    }
}
