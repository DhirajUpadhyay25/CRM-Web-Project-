package in.project.main.controllers;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

import in.project.main.entities.Course;
import in.project.main.entities.Enrollment;
import in.project.main.entities.Notification;
import in.project.main.entities.Orders;
import in.project.main.entities.User;
import in.project.main.entities.Lesson;
import in.project.main.entities.LessonProgress;
import in.project.main.entities.Assignment;
import in.project.main.entities.StudentActivity;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.util.DateTimeUtil;
import in.project.main.repositories.LessonProgressRepository;
import in.project.main.repositories.LessonRepository;
import in.project.main.repositories.AssignmentRepository;
import in.project.main.repositories.AssignmentSubmissionRepository;
import in.project.main.repositories.StudentActivityRepository;
import in.project.main.entities.Payment;
import in.project.main.entities.Refund;
import in.project.main.repositories.PaymentRepository;
import in.project.main.repositories.RefundRepository;
import in.project.main.services.CourseService;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.CategoryService;
import in.project.main.services.CourseService;
import in.project.main.services.NotificationService;
import in.project.main.services.LearningService;

@Controller
@RequestMapping("/student")
public class StudentDashboardController {

    @Autowired private LessonProgressRepository lessonProgressRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Autowired private StudentActivityRepository studentActivityRepository;
    @Autowired private LearningService learningService;

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

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CourseRepository courseRepository;

    @org.springframework.beans.factory.annotation.Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

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

        long activeCount = enrollmentRepository.countByUserEmailAndStatus(email, EnrollmentStatus.ACTIVE);
        long completedCount = enrollmentRepository.countByUserEmailAndStatus(email, EnrollmentStatus.COMPLETED);
        long totalEnrolled = activeCount + completedCount;

        // A student with no enrollments sees an empty dashboard. This used to call
        // seedStudentPanelData() instead, which fabricated enrollments, orders and
        // certificates for every user in the database on an ordinary page load.

        model.addAttribute("totalEnrolled", totalEnrolled);
        model.addAttribute("activeCourses", activeCount);
        model.addAttribute("completedCourses", completedCount);

        long totalPurchases = ordersRepository.findByUserEmailOrderByDateOfPurchaseDesc(email, PageRequest.of(0, 1)).getTotalElements();
        model.addAttribute("totalPurchases", totalPurchases);

        Pageable recentOrdersPage = PageRequest.of(0, 5);
        Page<Orders> recentOrders = ordersRepository.findByUserEmailOrderByDateOfPurchaseDesc(email, recentOrdersPage);
        model.addAttribute("recentOrders", recentOrders.getContent());

        List<Notification> recentNotifications = notificationService.getRecentNotifications(email, 5);
        List<Map<String, Object>> processedNotifications = new ArrayList<>();
        for (Notification n : recentNotifications) {
            Map<String, Object> notifMap = new java.util.HashMap<>();
            notifMap.put("id", n.getId());
            notifMap.put("title", n.getTitle());
            notifMap.put("message", n.getMessage());
            notifMap.put("type", n.getType() != null ? n.getType().name() : "SYSTEM");
            notifMap.put("read", n.isRead());
            notifMap.put("targetUrl", n.getTargetUrl());
            notifMap.put("timeAgo", getTimeAgo(n.getCreatedAt()));
            processedNotifications.add(notifMap);
        }
        model.addAttribute("recentNotifications", processedNotifications);

        long unreadNotifications = notificationService.getUnreadCount(email);
        model.addAttribute("unreadNotifications", unreadNotifications);

        // Continue Learning Engine
        List<Enrollment> activeEnrollments = enrollmentRepository.findByUserEmailOrderByEnrolledAtDesc(email)
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .toList();

        Course continueCourse = null;
        Lesson continueLesson = null;
        String continueModule = null;
        int continueProgress = 0;

        LessonProgress latestProgress = null;
        for (Enrollment e : activeEnrollments) {
            LessonProgress lp = lessonProgressRepository.findFirstByUserEmailAndCourseIdOrderByLastAccessedAtDesc(email, e.getCourse().getId());
            if (lp != null) {
                if (latestProgress == null || lp.getLastAccessedAt().isAfter(latestProgress.getLastAccessedAt())) {
                    latestProgress = lp;
                    continueCourse = e.getCourse();
                }
            }
        }

        if (continueCourse != null && latestProgress != null) {
            continueLesson = lessonRepository.findById(latestProgress.getLessonId()).orElse(null);
            continueModule = continueLesson != null ? continueLesson.getSectionName() : "Getting Started";
            continueProgress = learningService.getCourseProgressPercent(email, continueCourse.getId());
        } else if (!activeEnrollments.isEmpty()) {
            continueCourse = activeEnrollments.get(0).getCourse();
            List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(String.valueOf(continueCourse.getId()));
            if (!courseLessons.isEmpty()) {
                continueLesson = courseLessons.get(0);
                continueModule = continueLesson.getSectionName();
            }
            continueProgress = 0;
        }

        model.addAttribute("continueCourse", continueCourse);
        model.addAttribute("continueLesson", continueLesson);
        model.addAttribute("continueModule", continueModule != null ? continueModule : "Getting Started");
        model.addAttribute("continueProgress", continueProgress);

