package in.project.main.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.dto.BulkEnrollmentDTO;
import in.project.main.dto.BulkEnrollmentResultDTO;
import in.project.main.dto.BulkEnrollmentStatusUpdateDTO;
import in.project.main.dto.EnrollmentAnalyticsDTO;
import in.project.main.dto.EnrollmentDTO;
import in.project.main.dto.EnrollmentDetailDTO;
import in.project.main.dto.EnrollmentStatsDTO;
import in.project.main.dto.EnrollmentStatusUpdateDTO;
import in.project.main.dto.ManualEnrollmentDTO;
import in.project.main.entities.AuditLog;
import in.project.main.entities.Certificate;
import in.project.main.entities.Course;
import in.project.main.entities.Enrollment;
import in.project.main.entities.Lesson;
import in.project.main.entities.Orders;
import in.project.main.entities.Payment;
import in.project.main.entities.Quiz;
import in.project.main.entities.User;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.NotificationType;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AssignmentRepository;
import in.project.main.repositories.AssignmentSubmissionRepository;
import in.project.main.repositories.AuditLogRepository;
import in.project.main.repositories.CertificateRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.LessonProgressRepository;
import in.project.main.repositories.LessonRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.PaymentRepository;
import in.project.main.repositories.QuizAttemptRepository;
import in.project.main.repositories.QuizRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.AuditLogService;
import in.project.main.services.EnrollmentService;
import in.project.main.services.NotificationService;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // =========================================================================
    // 1. PAGINATED LISTING & SEARCH
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDTO> getEnrollmentsPage(
            String search,
            EnrollmentStatus status,
            Long courseId,
            String paymentStatus,
            String enrollmentType,
            String enrollmentSource,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        String sanitizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String sanitizedPayment = (paymentStatus != null && !paymentStatus.trim().isEmpty()) ? paymentStatus.trim() : null;
        String sanitizedType = (enrollmentType != null && !enrollmentType.trim().isEmpty()) ? enrollmentType.trim() : null;
        String sanitizedSource = (enrollmentSource != null && !enrollmentSource.trim().isEmpty()) ? enrollmentSource.trim() : null;

        Page<Enrollment> page = enrollmentRepository.searchAndFilter(
                sanitizedSearch,
                status,
                courseId,
                sanitizedPayment,
                sanitizedType,
                sanitizedSource,
                startDate,
                endDate,
                pageable);

        List<EnrollmentDTO> dtos = page.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // =========================================================================
    // 2. ENROLLMENT DETAILS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public EnrollmentDetailDTO getEnrollmentDetails(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Enrollment ID cannot be null");
        }

        Enrollment enrollment = enrollmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with ID: " + id));

        EnrollmentDetailDTO dto = new EnrollmentDetailDTO();
        dto.setId(enrollment.getId());

        // Student Details
        User student = enrollment.getUser();
        if (student != null) {
            dto.setStudentId(student.getId());
            dto.setStudentName(student.getName());
            dto.setStudentEmail(student.getEmail());
            dto.setStudentPhone(student.getPhoneno());
            dto.setStudentCity(student.getCity());
            dto.setStudentAvatar(student.getImageName());
            dto.setStudentBanned(student.isBanStatus());
            dto.setStudentRegisteredDate(student.getCreatedAt() != null ? student.getCreatedAt().format(DATE_FORMATTER) : null);
        }

        // Course Details
        Course course = enrollment.getCourse();
        if (course != null) {
            dto.setCourseId(course.getId());
            dto.setCourseName(course.getName());
            dto.setCourseSlug(course.getSlug());
            dto.setCourseImage(course.getImageUrl());
            dto.setCourseHeadline(course.getShortDescription());
            dto.setCourseLevel(course.getLevel() != null ? course.getLevel().name() : "ALL_LEVELS");
            dto.setCourseLanguage(course.getLanguage());
            dto.setCourseDuration(course.getDuration());
            dto.setCourseOriginalPrice(course.getOriginalPrice());
            dto.setCourseDiscountedPrice(course.getDiscountedPrice());
            dto.setCourseStatus(course.getStatus() != null ? course.getStatus().name() : "UNKNOWN");
            dto.setInstructorName(course.getInstructor());
            dto.setInstructorEmail(course.getInstructorEmail());
            if (course.getCategory() != null) {
                dto.setCategoryName(course.getCategory().getName());
            }
        }

        // Lifecycle & Metadata
        dto.setStatus(enrollment.getStatus());
        dto.setStatusDisplayName(enrollment.getStatus() != null ? enrollment.getStatus().getDisplayName() : "Unknown");
        dto.setStatusBadgeClass(enrollment.getStatus() != null ? enrollment.getStatus().getBadgeClass() : "bg-gray-100 text-gray-700");
        dto.setStatusDotClass(enrollment.getStatus() != null ? enrollment.getStatus().getDotClass() : "bg-gray-400");
        dto.setEnrollmentType(enrollment.getEnrollmentType() != null ? enrollment.getEnrollmentType() : "FREE");
        dto.setEnrollmentSource(enrollment.getEnrollmentSource() != null ? enrollment.getEnrollmentSource() : "DIRECT");
        dto.setEnrolledAt(enrollment.getEnrolledAt());
        dto.setStartDate(enrollment.getStartDate());
        dto.setExpiryDate(enrollment.getExpiryDate());
        dto.setCompletedAt(enrollment.getCompletedAt());
        dto.setLastAccessedAt(enrollment.getLastAccessedAt());
        dto.setUpdatedAt(enrollment.getUpdatedAt());
        dto.setStatusReason(enrollment.getStatusReason());
        dto.setAdminNote(enrollment.getAdminNote());
        dto.setAccessAllowed(enrollment.canAccess());

        // Learning Progress Breakdown
        if (student != null && course != null) {
            List<Lesson> lessons = lessonRepository.findByCourseId(String.valueOf(course.getId()));
            dto.setTotalLessonsCount(lessons.size());
            long completedLessons = lessonProgressRepository.countByUserEmailAndCourseIdAndCompleted(student.getEmail(), course.getId(), true);
            dto.setCompletedLessonsCount(completedLessons);

            int percent = lessons.isEmpty() ? 0 : (int) ((completedLessons * 100) / lessons.size());
            dto.setProgressPercent(percent);

            // Module Count
            Set<String> totalModules = new HashSet<>();
            Set<String> completedModules = new HashSet<>();
            Map<String, List<Lesson>> moduleLessons = new HashMap<>();
            for (Lesson l : lessons) {
                String sec = l.getSectionName() != null ? l.getSectionName() : "General";
                totalModules.add(sec);
                moduleLessons.computeIfAbsent(sec, k -> new ArrayList<>()).add(l);
            }
            for (Map.Entry<String, List<Lesson>> entry : moduleLessons.entrySet()) {
                boolean allComplete = true;
                for (Lesson l : entry.getValue()) {
                    var prog = lessonProgressRepository.findByUserEmailAndLessonId(student.getEmail(), l.getId());
                    if (prog.isEmpty() || !prog.get().isCompleted()) {
                        allComplete = false;
                        break;
                    }
                }
                if (allComplete) {
                    completedModules.add(entry.getKey());
                }
            }
            dto.setTotalModulesCount(totalModules.size());
            dto.setCompletedModulesCount(completedModules.size());

            // Quizzes & Assignments
            List<Quiz> quizzes = quizRepository.findByCourseId(course.getId());
            dto.setTotalQuizzesCount(quizzes.size());
            int passedQuizzes = 0;
            for (Quiz q : quizzes) {
                long passed = quizAttemptRepository.countByUserEmailAndQuizIdAndPassed(student.getEmail(), q.getId(), true);
                if (passed > 0) passedQuizzes++;
            }
            dto.setQuizzesPassedCount(passedQuizzes);

            var assignments = assignmentRepository.findByCourseId(course.getId());
            dto.setTotalAssignmentsCount(assignments.size());
            int submittedAssignments = 0;
            for (var a : assignments) {
                var sub = assignmentSubmissionRepository.findByUserEmailAndAssignmentId(student.getEmail(), a.getId());
                if (sub.isPresent() && !"DRAFT".equalsIgnoreCase(sub.get().getStatus())) {
                    submittedAssignments++;
                }
            }
            dto.setAssignmentsSubmittedCount(submittedAssignments);

            // Certificate
            Certificate cert = certificateRepository.findByEnrollmentId(String.valueOf(enrollment.getId())).orElse(null);
            if (cert != null) {
                dto.setCertificateCode(cert.getCertificateCode());
                dto.setCertificateIssuedDate(cert.getIssueDate());
            }
        }

        // Financial & Order Info
        if (enrollment.getOrderId() != null || (student != null && course != null)) {
            Orders order = null;
            if (enrollment.getOrderId() != null) {
                order = ordersRepository.findByOrderId(enrollment.getOrderId());
            }
            if (order == null && student != null && course != null) {
                var orders = ordersRepository.findByUserEmail(student.getEmail());
                for (Orders o : orders) {
                    if (course.getName().equalsIgnoreCase(o.getCourseName())) {
                        order = o;
                        break;
                    }
                }
            }
            if (order != null) {
                dto.setOrderId(order.getOrderId());
                dto.setPaymentId(order.getPaymentId());
                dto.setTransactionAmount(order.getCourseAmount());
                dto.setPaymentStatus(order.getStatus());
                dto.setPaymentBadgeClass("COMPLETED".equalsIgnoreCase(order.getStatus()) ? "bg-emerald-50 text-emerald-700 border-emerald-200" : "bg-amber-50 text-amber-700 border-amber-200");
                dto.setPaymentDate(order.getDateOfPurchase());
                dto.setCouponCode(order.getCouponCode());
                dto.setDiscountAmount(order.getDiscountAmount());

                Payment payment = paymentRepository.findByOrderId(order.getOrderId());
                if (payment != null) {
                    dto.setPaymentMethod(payment.getPaymentMethod());
                }
            } else {
                dto.setPaymentStatus(enrollment.getPaymentStatus() != null ? enrollment.getPaymentStatus() : "FREE");
                dto.setPaymentBadgeClass(resolvePaymentBadgeClass(enrollment.getPaymentStatus()));
            }
        }

        // Operational Audit & History Timeline
        dto.setHistoryTimeline(getEnrollmentHistory(enrollment.getId()));

        return dto;
    }

    // =========================================================================
    // 3. STATS & ANALYTICS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public EnrollmentStatsDTO getEnrollmentStats() {
        EnrollmentStatsDTO stats = new EnrollmentStatsDTO();
        long total = enrollmentRepository.count();
        stats.setTotalEnrollments(total);
        stats.setActiveEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE));
        stats.setPendingEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.PENDING));
        stats.setCompletedEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.COMPLETED));
        stats.setSuspendedEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.SUSPENDED));
        stats.setCancelledEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.CANCELLED));
        stats.setRevokedEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.REVOKED));
        stats.setExpiredEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.EXPIRED));

        stats.setFreeEnrollments(enrollmentRepository.countByPaymentStatusIgnoreCase("FREE"));
        stats.setPaidEnrollments(enrollmentRepository.countByPaymentStatusIgnoreCase("PAID"));

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().minusDays(30).atStartOfDay();

        stats.setTodayEnrollments(enrollmentRepository.countByEnrolledAtAfter(startOfToday));
        stats.setThisWeekEnrollments(enrollmentRepository.countByEnrolledAtAfter(startOfWeek));
        stats.setThisMonthEnrollments(enrollmentRepository.countByEnrolledAtAfter(startOfMonth));

        if (total > 0) {
            double compRate = ((double) stats.getCompletedEnrollments() / total) * 100.0;
            double cancRate = ((double) (stats.getCancelledEnrollments() + stats.getRevokedEnrollments()) / total) * 100.0;
            stats.setCompletionRate(Math.round(compRate * 10.0) / 10.0);
            stats.setCancellationRate(Math.round(cancRate * 10.0) / 10.0);
        }

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentAnalyticsDTO getEnrollmentAnalytics() {
        EnrollmentAnalyticsDTO analytics = new EnrollmentAnalyticsDTO();
        EnrollmentStatsDTO stats = getEnrollmentStats();
        analytics.setStats(stats);

        // Status Breakdown
        analytics.getStatusBreakdown().add(new EnrollmentAnalyticsDTO.StatusCountItem("ACTIVE", "Active", stats.getActiveEnrollments(), "#10b981"));
        analytics.getStatusBreakdown().add(new EnrollmentAnalyticsDTO.StatusCountItem("COMPLETED", "Completed", stats.getCompletedEnrollments(), "#3b82f6"));
        analytics.getStatusBreakdown().add(new EnrollmentAnalyticsDTO.StatusCountItem("PENDING", "Pending", stats.getPendingEnrollments(), "#f59e0b"));
        analytics.getStatusBreakdown().add(new EnrollmentAnalyticsDTO.StatusCountItem("SUSPENDED", "Suspended", stats.getSuspendedEnrollments(), "#f97316"));
        analytics.getStatusBreakdown().add(new EnrollmentAnalyticsDTO.StatusCountItem("CANCELLED", "Cancelled", stats.getCancelledEnrollments(), "#ef4444"));
        analytics.getStatusBreakdown().add(new EnrollmentAnalyticsDTO.StatusCountItem("REVOKED", "Revoked", stats.getRevokedEnrollments(), "#b91c1c"));
        analytics.getStatusBreakdown().add(new EnrollmentAnalyticsDTO.StatusCountItem("EXPIRED", "Expired", stats.getExpiredEnrollments(), "#6b7280"));

        // Type Breakdown
        analytics.getTypeBreakdown().add(new EnrollmentAnalyticsDTO.TypeCountItem("PAID", "Paid Enrollments", stats.getPaidEnrollments()));
        analytics.getTypeBreakdown().add(new EnrollmentAnalyticsDTO.TypeCountItem("FREE", "Free Enrollments", stats.getFreeEnrollments()));
        long adminAssigned = enrollmentRepository.findAll().stream()
                .filter(e -> "ADMIN_ASSIGNED".equalsIgnoreCase(e.getEnrollmentType()))
                .count();
        analytics.getTypeBreakdown().add(new EnrollmentAnalyticsDTO.TypeCountItem("ADMIN_ASSIGNED", "Admin Assigned", adminAssigned));

        // Top 5 Enrolled Courses
        try {
            List<Object[]> rawTop = enrollmentRepository.findTopEnrolledCoursesRaw();
            int limit = Math.min(5, rawTop.size());
            for (int i = 0; i < limit; i++) {
                Object[] row = rawTop.get(i);
                Long cId = ((Number) row[0]).longValue();
                String cName = (String) row[1];
                long totalCount = ((Number) row[2]).longValue();
                long actCount = enrollmentRepository.countByCourseIdAndStatus(cId, EnrollmentStatus.ACTIVE);
                long compCount = enrollmentRepository.countByCourseIdAndStatus(cId, EnrollmentStatus.COMPLETED);

                Course course = courseRepository.findById(cId).orElse(null);
                String catName = (course != null && course.getCategory() != null) ? course.getCategory().getName() : "General";

                analytics.getTopCourses().add(new EnrollmentAnalyticsDTO.CourseEnrollmentCountItem(cId, cName, catName, totalCount, actCount, compCount));
            }
        } catch (Exception e) {
            log.warn("Could not calculate top enrolled courses: {}", e.getMessage());
        }

        // Daily trend last 30 days
        try {
            LocalDateTime thirtyDaysAgo = LocalDate.now().minusDays(30).atStartOfDay();
            List<Object[]> rawTrends = enrollmentRepository.findDailyEnrollmentCountsSince(thirtyDaysAgo);
            Map<String, Long> dateCountMap = new LinkedHashMap<>();
            for (int d = 30; d >= 0; d--) {
                String dayStr = LocalDate.now().minusDays(d).toString();
                dateCountMap.put(dayStr, 0L);
            }
            for (Object[] r : rawTrends) {
                if (r[0] != null) {
                    String dStr = r[0].toString();
                    long cnt = ((Number) r[1]).longValue();
                    dateCountMap.put(dStr, cnt);
                }
            }
            dateCountMap.forEach((k, v) -> analytics.getTrendLast30Days().add(new EnrollmentAnalyticsDTO.DailyTrendItem(k, v)));
        } catch (Exception e) {
            log.warn("Could not calculate enrollment daily trend: {}", e.getMessage());
        }

        return analytics;
    }

    // =========================================================================
    // 4. MANUAL ENROLLMENT BY ADMIN
    // =========================================================================

    @Override
    @Transactional
    public EnrollmentDTO manualEnrollStudent(ManualEnrollmentDTO dto, String actorEmail) {
        if (dto == null) {
            throw new IllegalArgumentException("Enrollment request cannot be empty");
        }

        // 1. Validate Student
        User student = null;
        if (dto.getStudentId() != null) {
            student = userRepository.findById(dto.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + dto.getStudentId()));
        } else if (dto.getStudentEmail() != null && !dto.getStudentEmail().isBlank()) {
            student = userRepository.findByEmail(dto.getStudentEmail().trim());
            if (student == null) {
                throw new IllegalArgumentException("Student not found with email: " + dto.getStudentEmail());
            }
        } else {
            throw new IllegalArgumentException("Student selection is required");
        }

        if (student.isBanStatus()) {
            throw new IllegalStateException("Cannot enroll banned student (" + student.getEmail() + ").");
        }

        // 2. Validate Course
        if (dto.getCourseId() == null) {
            throw new IllegalArgumentException("Course selection is required");
        }
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + dto.getCourseId()));

        // 3. Duplicate / Re-enrollment Handling
        Optional<Enrollment> existingOpt = enrollmentRepository.findByUserIdAndCourseId(student.getId(), course.getId());
        Enrollment enrollment;

        if (existingOpt.isPresent()) {
            Enrollment existing = existingOpt.get();
            if (existing.getStatus() == EnrollmentStatus.ACTIVE) {
                throw new IllegalStateException("Student '" + student.getName() + "' is already actively enrolled in '" + course.getName() + "'.");
            }

            // Reactivate previous lifecycle while retaining progress
            existing.setStatus(EnrollmentStatus.ACTIVE);
            existing.setEnrolledAt(LocalDateTime.now());
            existing.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDateTime.now());
            existing.setExpiryDate(dto.getExpiryDate());
            existing.setEnrollmentType(dto.getEnrollmentType() != null ? dto.getEnrollmentType() : "ADMIN_ASSIGNED");
            existing.setEnrollmentSource(dto.getEnrollmentSource() != null ? dto.getEnrollmentSource() : "ADMIN_ASSIGNMENT");
            existing.setPaymentStatus("FREE".equalsIgnoreCase(dto.getEnrollmentType()) ? "FREE" : "PAID");
            existing.setStatusReason(null);
            if (dto.getAdminNote() != null && !dto.getAdminNote().isBlank()) {
                existing.setAdminNote(dto.getAdminNote().trim());
            }
            existing.setUpdatedAt(LocalDateTime.now());
            enrollment = enrollmentRepository.save(existing);
            log.info("Reactivated enrollment #{} for student {} in course {}", enrollment.getId(), student.getEmail(), course.getName());
        } else {
            // Create New Enrollment
            enrollment = new Enrollment();
            enrollment.setUser(student);
            enrollment.setCourse(course);
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollment.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDateTime.now());
            enrollment.setExpiryDate(dto.getExpiryDate());
            enrollment.setEnrollmentType(dto.getEnrollmentType() != null ? dto.getEnrollmentType() : "ADMIN_ASSIGNED");
            enrollment.setEnrollmentSource(dto.getEnrollmentSource() != null ? dto.getEnrollmentSource() : "ADMIN_ASSIGNMENT");
            enrollment.setPaymentStatus("FREE".equalsIgnoreCase(dto.getEnrollmentType()) ? "FREE" : "PAID");
            enrollment.setAdminNote(dto.getAdminNote());
            enrollment = enrollmentRepository.save(enrollment);
            log.info("Created new manual enrollment #{} for student {} in course {} by {}", enrollment.getId(), student.getEmail(), course.getName(), actorEmail);
        }

        // 4. Audit Log Recording
        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        actorEmail,
                        AuditEventType.ENROLLMENT_CREATED,
                        "MANUAL_ENROLLMENT",
                        "Admin manually enrolled student '" + student.getName() + "' (" + student.getEmail() + ") into course '" + course.getName() + "'."
                    )
                    .withActor(null, actorEmail, "Administrator", "ADMIN")
                    .withEntity("ENROLLMENT", String.valueOf(enrollment.getId()), course.getName())
                    .withStatus(AuditStatus.SUCCESS)
                    .withSeverity(AuditSeverity.INFO)
                );
            } catch (Exception e) {
                log.warn("Failed to audit manual enrollment: {}", e.getMessage());
            }
        }

        // 5. Notifications
        if (dto.isNotifyStudent()) {
            try {
                notificationService.sendToUser(
                    student.getEmail(),
                    NotificationType.COURSE_ACCESS_GRANTED,
                    "Enrolled in " + course.getName(),
                    "An administrator has enrolled you in '" + course.getName() + "'. You now have full access.",
                    "/student/courses/" + course.getId() + "/player",
                    "COURSE",
                    String.valueOf(course.getId())
                );
            } catch (Exception e) {
                log.warn("Failed to send student enrollment notification: {}", e.getMessage());
            }
        }

        // Notify Instructor
        String instEmail = (course.getInstructorRef() != null) ? course.getInstructorRef().getEmail() : course.getInstructorEmail();
        if (instEmail != null && !instEmail.isBlank()) {
            try {
                notificationService.sendToInstructor(
                    instEmail,
                    NotificationType.COURSE_ENROLLED,
                    "New Student Enrolled",
                    student.getName() + " has been enrolled in your course '" + course.getName() + "'.",
                    "/instructor/students",
                    "COURSE",
                    String.valueOf(course.getId())
                );
            } catch (Exception e) {
                log.warn("Failed to send instructor notification: {}", e.getMessage());
            }
        }

        return toDTO(enrollment);
    }

    // =========================================================================
    // 5. STATUS TRANSITIONS & LIFECYCLE MANAGEMENT
    // =========================================================================

    @Override
    @Transactional
    public EnrollmentDTO updateEnrollmentStatus(Long id, EnrollmentStatusUpdateDTO dto, String actorEmail) {
        if (id == null || dto == null || dto.getStatus() == null) {
            throw new IllegalArgumentException("Invalid status update request");
        }

        Enrollment enrollment = enrollmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with ID: " + id));

        EnrollmentStatus currentStatus = enrollment.getStatus();
        EnrollmentStatus newStatus = dto.getStatus();

        if (currentStatus == newStatus && dto.getExpiryDate() == null) {
            return toDTO(enrollment); // No-op
        }

        // State Machine Transition Validation
        validateStatusTransition(currentStatus, newStatus);

        // Require reason for destructive actions
        if ((newStatus == EnrollmentStatus.SUSPENDED || newStatus == EnrollmentStatus.CANCELLED || newStatus == EnrollmentStatus.REVOKED)
                && (dto.getReason() == null || dto.getReason().trim().isEmpty())) {
            throw new IllegalArgumentException("A mandatory reason is required to " + newStatus.name().toLowerCase() + " an enrollment.");
        }

        enrollment.setStatus(newStatus);
        if (dto.getReason() != null && !dto.getReason().isBlank()) {
            enrollment.setStatusReason(dto.getReason().trim());
        }

        if (newStatus == EnrollmentStatus.COMPLETED) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        if (dto.getExpiryDate() != null) {
            enrollment.setExpiryDate(dto.getExpiryDate());
        }

        enrollment.setUpdatedAt(LocalDateTime.now());
        Enrollment updated = enrollmentRepository.save(enrollment);
        log.info("Updated enrollment #{} status: {} -> {} by {}", id, currentStatus, newStatus, actorEmail);

        // Audit Log
        AuditEventType auditType = mapStatusToAuditEvent(newStatus);
        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        actorEmail,
                        auditType,
                        "STATUS_TRANSITION_" + newStatus.name(),
                        "Changed enrollment #" + id + " status from " + currentStatus.name() + " to " + newStatus.name() + ". Reason: " + (dto.getReason() != null ? dto.getReason() : "N/A")
                    )
                    .withActor(null, actorEmail, "Administrator", "ADMIN")
                    .withEntity("ENROLLMENT", String.valueOf(id), updated.getCourse().getName())
                    .withStatus(AuditStatus.SUCCESS)
                    .withSeverity(newStatus == EnrollmentStatus.REVOKED || newStatus == EnrollmentStatus.SUSPENDED ? AuditSeverity.HIGH : AuditSeverity.INFO)
                    .withChanges(currentStatus.name(), newStatus.name(), "status")
                );
            } catch (Exception e) {
                log.warn("Failed to audit enrollment status change: {}", e.getMessage());
            }
        }

        // Notification to Student
        if (dto.isNotifyStudent() && updated.getUser() != null) {
            try {
                NotificationType notifType = mapStatusToNotificationType(newStatus);
                String title = "Enrollment " + newStatus.getDisplayName();
                String message = resolveStatusNotificationMessage(newStatus, updated.getCourse().getName(), dto.getReason());
                notificationService.sendToUser(
                    updated.getUser().getEmail(),
                    notifType,
                    title,
                    message,
                    "/student/courses",
                    "ENROLLMENT",
                    String.valueOf(id)
                );
            } catch (Exception e) {
                log.warn("Failed to notify student on status change: {}", e.getMessage());
            }
        }

        return toDTO(updated);
    }

    private void validateStatusTransition(EnrollmentStatus current, EnrollmentStatus target) {
        if (current == target) return;

        switch (current) {
            case PENDING:
                if (target == EnrollmentStatus.ACTIVE || target == EnrollmentStatus.CANCELLED) return;
                break;
            case ACTIVE:
                if (target == EnrollmentStatus.COMPLETED || target == EnrollmentStatus.SUSPENDED ||
                    target == EnrollmentStatus.CANCELLED || target == EnrollmentStatus.REVOKED ||
                    target == EnrollmentStatus.EXPIRED) return;
                break;
            case SUSPENDED:
                if (target == EnrollmentStatus.ACTIVE || target == EnrollmentStatus.CANCELLED ||
                    target == EnrollmentStatus.REVOKED) return;
                break;
            case CANCELLED:
            case REVOKED:
            case EXPIRED:
            case COMPLETED:
                if (target == EnrollmentStatus.ACTIVE) return; // Allow reactivation
                break;
        }

        throw new IllegalStateException("Invalid status transition from " + current.name() + " to " + target.name() + ".");
    }

    // =========================================================================
    // 6. BULK ENROLLMENT
    // =========================================================================

    @Override
    @Transactional
    public BulkEnrollmentResultDTO bulkEnrollStudents(BulkEnrollmentDTO dto, String actorEmail) {
        if (dto == null || dto.getCourseId() == null) {
            throw new IllegalArgumentException("Course selection is required for bulk enrollment");
        }

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + dto.getCourseId()));

        Set<String> emailTargets = new HashSet<>();
        if (dto.getStudentEmails() != null) {
            for (String em : dto.getStudentEmails()) {
                if (em != null && !em.isBlank()) emailTargets.add(em.trim().toLowerCase());
            }
        }

        if (dto.getStudentIds() != null && !dto.getStudentIds().isEmpty()) {
            List<User> byIds = userRepository.findAllById(dto.getStudentIds());
            for (User u : byIds) {
                if (u.getEmail() != null) emailTargets.add(u.getEmail().trim().toLowerCase());
            }
        }

        BulkEnrollmentResultDTO result = new BulkEnrollmentResultDTO();
        result.setCourseId(course.getId());
        result.setCourseName(course.getName());
        result.setTotalRequested(emailTargets.size());

        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (String email : emailTargets) {
            try {
                User student = userRepository.findByEmail(email);
                if (student == null) {
                    failed++;
                    result.getResults().add(new BulkEnrollmentResultDTO.ItemResult(email, "Unknown", false, "INVALID_STUDENT", "Student account does not exist", null));
                    continue;
                }

                if (student.isBanStatus()) {
                    failed++;
                    result.getResults().add(new BulkEnrollmentResultDTO.ItemResult(email, student.getName(), false, "FAILED", "Student account is banned", null));
                    continue;
                }

                Optional<Enrollment> existingOpt = enrollmentRepository.findByUserIdAndCourseId(student.getId(), course.getId());
                if (existingOpt.isPresent() && existingOpt.get().getStatus() == EnrollmentStatus.ACTIVE) {
                    skipped++;
                    result.getResults().add(new BulkEnrollmentResultDTO.ItemResult(email, student.getName(), true, "ALREADY_ENROLLED", "Student already has active enrollment", existingOpt.get().getId()));
                    continue;
                }

                Enrollment enrollment;
                if (existingOpt.isPresent()) {
                    enrollment = existingOpt.get();
                    enrollment.setStatus(EnrollmentStatus.ACTIVE);
                    enrollment.setEnrolledAt(LocalDateTime.now());
                    enrollment.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDateTime.now());
                    enrollment.setExpiryDate(dto.getExpiryDate());
                    enrollment.setEnrollmentType(dto.getEnrollmentType() != null ? dto.getEnrollmentType() : "ADMIN_ASSIGNED");
                    enrollment.setEnrollmentSource(dto.getEnrollmentSource() != null ? dto.getEnrollmentSource() : "BULK_ENROLLMENT");
                    enrollment.setPaymentStatus("FREE".equalsIgnoreCase(dto.getEnrollmentType()) ? "FREE" : "PAID");
                    enrollment.setStatusReason(null);
                    if (dto.getAdminNote() != null) enrollment.setAdminNote(dto.getAdminNote());
                } else {
                    enrollment = new Enrollment();
                    enrollment.setUser(student);
                    enrollment.setCourse(course);
                    enrollment.setStatus(EnrollmentStatus.ACTIVE);
                    enrollment.setEnrolledAt(LocalDateTime.now());
                    enrollment.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDateTime.now());
                    enrollment.setExpiryDate(dto.getExpiryDate());
                    enrollment.setEnrollmentType(dto.getEnrollmentType() != null ? dto.getEnrollmentType() : "ADMIN_ASSIGNED");
                    enrollment.setEnrollmentSource(dto.getEnrollmentSource() != null ? dto.getEnrollmentSource() : "BULK_ENROLLMENT");
                    enrollment.setPaymentStatus("FREE".equalsIgnoreCase(dto.getEnrollmentType()) ? "FREE" : "PAID");
                    enrollment.setAdminNote(dto.getAdminNote());
                }

                Enrollment saved = enrollmentRepository.save(enrollment);
                success++;
                result.getResults().add(new BulkEnrollmentResultDTO.ItemResult(email, student.getName(), true, "ENROLLED", "Enrolled successfully", saved.getId()));

                if (dto.isNotifyStudents()) {
                    try {
                        notificationService.sendToUser(
                            student.getEmail(),
                            NotificationType.COURSE_ACCESS_GRANTED,
                            "Enrolled in " + course.getName(),
                            "You have been granted access to '" + course.getName() + "'.",
                            "/student/courses/" + course.getId() + "/player",
                            "COURSE",
                            String.valueOf(course.getId())
                        );
                    } catch (Exception ignored) {}
                }

            } catch (Exception ex) {
                failed++;
                result.getResults().add(new BulkEnrollmentResultDTO.ItemResult(email, "Student", false, "FAILED", ex.getMessage(), null));
            }
        }

        result.setSuccessCount(success);
        result.setSkippedAlreadyEnrolledCount(skipped);
        result.setFailedCount(failed);

        // Bulk Audit
        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        actorEmail,
                        AuditEventType.BULK_ENROLLMENT_CREATED,
                        "BULK_ENROLLMENT",
                        "Bulk enrolled " + success + " students into course '" + course.getName() + "' (Skipped: " + skipped + ", Failed: " + failed + ")."
                    )
                    .withActor(null, actorEmail, "Administrator", "ADMIN")
                    .withEntity("COURSE", String.valueOf(course.getId()), course.getName())
                    .withStatus(AuditStatus.SUCCESS)
                    .withSeverity(AuditSeverity.MEDIUM)
                );
            } catch (Exception e) {
                log.warn("Failed to audit bulk enrollment: {}", e.getMessage());
            }
        }

        return result;
    }

    // =========================================================================
    // 7. BULK STATUS UPDATES
    // =========================================================================

    @Override
    @Transactional
    public BulkEnrollmentResultDTO bulkUpdateStatus(BulkEnrollmentStatusUpdateDTO dto, String actorEmail) {
        if (dto == null || dto.getStatus() == null || dto.getEnrollmentIds() == null || dto.getEnrollmentIds().isEmpty()) {
            throw new IllegalArgumentException("Invalid bulk status change request");
        }

        BulkEnrollmentResultDTO result = new BulkEnrollmentResultDTO();
        result.setTotalRequested(dto.getEnrollmentIds().size());

        int success = 0;
        int failed = 0;

        for (Long id : dto.getEnrollmentIds()) {
            try {
                EnrollmentStatusUpdateDTO singleUpdate = new EnrollmentStatusUpdateDTO(dto.getStatus(), dto.getReason());
                singleUpdate.setExpiryDate(dto.getExpiryDate());
                singleUpdate.setNotifyStudent(dto.isNotifyStudents());
                updateEnrollmentStatus(id, singleUpdate, actorEmail);
                success++;
                result.getResults().add(new BulkEnrollmentResultDTO.ItemResult("ID:" + id, "Enrollment", true, dto.getStatus().name(), "Status updated successfully", id));
            } catch (Exception e) {
                failed++;
                result.getResults().add(new BulkEnrollmentResultDTO.ItemResult("ID:" + id, "Enrollment", false, "FAILED", e.getMessage(), id));
            }
        }

        result.setSuccessCount(success);
        result.setFailedCount(failed);
        return result;
    }

    // =========================================================================
    // 8. CSV EXPORT
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportEnrollmentsToCsv(
            String search,
            EnrollmentStatus status,
            Long courseId,
            String paymentStatus,
            String enrollmentType) {

        String sanitizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String sanitizedPayment = (paymentStatus != null && !paymentStatus.trim().isEmpty()) ? paymentStatus.trim() : null;
        String sanitizedType = (enrollmentType != null && !enrollmentType.trim().isEmpty()) ? enrollmentType.trim() : null;

        List<Enrollment> list = enrollmentRepository.findFilteredForExport(
                sanitizedSearch,
                status,
                courseId,
                sanitizedPayment,
                sanitizedType);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {
            // Write CSV Header
            writer.println("Enrollment ID,Student ID,Student Name,Student Email,Course ID,Course Name,Instructor,Enrollment Type,Enrollment Source,Status,Payment Status,Order ID,Enrolled Date,Start Date,Expiry Date,Completed Date,Progress %");

            for (Enrollment e : list) {
                User u = e.getUser();
                Course c = e.getCourse();

                int progress = 0;
                if (u != null && c != null) {
                    List<Lesson> lessons = lessonRepository.findByCourseId(String.valueOf(c.getId()));
                    if (!lessons.isEmpty()) {
                        long completed = lessonProgressRepository.countByUserEmailAndCourseIdAndCompleted(u.getEmail(), c.getId(), true);
                        progress = (int) ((completed * 100) / lessons.size());
                    }
                }

                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d%%",
                        escapeCsv(String.valueOf(e.getId())),
                        escapeCsv(u != null ? String.valueOf(u.getId()) : ""),
                        escapeCsv(u != null ? u.getName() : ""),
                        escapeCsv(u != null ? u.getEmail() : ""),
                        escapeCsv(c != null ? String.valueOf(c.getId()) : ""),
                        escapeCsv(c != null ? c.getName() : ""),
                        escapeCsv(c != null ? c.getInstructor() : ""),
                        escapeCsv(e.getEnrollmentType() != null ? e.getEnrollmentType() : ""),
                        escapeCsv(e.getEnrollmentSource() != null ? e.getEnrollmentSource() : ""),
                        escapeCsv(e.getStatus() != null ? e.getStatus().name() : ""),
                        escapeCsv(e.getPaymentStatus() != null ? e.getPaymentStatus() : ""),
                        escapeCsv(e.getOrderId() != null ? e.getOrderId() : ""),
                        escapeCsv(e.getEnrolledAt() != null ? e.getEnrolledAt().format(DATE_FORMATTER) : ""),
                        escapeCsv(e.getStartDate() != null ? e.getStartDate().format(DATE_FORMATTER) : ""),
                        escapeCsv(e.getExpiryDate() != null ? e.getExpiryDate().format(DATE_FORMATTER) : ""),
                        escapeCsv(e.getCompletedAt() != null ? e.getCompletedAt().format(DATE_FORMATTER) : ""),
                        progress
                ));
            }
        }

        return baos.toByteArray();
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String clean = value.replace("\"", "\"\"");
        // Neutralize formula injection
        if (clean.startsWith("=") || clean.startsWith("+") || clean.startsWith("-") || clean.startsWith("@")) {
            clean = "'" + clean;
        }
        return "\"" + clean + "\"";
    }

    // =========================================================================
    // 9. COURSE ACCESS ENFORCEMENT
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessCourse(String email, Long courseId) {
        if (email == null || courseId == null) return false;
        User user = userRepository.findByEmail(email.trim());
        if (user == null || user.isBanStatus()) return false;

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUserEmailAndCourseId(email.trim(), courseId);
        if (enrollmentOpt.isEmpty()) return false;

        return enrollmentOpt.get().canAccess();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessCourse(Long userId, Long courseId) {
        if (userId == null || courseId == null) return false;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.isBanStatus()) return false;

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        if (enrollmentOpt.isEmpty()) return false;

        return enrollmentOpt.get().canAccess();
    }

    // =========================================================================
    // 10. ENROLLMENT HISTORY / AUDIT TIMELINE
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDetailDTO.AuditTimelineItemDTO> getEnrollmentHistory(Long enrollmentId) {
        List<EnrollmentDetailDTO.AuditTimelineItemDTO> items = new ArrayList<>();
        if (enrollmentId == null) return items;

        try {
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(l -> ("ENROLLMENT".equalsIgnoreCase(l.getEntityType()) && String.valueOf(enrollmentId).equals(l.getEntityId()))
                            || (l.getDescription() != null && l.getDescription().contains("enrollment #" + enrollmentId)))
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());

            for (AuditLog logItem : logs) {
                String badge = "bg-blue-50 text-blue-700 border-blue-200";
                String icon = "bi-clock-history";
                if (logItem.getAction() != null) {
                    if (logItem.getAction().contains("SUSPEND")) {
                        badge = "bg-orange-50 text-orange-700 border-orange-200";
                        icon = "bi-pause-circle";
                    } else if (logItem.getAction().contains("CANCEL") || logItem.getAction().contains("REVOKE")) {
                        badge = "bg-red-50 text-red-700 border-red-200";
                        icon = "bi-x-circle";
                    } else if (logItem.getAction().contains("ACTIVATE") || logItem.getAction().contains("CREATE") || logItem.getAction().contains("RESUME")) {
                        badge = "bg-emerald-50 text-emerald-700 border-emerald-200";
                        icon = "bi-check-circle";
                    }
                }

                items.add(new EnrollmentDetailDTO.AuditTimelineItemDTO(
                        logItem.getAction() != null ? logItem.getAction() : "AUDIT_EVENT",
                        logItem.getEventType() != null ? logItem.getEventType().name() : "Enrollment Update",
                        logItem.getDescription() != null ? logItem.getDescription() : "",
                        logItem.getActorEmail() != null ? logItem.getActorEmail() : "System",
                        logItem.getCreatedAt() != null ? logItem.getCreatedAt().format(DATE_FORMATTER) : "",
                        badge,
                        icon
                ));
            }
        } catch (Exception e) {
            log.warn("Could not load audit timeline for enrollment #{}: {}", enrollmentId, e.getMessage());
        }

        // Add Initial Creation fallback if timeline is empty
        if (items.isEmpty()) {
            enrollmentRepository.findById(enrollmentId).ifPresent(e -> {
                items.add(new EnrollmentDetailDTO.AuditTimelineItemDTO(
                        "ENROLLMENT_CREATED",
                        "Enrollment Record Created",
                        "Enrolled in course via " + (e.getEnrollmentSource() != null ? e.getEnrollmentSource() : "system"),
                        e.getUser() != null ? e.getUser().getEmail() : "System",
                        e.getEnrolledAt() != null ? e.getEnrolledAt().format(DATE_FORMATTER) : "",
                        "bg-emerald-50 text-emerald-700 border-emerald-200",
                        "bi-check-circle"
                ));
            });
        }

        return items;
    }

    // =========================================================================
    // HELPER CONVERTERS
    // =========================================================================

    private EnrollmentDTO toDTO(Enrollment e) {
        EnrollmentDTO dto = new EnrollmentDTO();
        dto.setId(e.getId());

        User u = e.getUser();
        if (u != null) {
            dto.setStudentId(u.getId());
            dto.setStudentName(u.getName());
            dto.setStudentEmail(u.getEmail());
            dto.setStudentPhone(u.getPhoneno());
            dto.setStudentAvatar(u.getImageName());
            dto.setStudentBanned(u.isBanStatus());
        }

        Course c = e.getCourse();
        if (c != null) {
            dto.setCourseId(c.getId());
            dto.setCourseName(c.getName());
            dto.setCourseSlug(c.getSlug());
            dto.setCourseImage(c.getImageUrl());
            dto.setCourseLevel(c.getLevel() != null ? c.getLevel().name() : "ALL_LEVELS");
            dto.setInstructorName(c.getInstructor());
            dto.setInstructorEmail(c.getInstructorEmail());
            if (c.getCategory() != null) {
                dto.setCategoryName(c.getCategory().getName());
            }

            // Real learning progress
            if (u != null) {
                List<Lesson> lessons = lessonRepository.findByCourseId(String.valueOf(c.getId()));
                dto.setTotalLessons(lessons.size());
                long completed = lessonProgressRepository.countByUserEmailAndCourseIdAndCompleted(u.getEmail(), c.getId(), true);
                dto.setCompletedLessons(completed);
                int percent = lessons.isEmpty() ? 0 : (int) ((completed * 100) / lessons.size());
                dto.setProgressPercent(percent);
            }
        }

        dto.setStatus(e.getStatus());
        dto.setStatusDisplayName(e.getStatus() != null ? e.getStatus().getDisplayName() : "Unknown");
        dto.setStatusBadgeClass(e.getStatus() != null ? e.getStatus().getBadgeClass() : "bg-gray-100 text-gray-700");
        dto.setStatusDotClass(e.getStatus() != null ? e.getStatus().getDotClass() : "bg-gray-400");
        dto.setEnrollmentType(e.getEnrollmentType() != null ? e.getEnrollmentType() : "FREE");
        dto.setEnrollmentSource(e.getEnrollmentSource() != null ? e.getEnrollmentSource() : "DIRECT");
        dto.setPaymentStatus(e.getPaymentStatus() != null ? e.getPaymentStatus() : "FREE");
        dto.setPaymentBadgeClass(resolvePaymentBadgeClass(e.getPaymentStatus()));
        dto.setOrderId(e.getOrderId());
        dto.setEnrolledAt(e.getEnrolledAt());
        dto.setStartDate(e.getStartDate());
        dto.setExpiryDate(e.getExpiryDate());
        dto.setCompletedAt(e.getCompletedAt());
        dto.setLastAccessedAt(e.getLastAccessedAt());
        dto.setAdminNote(e.getAdminNote());
        dto.setStatusReason(e.getStatusReason());
        dto.setAccessAllowed(e.canAccess());

        return dto;
    }

    private String resolvePaymentBadgeClass(String paymentStatus) {
        if (paymentStatus == null) return "bg-gray-50 text-gray-600 border-gray-200";
        switch (paymentStatus.toUpperCase()) {
            case "PAID":
            case "SUCCESS":
            case "COMPLETED":
                return "bg-emerald-50 text-emerald-700 border-emerald-200";
            case "FREE":
                return "bg-blue-50 text-blue-700 border-blue-200";
            case "PENDING":
                return "bg-amber-50 text-amber-700 border-amber-200";
            case "FAILED":
                return "bg-rose-50 text-rose-700 border-rose-200";
            case "REFUNDED":
                return "bg-purple-50 text-purple-700 border-purple-200";
            default:
                return "bg-gray-50 text-gray-600 border-gray-200";
        }
    }

    private AuditEventType mapStatusToAuditEvent(EnrollmentStatus status) {
        switch (status) {
            case ACTIVE: return AuditEventType.ENROLLMENT_ACTIVATED;
            case SUSPENDED: return AuditEventType.ENROLLMENT_SUSPENDED;
            case COMPLETED: return AuditEventType.ENROLLMENT_COMPLETED;
            case CANCELLED: return AuditEventType.ENROLLMENT_CANCELLED;
            case REVOKED: return AuditEventType.ENROLLMENT_REVOKED;
            default: return AuditEventType.ENROLLMENT_CREATED;
        }
    }

    private NotificationType mapStatusToNotificationType(EnrollmentStatus status) {
        switch (status) {
            case ACTIVE: return NotificationType.ENROLLMENT_RESUMED;
            case SUSPENDED: return NotificationType.ENROLLMENT_SUSPENDED;
            case COMPLETED: return NotificationType.ENROLLMENT_COMPLETED;
            case CANCELLED: return NotificationType.ENROLLMENT_CANCELLED;
            case REVOKED: return NotificationType.COURSE_ACCESS_REVOKED;
            default: return NotificationType.COURSE_ENROLLED;
        }
    }

    private String resolveStatusNotificationMessage(EnrollmentStatus status, String courseName, String reason) {
        String base = "Your enrollment in '" + courseName + "' has been " + status.name().toLowerCase() + ".";
        if (reason != null && !reason.isBlank()) {
            base += " Reason: " + reason;
        }
        return base;
    }
}
