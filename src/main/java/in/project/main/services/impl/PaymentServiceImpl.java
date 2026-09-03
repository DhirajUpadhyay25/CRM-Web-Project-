package in.project.main.services.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.*;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.NotificationType;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.*;
import in.project.main.services.AuditLogService;
import in.project.main.services.CourseService;
import in.project.main.services.NotificationService;
import in.project.main.services.PaymentService;
import in.project.main.util.DateTimeUtil;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Override
    public Page<Payment> getPaymentsPage(String search, String status, String paymentMethod, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim() : null;
        String cleanMethod = (paymentMethod != null && !paymentMethod.trim().isEmpty() && !"ALL".equalsIgnoreCase(paymentMethod.trim())) ? paymentMethod.trim() : null;

        return paymentRepository.searchPayments(cleanSearch, cleanStatus, cleanMethod, pageable);
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElse(null);
    }

    @Override
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public Payment recordManualPayment(String orderId, String userEmail, String courseName, String amount, String method, String notes, String adminEmail) {
        if (orderId == null || orderId.trim().isEmpty()) {
            orderId = "OFFLINE_" + System.currentTimeMillis();
        } else {
            orderId = orderId.trim();
        }

        User user = userRepository.findByEmail(userEmail != null ? userEmail.trim() : "");
        if (user == null) {
            throw new IllegalArgumentException("No registered student found with email: " + userEmail);
        }

        Course course = courseService.getCourseDetails(courseName != null ? courseName.trim() : "");
        if (course == null) {
            throw new IllegalArgumentException("Course not found: " + courseName);
        }

        // 1. Create / Update Order
        Orders order = ordersRepository.findByOrderId(orderId);
        if (order == null) {
            order = new Orders();
            order.setOrderId(orderId);
            order.setUserEmail(user.getEmail());
            order.setCourseName(course.getName());
            order.setCourseAmount(amount != null ? amount.trim() : course.getEffectivePrice().toPlainString());
            order.setDateOfPurchase(DateTimeUtil.getCurrentDateTimeFormatted());
        }
        order.setStatus("COMPLETED");
        order.setPaymentId("MANUAL_" + System.currentTimeMillis());
        ordersRepository.save(order);

        // 2. Create / Update Payment Record
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(orderId);
        }
        payment.setPaymentId(order.getPaymentId());
        payment.setUserEmail(user.getEmail());
        payment.setAmount(order.getCourseAmount());
        payment.setPaymentMethod(method != null ? method.toUpperCase() : "CASH");
        payment.setStatus("SUCCESS");
        payment.setPaymentDate(DateTimeUtil.getCurrentDateTimeFormatted());
        payment.setNotes(notes);
        payment.setVerifiedBy(adminEmail);
        Payment savedPayment = paymentRepository.save(payment);

        // 3. Grant Student Course Enrollment
        Optional<Enrollment> existingOpt = enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId());
        Enrollment enrollment;
        if (existingOpt.isPresent()) {
            enrollment = existingOpt.get();
        } else {
            enrollment = new Enrollment();
            enrollment.setUser(user);
            enrollment.setCourse(course);
        }
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setPaymentStatus("PAID");
        enrollment.setEnrollmentType("PAID");
        enrollment.setEnrollmentSource("ADMIN_MANUAL_PAYMENT");
        enrollment.setOrderId(orderId);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setStartDate(LocalDateTime.now());
        enrollment.setAdminNote("Manual payment recorded by " + adminEmail + (notes != null ? ": " + notes : ""));
        enrollmentRepository.save(enrollment);

        // 4. Record Audit Log
        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    adminEmail,
                    AuditEventType.PAYMENT_SUCCESS,
                    "MANUAL_PAYMENT_RECORDED",
                    "Admin recorded manual payment of ₹" + payment.getAmount() + " (" + payment.getPaymentMethod() + ") for student " + user.getEmail() + " in course '" + course.getName() + "'."
            )
            .withActor(null, adminEmail, "Admin", "ADMIN")
            .withEntity("PAYMENT", String.valueOf(savedPayment.getId()), orderId)
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        // 5. Send In-App Notifications
        try {
            notificationService.sendToUser(
                    user.getEmail(),
                    NotificationType.PAYMENT_SUCCESS,
                    "Payment Received & Course Enrolled",
                    "Your payment of ₹" + payment.getAmount() + " for '" + course.getName() + "' has been confirmed. You now have full access to your learning materials.",
                    "/student/courses/" + course.getId() + "/overview",
                    "ORDER",
                    orderId
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch payment notification: {}", e.getMessage());
        }

        return savedPayment;
    }

    @Override
    public Map<String, Object> getPaymentStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Payment> allPayments = paymentRepository.findAll();
        long totalCount = allPayments.size();
        long successCount = 0;
        long failedCount = 0;
        long refundedCount = 0;
        long pendingCount = 0;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalRefunded = BigDecimal.ZERO;

        for (Payment p : allPayments) {
            String st = p.getStatus() != null ? p.getStatus().toUpperCase() : "PENDING";
            BigDecimal amt = BigDecimal.ZERO;
            try {
                if (p.getAmount() != null && !p.getAmount().trim().isEmpty()) {
                    amt = new BigDecimal(p.getAmount().trim().replaceAll("[^0-9.]", ""));
                }
            } catch (Exception ignored) {}

            if ("SUCCESS".equals(st) || "COMPLETED".equals(st)) {
                successCount++;
                totalCollected = totalCollected.add(amt);
            } else if ("REFUNDED".equals(st)) {
                refundedCount++;
                totalRefunded = totalRefunded.add(amt);
            } else if ("FAILED".equals(st)) {
                failedCount++;
            } else {
                pendingCount++;
            }
        }

        stats.put("totalCount", totalCount);
        stats.put("successCount", successCount);
        stats.put("failedCount", failedCount);
        stats.put("refundedCount", refundedCount);
        stats.put("pendingCount", pendingCount);
        stats.put("totalCollected", totalCollected);
        stats.put("totalRefunded", totalRefunded);

        return stats;
    }
}