        // Upcoming Deadlines
        List<Long> activeCourseIds = activeEnrollments.stream().map(e -> e.getCourse().getId()).toList();
        List<Map<String, Object>> upcomingDeadlines = new ArrayList<>();
        if (!activeCourseIds.isEmpty()) {
            List<Assignment> assignments = assignmentRepository.findByCourseIdIn(activeCourseIds);
            List<Assignment> pendingAssignments = new ArrayList<>();
            for (Assignment a : assignments) {
                if (a.getDueDate().isAfter(LocalDateTime.now())) {
                    boolean submitted = assignmentSubmissionRepository.findByUserEmailAndAssignmentId(email, a.getId()).isPresent();
                    if (!submitted) {
                        pendingAssignments.add(a);
                    }
                }
            }
            pendingAssignments.sort(Comparator.comparing(Assignment::getDueDate));
            for (int i = 0; i < Math.min(3, pendingAssignments.size()); i++) {
                Assignment a = pendingAssignments.get(i);
                Course c = courseRepository.findById(a.getCourseId()).orElse(null);
                Map<String, Object> map = new HashMap<>();
                map.put("id", a.getId());
                map.put("title", a.getTitle());
                map.put("dueDate", a.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
                map.put("courseName", c != null ? c.getName() : "Course");
                upcomingDeadlines.add(map);
            }
        }
        model.addAttribute("upcomingDeadlines", upcomingDeadlines);

        // Recent Activity Timeline
        List<StudentActivity> activities = studentActivityRepository.findByUserEmailOrderByCreatedAtDesc(email, PageRequest.of(0, 5));
        List<Map<String, Object>> formattedActs = new ArrayList<>();
        for (StudentActivity sa : activities) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", sa.getActivityType());
            map.put("description", sa.getDescription());
            map.put("timeStr", sa.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a")));
            formattedActs.add(map);
        }
        model.addAttribute("recentActivities", formattedActs);

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
            orderData.put("status", order.getStatus() != null ? order.getStatus() : "COMPLETED");
            orderList.add(orderData);
        }

        model.addAttribute("orders", orderList);
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);

        return "student/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        addCommonStudentAttributes(userDetails, model);
        String email = userDetails.getUsername();
        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null || !email.equals(order.getUserEmail())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Order not found or access denied.");
            return "redirect:/student/orders";
        }

        Course course = courseService.getCourseDetails(order.getCourseName());
        Enrollment enrollment = null;
        if (course != null) {
            User user = userRepository.findByEmail(email);
            if (user != null) {
                enrollment = enrollmentRepository.findByUserEmailAndCourseId(email, course.getId()).orElse(null);
            }
        }

        Refund refund = refundRepository.findByOrderId(order.getOrderId());

        model.addAttribute("order", order);
        model.addAttribute("course", course);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("refund", refund);

        return "student/order-detail";
    }

    @GetMapping("/orders/{id}/invoice")
    public String downloadInvoice(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();
        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null || !email.equals(order.getUserEmail())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Order not found or access denied.");
            return "redirect:/student/orders";
        }

        Course course = courseService.getCourseDetails(order.getCourseName());
        User user = userRepository.findByEmail(email);

        model.addAttribute("order", order);
        model.addAttribute("course", course);
        model.addAttribute("user", user);

        return "student/invoice";
    }

    @PostMapping("/orders/{id}/refund")
    public String requestRefund(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();
        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null || !email.equals(order.getUserEmail())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Order not found or access denied.");
            return "redirect:/student/orders";
        }

        if (!"COMPLETED".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Only completed orders are eligible for refunds.");
            return "redirect:/student/orders/" + id;
        }

        Refund existingRefund = refundRepository.findByOrderId(order.getOrderId());
        if (existingRefund != null) {
            redirectAttributes.addFlashAttribute("errorMsg", "A refund has already been requested for this order.");
            return "redirect:/student/orders/" + id;
        }

        Refund refund = new Refund();
        refund.setOrderId(order.getOrderId());
        refund.setAmount(order.getCourseAmount());
        refund.setReason(reason);
        refund.setStatus("PENDING");
        refund.setRefundDate(DateTimeUtil.getCurrentDateTimeFormatted());
        refundRepository.save(refund);

        redirectAttributes.addFlashAttribute("successMsg", "Refund requested successfully. It is pending admin approval.");
        return "redirect:/student/orders/" + id;
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

    @GetMapping("/browse")
    public String browseCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "level", required = false) CourseLevel level,
            @RequestParam(name = "pricingType", required = false) String pricingType,
            @RequestParam(name = "sort", defaultValue = "newest") String sort) {

        addCommonStudentAttributes(userDetails, model);
        String email = userDetails.getUsername();

        org.springframework.data.domain.Sort sortObj;
        if ("price_asc".equalsIgnoreCase(sort)) {
            sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "originalPrice");
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "originalPrice");
        } else if ("title_asc".equalsIgnoreCase(sort)) {
            sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name");
        } else {
            sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);
        Page<Course> coursesPage = courseService.getPublicStorefrontCourses(keyword, categoryId, level, pricingType, pageable);

        List<Long> purchasedCourseIds = new ArrayList<>();
        for (Course course : coursesPage.getContent()) {
            if (ordersRepository.existsByUserEmailAndCourseName(email, course.getName())) {
                purchasedCourseIds.add(course.getId());
            }
        }

        model.addAttribute("coursesPage", coursesPage);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("levels", CourseLevel.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("level", level);
        model.addAttribute("pricingType", pricingType);
        model.addAttribute("sort", sort);
        model.addAttribute("purchasedCourseIds", purchasedCourseIds);
        model.addAttribute("razorpayKeyId", razorpayKeyId);

        return "student/browse";
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
