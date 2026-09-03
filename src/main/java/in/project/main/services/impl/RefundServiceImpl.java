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
import in.project.main.entities.enums.CertificateStatus;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.NotificationType;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.*;
import in.project.main.services.AuditLogService;
import in.project.main.services.CourseService;
import in.project.main.services.NotificationService;
import in.project.main.services.RefundService;
import in.project.main.util.DateTimeUtil;

@Service
public class RefundServiceImpl implements RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundServiceImpl.class);

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Override
    public Page<Refund> getRefundsPage(String search, String status, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim() : null;

        return refundRepository.searchRefunds(cleanSearch, cleanStatus, pageable);
    }

    @Override
    public Refund getRefundById(Long id) {
        return refundRepository.findById(id).orElse(null);
    }

    @Override
    public Refund getRefundByOrderId(String orderId) {
        return refundRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public Refund requestRefundByStudent(String userEmail, Long orderDbId, String reason, String remarks) {
        Orders order = ordersRepository.findById(orderDbId).orElse(null);
        if (order == null || !userEmail.equalsIgnoreCase(order.getUserEmail())) {
            throw new IllegalArgumentException("Order not found or access denied.");
        }

        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Only completed orders are eligible for refund requests.");
        }

        Refund existing = refundRepository.findByOrderId(order.getOrderId());
        if (existing != null && !"REJECTED".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalStateException("A refund request is already " + existing.getStatus() + " for this order.");
        }

        String fullReason = reason != null ? reason : "Operational refund request";
        if (remarks != null && !remarks.trim().isEmpty()) {
            fullReason += " — " + remarks.trim();
        }

        Refund refund = (existing != null) ? existing : new Refund();
        refund.setOrderId(order.getOrderId());
        refund.setUserEmail(userEmail);
        refund.setCourseName(order.getCourseName());
        refund.setAmount(order.getCourseAmount());
        refund.setReason(fullReason);
        refund.setStatus("PENDING_REVIEW");
        refund.setRefundDate(DateTimeUtil.getCurrentDateTimeFormatted());
        refund.setRequestedAt(DateTimeUtil.getCurrentDateTimeFormatted());
        refund.setAdminNote(null);
        refund.setRejectionReason(null);

        Refund saved = refundRepository.save(refund);

        // Notify Admin of new refund request
        try {
            notificationService.sendToAdmin(
                    NotificationType.PAYMENT_REFUNDED,
                    "New Refund Request Received",
                    "Student " + userEmail + " requested a refund of ₹" + order.getCourseAmount() + " for order #" + order.getOrderId() + " ('" + order.getCourseName() + "').",
                    "/admin/refunds",
                    "REFUND",
                    String.valueOf(saved.getId()),
                    userEmail,
                    userEmail
            );
        } catch (Exception e) {
            log.warn("Failed to notify admin of refund request: {}", e.getMessage());
        }

        // Record Audit Event
        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    userEmail,
                    AuditEventType.PAYMENT_REFUNDED,
                    "REFUND_REQUESTED",
                    "Student " + userEmail + " requested a refund for order #" + order.getOrderId() + " (Reason: " + fullReason + ")."
            )
            .withActor(null, userEmail, userEmail, "STUDENT")
            .withEntity("REFUND", String.valueOf(saved.getId()), order.getOrderId())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        return saved;
    }

    @Override
    @Transactional
    public Refund approveAndProcessRefund(Long refundId, String adminEmail, String adminNote) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found"));

        // 1. Update Refund Record
        refund.setStatus("PROCESSED");
        refund.setProcessedAt(DateTimeUtil.getCurrentDateTimeFormatted());
        refund.setProcessedBy(adminEmail);
        refund.setAdminNote(adminNote);
        refund.setGatewayRefundId("RFND_" + System.currentTimeMillis());
        Refund saved = refundRepository.save(refund);

        // 2. Update Order Status
        Orders order = ordersRepository.findByOrderId(refund.getOrderId());
        if (order != null) {
            order.setStatus("REFUNDED");
            ordersRepository.save(order);
        }

        // 3. Update Payment Status
        Payment payment = paymentRepository.findByOrderId(refund.getOrderId());
        if (payment != null) {
            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);
        }

        // 4. Revoke / Cancel Course Enrollment
        if (order != null && order.getUserEmail() != null) {
            User user = userRepository.findByEmail(order.getUserEmail());
            Course course = courseService.getCourseDetails(order.getCourseName());
            if (user != null && course != null) {
                Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId());
                if (enrollmentOpt.isPresent()) {
                    Enrollment e = enrollmentOpt.get();
                    e.setStatus(EnrollmentStatus.CANCELLED);
                    e.setPaymentStatus("REFUNDED");
                    e.setStatusReason("Enrollment cancelled due to processed refund #" + refund.getId());
                    enrollmentRepository.save(e);
                }

                // Revoke any issued certificate
                certificateRepository.findByStudentEmailAndCourseId(user.getEmail(), course.getId()).ifPresent(cert -> {
                    cert.setStatus(CertificateStatus.REVOKED);
                    cert.setRejectionReason("Refund processed for course order #" + refund.getOrderId());
                    cert.setRevokedAt(LocalDateTime.now());
                    cert.setRevokedByAdmin(adminEmail);
                    certificateRepository.save(cert);
                });
            }
        }

        // 5. Record Audit Log
        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    adminEmail,
                    AuditEventType.PAYMENT_REFUNDED,
                    "REFUND_APPROVED",
                    "Admin " + adminEmail + " approved refund of ₹" + refund.getAmount() + " for order #" + refund.getOrderId() + "."
            )
            .withActor(null, adminEmail, "Admin", "ADMIN")
            .withEntity("REFUND", String.valueOf(saved.getId()), refund.getOrderId())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.MEDIUM);

            auditLogService.record(audit);
        }

        // 6. Notify Student
        try {
            if (refund.getUserEmail() != null) {
                notificationService.sendToUser(
                        refund.getUserEmail(),
                        NotificationType.PAYMENT_REFUNDED,
                        "Refund Processed Successfully",
                        "Your refund of ₹" + refund.getAmount() + " for order #" + refund.getOrderId() + " ('" + refund.getCourseName() + "') has been processed and course access has been revoked.",
                        "/student/orders",
                        "REFUND",
                        String.valueOf(refund.getId())
                );
            }
        } catch (Exception e) {
            log.warn("Failed to notify student of refund approval: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public Refund rejectRefund(Long refundId, String adminEmail, String rejectionReason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found"));

        refund.setStatus("REJECTED");
        refund.setProcessedAt(DateTimeUtil.getCurrentDateTimeFormatted());
        refund.setProcessedBy(adminEmail);
        refund.setRejectionReason(rejectionReason != null ? rejectionReason.trim() : "Request rejected by administration.");
        Refund saved = refundRepository.save(refund);

        // Record Audit Log
        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    adminEmail,
                    AuditEventType.PAYMENT_REFUNDED,
                    "REFUND_REJECTED",
                    "Admin " + adminEmail + " rejected refund request for order #" + refund.getOrderId() + " (Reason: " + refund.getRejectionReason() + ")."
            )
            .withActor(null, adminEmail, "Admin", "ADMIN")
            .withEntity("REFUND", String.valueOf(saved.getId()), refund.getOrderId())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        // Notify Student
        try {
            if (refund.getUserEmail() != null) {
                notificationService.sendToUser(
                        refund.getUserEmail(),
                        NotificationType.PAYMENT_REFUNDED,
                        "Refund Request Declined",
                        "Your refund request for order #" + refund.getOrderId() + " was declined. Reason: " + refund.getRejectionReason(),
                        "/student/orders",
                        "REFUND",
                        String.valueOf(refund.getId())
                );
            }
        } catch (Exception e) {
            log.warn("Failed to notify student of refund rejection: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public Refund adminInitiateRefund(String orderId, String amount, String reason, String adminEmail) {
        Orders order = ordersRepository.findByOrderId(orderId != null ? orderId.trim() : "");
        if (order == null) {
            throw new IllegalArgumentException("Order #" + orderId + " not found");
        }

        Refund refund = refundRepository.findByOrderId(order.getOrderId());
        if (refund == null) {
            refund = new Refund();
            refund.setOrderId(order.getOrderId());
        }

        refund.setUserEmail(order.getUserEmail());
        refund.setCourseName(order.getCourseName());
        refund.setAmount(amount != null && !amount.trim().isEmpty() ? amount.trim() : order.getCourseAmount());
        refund.setReason(reason != null ? reason : "Admin initiated direct refund");
        refund.setStatus("PROCESSED");
        refund.setRefundDate(DateTimeUtil.getCurrentDateTimeFormatted());
        refund.setRequestedAt(DateTimeUtil.getCurrentDateTimeFormatted());
        refund.setProcessedAt(DateTimeUtil.getCurrentDateTimeFormatted());
        refund.setProcessedBy(adminEmail);
        refund.setGatewayRefundId("ADM_RFND_" + System.currentTimeMillis());
        Refund saved = refundRepository.save(refund);

        // Update Order and Payment
        order.setStatus("REFUNDED");
        ordersRepository.save(order);

        Payment payment = paymentRepository.findByOrderId(order.getOrderId());
        if (payment != null) {
            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);
        }

        // Revoke Enrollment and Certificate
        User user = userRepository.findByEmail(order.getUserEmail());
        Course course = courseService.getCourseDetails(order.getCourseName());
        if (user != null && course != null) {
            Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(user.getId(), course.getId());
            if (enrollmentOpt.isPresent()) {
                Enrollment e = enrollmentOpt.get();
                e.setStatus(EnrollmentStatus.CANCELLED);
                e.setPaymentStatus("REFUNDED");
                e.setStatusReason("Admin directly processed refund of ₹" + refund.getAmount());
                enrollmentRepository.save(e);
            }

            certificateRepository.findByStudentEmailAndCourseId(user.getEmail(), course.getId()).ifPresent(cert -> {
                cert.setStatus(CertificateStatus.REVOKED);
                cert.setRejectionReason("Direct refund processed by administration");
                cert.setRevokedAt(LocalDateTime.now());
                cert.setRevokedByAdmin(adminEmail);
                certificateRepository.save(cert);
            });
        }

        // Audit Log
        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    adminEmail,
                    AuditEventType.PAYMENT_REFUNDED,
                    "DIRECT_REFUND_ISSUED",
                    "Admin " + adminEmail + " directly issued refund of ₹" + refund.getAmount() + " for order #" + order.getOrderId() + "."
            )
            .withActor(null, adminEmail, "Admin", "ADMIN")
            .withEntity("REFUND", String.valueOf(saved.getId()), order.getOrderId())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.MEDIUM);

            auditLogService.record(audit);
        }

        // Notify Student
        try {
            if (order.getUserEmail() != null) {
                notificationService.sendToUser(
                        order.getUserEmail(),
                        NotificationType.PAYMENT_REFUNDED,
                        "Refund Issued for Your Order",
                        "A refund of ₹" + refund.getAmount() + " has been issued for order #" + order.getOrderId() + " ('" + order.getCourseName() + "').",
                        "/student/orders",
                        "REFUND",
                        String.valueOf(saved.getId())
                );
            }
        } catch (Exception e) {
            log.warn("Failed to notify student of direct refund: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    public Map<String, Object> getRefundStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Refund> all = refundRepository.findAll();

        long total = all.size();
        long pending = 0;
        long approved = 0;
        long rejected = 0;
        BigDecimal totalRefunded = BigDecimal.ZERO;

        for (Refund r : all) {
            String st = r.getStatus() != null ? r.getStatus().toUpperCase() : "PENDING_REVIEW";
            if ("PROCESSED".equals(st) || "APPROVED".equals(st)) {
                approved++;
                try {
                    if (r.getAmount() != null) {
                        totalRefunded = totalRefunded.add(new BigDecimal(r.getAmount().replaceAll("[^0-9.]", "")));
                    }
                } catch (Exception ignored) {}
            } else if ("REJECTED".equals(st)) {
                rejected++;
            } else {
                pending++;
            }
        }

        stats.put("totalRefunds", total);
        stats.put("pendingRefunds", pending);
        stats.put("approvedRefunds", approved);
        stats.put("rejectedRefunds", rejected);
        stats.put("totalRefundedAmount", totalRefunded);

        return stats;
    }
}
