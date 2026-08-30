package in.project.main.controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import in.project.main.entities.Enrollment;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/upload/";
    private static final String IMAGE_URL = "/upload/";

    private void addCommonStudentAttributes(CustomUserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("studentName", user.getName());
        model.addAttribute("studentEmail", user.getEmail());
        model.addAttribute("studentImage", user.getImageName());
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        addCommonStudentAttributes(userDetails, model);

        long totalEnrolled = enrollmentRepository.countByUserEmailAndStatus(email, EnrollmentStatus.ACTIVE);
        model.addAttribute("enrolledCourses", totalEnrolled);

        long completedCount = enrollmentRepository.countByUserEmailAndStatus(email, EnrollmentStatus.COMPLETED);
        model.addAttribute("completedCourses", completedCount);

        long inProgressCount = totalEnrolled - completedCount;
        model.addAttribute("activeCourses", inProgressCount > 0 ? inProgressCount : 0);

        long totalPurchases = ordersRepository.findByUserEmailOrderByDateOfPurchaseDesc(email, PageRequest.of(0, 1)).getTotalElements();
        model.addAttribute("totalPurchases", totalPurchases);

        Pageable recentOrdersPage = PageRequest.of(0, 5);
        Page<Orders> recentOrders = ordersRepository.findByUserEmailOrderByDateOfPurchaseDesc(email, recentOrdersPage);
        model.addAttribute("recentOrders", recentOrders.getContent());

        List<Notification> recentNotifications = notificationService.getRecentNotifications(email, 5);
        model.addAttribute("recentNotifications", recentNotifications);

        long unreadNotifications = notificationService.getUnreadCount(email);
        model.addAttribute("unreadNotifications", unreadNotifications);

        return "student/dashboard";
    }

    @GetMapping("/courses")
    public String myCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        addCommonStudentAttributes(userDetails, model);

        String email = userDetails.getUsername();
        List<Enrollment> enrollments = enrollmentRepository.findByUserEmailOrderByEnrolledAtDesc(email);
        model.addAttribute("enrollments", enrollments);

        long activeCount = enrollmentRepository.countByUserEmailAndStatus(email, EnrollmentStatus.ACTIVE);
        long completedCount = enrollmentRepository.countByUserEmailAndStatus(email, EnrollmentStatus.COMPLETED);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("completedCount", completedCount);

        return "student/courses";
    }

    @GetMapping("/orders")
    public String orders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        addCommonStudentAttributes(userDetails, model);
        String email = userDetails.getUsername();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Orders> ordersPage = ordersRepository.findByUserEmailOrderByDateOfPurchaseDesc(email, pageable);

        List<Object[]> coursesData = ordersRepository.findCustomerCoursesByEmail(email);
        Map<String, Object[]> courseMap = new java.util.HashMap<>();
        for (Object[] row : coursesData) {
            String courseName = (String) row[1];
            courseMap.put(courseName, row);
        }

        List<Map<String, Object>> orderList = new ArrayList<>();
        for (Orders order : ordersPage.getContent()) {
            Object[] courseInfo = courseMap.get(order.getCourseName());
            Map<String, Object> orderData = new java.util.HashMap<>();
            orderData.put("id", order.getId());
            orderData.put("courseName", order.getCourseName());
            orderData.put("amount", order.getCourseAmount());
            orderData.put("date", order.getDateOfPurchase());
            orderData.put("paymentId", order.getPaymentId());
            orderData.put("orderId", order.getOrderId());
            if (courseInfo != null) {
                orderData.put("courseImageUrl", courseInfo[0]);
                orderData.put("instructorName", courseInfo.length > 5 ? courseInfo[5] : null);
            }
            orderData.put("status", "COMPLETED");
            orderList.add(orderData);
        }

        model.addAttribute("orders", orderList);
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

        addCommonStudentAttributes(userDetails, model);
        String email = userDetails.getUsername();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Notification> notificationsPage = notificationService.getAllNotifications(email, pageable);

        List<Map<String, Object>> notifList = new ArrayList<>();
        for (Notification n : notificationsPage.getContent()) {
            Map<String, Object> notifData = new java.util.HashMap<>();
            notifData.put("id", n.getId());
            notifData.put("title", n.getTitle());
            notifData.put("message", n.getMessage());
            notifData.put("type", n.getType());
            notifData.put("read", n.isRead());
            notifData.put("targetUrl", n.getTargetUrl());
            notifData.put("createdAt", n.getCreatedAt());
            notifData.put("timeAgo", getTimeAgo(n.getCreatedAt()));
            notifList.add(notifData);
        }

        model.addAttribute("notifications", notifList);
        model.addAttribute("notificationsPage", notificationsPage);
        model.addAttribute("currentPage", page);

        long unreadCount = notificationService.getUnreadCount(email);
        model.addAttribute("unreadCount", unreadCount);

        return "student/notifications";
    }

    @GetMapping("/certificates")
    public String certificates(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        addCommonStudentAttributes(userDetails, model);
        String email = userDetails.getUsername();
        List<Enrollment> completedEnrollments = enrollmentRepository.findByUserEmailOrderByEnrolledAtDesc(email)
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .toList();
        model.addAttribute("completedEnrollments", completedEnrollments);
        return "student/certificates";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        addCommonStudentAttributes(userDetails, model);
        return "student/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("name") String name,
            @RequestParam(value = "phoneno", required = false) String phoneno,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "image", required = false) MultipartFile file,
            Model model) {
        try {
            User existingUser = userRepository.findByEmail(userDetails.getUsername());
            existingUser.setName(name);
            existingUser.setPhoneno(phoneno);
            existingUser.setCity(city);

            if (file != null && !file.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File uploadPath = new File(UPLOAD_DIR);
                if (!uploadPath.exists()) uploadPath.mkdirs();
                File saveFile = new File(UPLOAD_DIR + fileName);
                file.transferTo(saveFile);
                existingUser.setImageName(IMAGE_URL + fileName);
            }

            userRepository.save(existingUser);
            model.addAttribute("successMsg", "Profile updated successfully!");
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Failed to update profile.");
        }
        return profile(userDetails, model);
    }

    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        addCommonStudentAttributes(userDetails, model);
        return "student/settings";
    }

    @PostMapping("/settings/change-password")
    public String changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        addCommonStudentAttributes(userDetails, model);
        User user = userRepository.findByEmail(userDetails.getUsername());

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("errorMsg", "Current password is incorrect.");
            return "student/settings";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "New passwords do not match.");
            return "student/settings";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("errorMsg", "New password must be at least 6 characters.");
            return "student/settings";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        model.addAttribute("successMsg", "Password changed successfully!");
        return "student/settings";
    }

    @PostMapping("/api/notifications/{id}/read")
    @ResponseBody
    public void markNotifRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }

    @PostMapping("/api/notifications/mark-all-read")
    @ResponseBody
    public void markAllNotifsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
    }

    @GetMapping("/api/notifications/recent")
    @ResponseBody
    public List<Map<String, Object>> recentNotificationsApi(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getUsername();
        List<Notification> notifications = notificationService.getRecentNotifications(email, 10);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : notifications) {
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("id", n.getId());
            data.put("title", n.getTitle());
            data.put("message", n.getMessage());
            data.put("type", n.getType().name());
            data.put("read", n.isRead());
            data.put("targetUrl", n.getTargetUrl());
            data.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
            result.add(data);
        }
        return result;
    }

    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public Map<String, Long> unreadCountApi(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return Map.of("count", notificationService.getUnreadCount(userDetails.getUsername()));
    }

    private String getTimeAgo(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long seconds = java.time.Duration.between(dateTime, now).getSeconds();
        if (seconds < 60) return "Just now";
        if (seconds < 3600) return (seconds / 60) + " min ago";
        if (seconds < 86400) return (seconds / 3600) + " hr ago";
        if (seconds < 604800) return (seconds / 86400) + " day(s) ago";
        return dateTime.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"));
    }
}
