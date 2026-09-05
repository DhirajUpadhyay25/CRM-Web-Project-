package in.project.main.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Course;
import in.project.main.entities.Employee;
import in.project.main.entities.Enrollment;
import in.project.main.entities.Feedback;
import in.project.main.entities.FeedbackNote;
import in.project.main.entities.FeedbackResponse;
import in.project.main.entities.FeedbackStatusHistory;
import in.project.main.entities.Instructor;
import in.project.main.entities.User;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.FeedbackStatus;
import in.project.main.entities.enums.NotificationType;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.FeedbackNoteRepository;
import in.project.main.repositories.FeedbackRepository;
import in.project.main.repositories.FeedbackResponseRepository;
import in.project.main.repositories.FeedbackStatusHistoryRepository;
import in.project.main.repositories.InstructorRepository;
import in.project.main.repositories.UserRepository;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    @Autowired private FeedbackRepository feedbackRepository;
    @Autowired private FeedbackResponseRepository feedbackResponseRepository;
    @Autowired private FeedbackNoteRepository feedbackNoteRepository;
    @Autowired private FeedbackStatusHistoryRepository feedbackStatusHistoryRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private InstructorRepository instructorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditLogService auditLogService;

    // ==========================================
    // STUDENT: Submit Feedback
    // ==========================================

    @Transactional
    public Feedback submitFeedback(Long studentId, Long courseId, Long enrollmentId,
                                    Integer rating, String category, String subject,
                                    String message, Boolean isAnonymous, Boolean isPublic) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        Enrollment enrollment = null;
        if (enrollmentId != null) {
            enrollment = enrollmentRepository.findById(enrollmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        } else {
            enrollment = enrollmentRepository.findByUserIdAndCourseId(studentId, courseId).orElse(null);
        }

        // Validate enrollment belongs to student and course
        if (enrollment != null) {
            if (!enrollment.getUser().getId().equals(studentId)) {
                throw new SecurityException("You are not authorized to submit feedback for this enrollment");
            }
            if (!enrollment.getCourse().getId().equals(courseId)) {
                throw new IllegalArgumentException("Enrollment does not belong to this course");
            }
        }

        // Check duplicate: one feedback per student per course
        Optional<Feedback> existing = feedbackRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("You have already submitted feedback for this course");
        }

        // Validate rating
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Determine instructor from course
        Instructor instructor = null;
        if (course.getInstructorRef() != null) {
            instructor = course.getInstructorRef();
        }

        Feedback feedback = new Feedback();
        feedback.setStudent(student);
        feedback.setUserName(student.getName());
        feedback.setUserEmail(student.getEmail());
        feedback.setCourse(course);
        feedback.setInstructor(instructor);
        feedback.setEnrollment(enrollment);
        feedback.setFeedbackType(in.project.main.entities.enums.FeedbackType.COURSE);
        feedback.setRating(rating);
        feedback.setCategory(category);
        feedback.setSubject(subject);
        feedback.setMessage(message != null ? message.trim() : null);
        feedback.setAnonymous(Boolean.TRUE.equals(isAnonymous));
        feedback.setPublic(Boolean.TRUE.equals(isPublic));
        feedback.setStatus(FeedbackStatus.NEW);
        feedback.setDateOfFeedback(LocalDateTime.now().toLocalDate().toString());
        feedback.setTimeOfFeedback(LocalDateTime.now().toLocalTime().toString().substring(0, 8));

        Feedback saved = feedbackRepository.save(feedback);

        // Notify admins
        try {
            String courseName = course.getName() != null ? course.getName() : "Unknown Course";
            notificationService.sendToAdmins(
                NotificationType.FEEDBACK_RECEIVED,
                "New Feedback Received",
                student.getName() + " submitted feedback for \"" + courseName + "\" (Rating: " + rating + "/5)",
                "/admin/feedback/" + saved.getId(),
                "FEEDBACK",
                String.valueOf(saved.getId())
            );

            // Low rating alert
            if (rating <= 2) {
                notificationService.sendToAdmins(
                    NotificationType.LOW_RATING_ALERT,
                    "Low Rating Alert",
                    "Feedback #" + saved.getId() + " received a " + rating + "-star rating for \"" + courseName + "\"",
                    "/admin/feedback/" + saved.getId(),
                    "FEEDBACK",
                    String.valueOf(saved.getId())
                );
            }
        } catch (Exception e) {
            log.warn("Could not send feedback notification: {}", e.getMessage());
        }

        // Audit log
        recordAudit(student.getEmail(), student.getName(), AuditEventType.FEEDBACK_CREATED,
                "Feedback submitted for course: " + (course.getName() != null ? course.getName() : courseId),
                saved.getId(), AuditStatus.SUCCESS);

        return saved;
    }

    // ==========================================
    // STUDENT: Get own feedback list
    // ==========================================

    @Transactional(readOnly = true)
    public Page<Feedback> getStudentFeedback(Long studentId, Pageable pageable) {
        return feedbackRepository.findByStudentIdAndDeletedFalse(studentId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Feedback> getStudentFeedbackList(Long studentId) {
        return feedbackRepository.findByStudentIdAndDeletedFalse(studentId);
    }

    @Transactional(readOnly = true)
    public Optional<Feedback> getStudentFeedbackById(Long feedbackId, Long studentId) {
        Optional<Feedback> feedback = feedbackRepository.findById(feedbackId);
        if (feedback.isPresent() && feedback.get().getStudent() != null
                && feedback.get().getStudent().getId().equals(studentId)
                && !feedback.get().isDeleted()) {
            return feedback;
        }
        return Optional.empty();
    }

    // ==========================================
    // STUDENT: Check if already submitted
    // ==========================================

    @Transactional(readOnly = true)
    public boolean hasStudentSubmittedFeedback(Long studentId, Long courseId) {
        return feedbackRepository.findByStudentIdAndCourseIdAndDeletedFalse(studentId, courseId).isPresent();
    }

    // ==========================================
    // STUDENT: Get eligible courses
    // ==========================================

    @Transactional(readOnly = true)
    public List<Enrollment> getStudentEligibleCourses(Long studentId) {
        return enrollmentRepository.findByUserIdWithCourse(studentId);
    }

    // ==========================================
    // STUDENT: Edit feedback
    // ==========================================

    @Transactional
    public Feedback editFeedback(Long feedbackId, Long studentId, Integer rating,
                                  String category, String subject, String message) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));

        if (feedback.getStudent() == null || !feedback.getStudent().getId().equals(studentId)) {
            throw new SecurityException("You are not authorized to edit this feedback");
        }

        if (feedback.getStatus() != null && !feedback.getStatus().isEditable()) {
            throw new IllegalStateException("Feedback can no longer be edited in its current status");
        }

        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        if (rating != null) feedback.setRating(rating);
        if (category != null) feedback.setCategory(category);
        if (subject != null) feedback.setSubject(subject);
        if (message != null) feedback.setMessage(message.trim());

        Feedback saved = feedbackRepository.save(feedback);

        recordAudit(feedback.getUserEmail(), feedback.getUserName(),
                AuditEventType.FEEDBACK_UPDATED, "Feedback #" + feedbackId + " updated by student",
                feedbackId, AuditStatus.SUCCESS);

        return saved;
    }

    // ==========================================
    // STUDENT: Delete/Withdraw feedback
    // ==========================================

    @Transactional
    public void deleteFeedback(Long feedbackId, Long studentId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));

        if (feedback.getStudent() == null || !feedback.getStudent().getId().equals(studentId)) {
            throw new SecurityException("You are not authorized to delete this feedback");
        }

        feedback.setDeleted(true);
        feedbackRepository.save(feedback);

        recordAudit(feedback.getUserEmail(), feedback.getUserName(),
                AuditEventType.FEEDBACK_DELETED, "Feedback #" + feedbackId + " withdrawn by student",
                feedbackId, AuditStatus.SUCCESS);
    }

    // ==========================================
    // ADMIN: Search and filter
    // ==========================================

    @Transactional(readOnly = true)
    public Page<Feedback> adminSearchAndFilter(String keyword, FeedbackStatus status,
                                                Integer rating, String category,
                                                Long courseId, Long instructorId,
                                                Integer minRating, LocalDateTime startDate,
                                                LocalDateTime endDate, Pageable pageable) {
        return feedbackRepository.adminSearchAndFilter(
                keyword, status, rating, category, courseId, instructorId,
                minRating, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Feedback> getFeedbackById(Long id) {
        return feedbackRepository.findById(id);
    }

    // ==========================================
    // ADMIN: Respond to feedback
    // ==========================================

    @Transactional
    public FeedbackResponse respondToFeedback(Long feedbackId, String responderEmail,
                                               String responderName, String responderRole,
                                               String message) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));

        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Response message cannot be empty");
        }

        FeedbackResponse response = new FeedbackResponse();
        response.setFeedback(feedback);
        response.setResponderEmail(responderEmail);
        response.setResponderName(responderName);
        response.setResponderRole(responderRole);
        response.setMessage(message.trim());

        FeedbackResponse saved = feedbackResponseRepository.save(response);

        // Update feedback status
        FeedbackStatus oldStatus = feedback.getStatus();
        feedback.setStatus(FeedbackStatus.RESPONDED);
        feedback.setAdminResponse(message.trim());
        feedbackRepository.save(feedback);

        // Record status change
        recordStatusChange(feedbackId, oldStatus, FeedbackStatus.RESPONDED, responderEmail);

        // Notify student
        try {
            if (feedback.getStudent() != null && feedback.getStudent().getEmail() != null) {
                String courseName = feedback.getCourse() != null ? feedback.getCourse().getName() : "your course";
                notificationService.sendToStudent(
                    feedback.getStudent().getEmail(),
                    NotificationType.FEEDBACK_RESPONDED,
                    "Feedback Response Available",
                    "Your feedback for \"" + courseName + "\" has received a response.",
                    "/student/feedback/" + feedbackId,
                    "FEEDBACK",
                    String.valueOf(feedbackId)
                );
            }
        } catch (Exception e) {
            log.warn("Could not send feedback response notification: {}", e.getMessage());
        }

        // Audit log
        recordAudit(responderEmail, responderName, AuditEventType.FEEDBACK_RESPONDED,
                "Response added to feedback #" + feedbackId, feedbackId, AuditStatus.SUCCESS);

        return saved;
    }

    // ==========================================
    // ADMIN: Update status
    // ==========================================

    @Transactional
    public void updateFeedbackStatus(Long feedbackId, FeedbackStatus newStatus,
                                      String changedBy, String reason) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));

        FeedbackStatus oldStatus = feedback.getStatus();
        feedback.setStatus(newStatus);

        if (newStatus == FeedbackStatus.RESOLVED) {
            feedback.setResolvedAt(LocalDateTime.now());
        }

        feedbackRepository.save(feedback);
        recordStatusChange(feedbackId, oldStatus, newStatus, changedBy);

        // Notify student on resolve
        try {
            if (newStatus == FeedbackStatus.RESOLVED && feedback.getStudent() != null
                    && feedback.getStudent().getEmail() != null) {
                String courseName = feedback.getCourse() != null ? feedback.getCourse().getName() : "your course";
                notificationService.sendToStudent(
                    feedback.getStudent().getEmail(),
                    NotificationType.FEEDBACK_RESOLVED,
                    "Feedback Resolved",
                    "Your feedback for \"" + courseName + "\" has been resolved.",
                    "/student/feedback/" + feedbackId,
                    "FEEDBACK",
                    String.valueOf(feedbackId)
                );
            }
        } catch (Exception e) {
            log.warn("Could not send feedback resolved notification: {}", e.getMessage());
        }

        recordAudit(changedBy, changedBy, AuditEventType.FEEDBACK_STATUS_CHANGED,
                "Feedback #" + feedbackId + " status changed from " + oldStatus + " to " + newStatus,
                feedbackId, AuditStatus.SUCCESS);
    }

    // ==========================================
    // ADMIN: Assign feedback
    // ==========================================

    @Transactional
    public void assignFeedback(Long feedbackId, Long adminId, String adminEmail) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));

        feedback.setAssignedAdminId(adminId);
        feedback.setAssignedAdminEmail(adminEmail);
        feedback.setStatus(FeedbackStatus.UNDER_REVIEW);
        feedbackRepository.save(feedback);

        recordAudit(adminEmail, adminEmail, AuditEventType.FEEDBACK_STATUS_CHANGED,
                "Feedback #" + feedbackId + " assigned to " + adminEmail,
                feedbackId, AuditStatus.SUCCESS);
    }

    // ==========================================
    // ADMIN: Add internal note
    // ==========================================

    @Transactional
    public FeedbackNote addInternalNote(Long feedbackId, String adminEmail, String adminName, String note) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found"));

        FeedbackNote feedbackNote = new FeedbackNote();
        feedbackNote.setFeedbackId(feedbackId);
        feedbackNote.setAdminEmail(adminEmail);
        feedbackNote.setAdminName(adminName);
        feedbackNote.setNote(note.trim());

        return feedbackNoteRepository.save(feedbackNote);
    }

    @Transactional(readOnly = true)
    public List<FeedbackNote> getFeedbackNotes(Long feedbackId) {
        return feedbackNoteRepository.findByFeedbackIdOrderByCreatedAtDesc(feedbackId);
    }

    // ==========================================
    // ADMIN: Get status history
    // ==========================================

    @Transactional(readOnly = true)
    public List<FeedbackStatusHistory> getStatusHistory(Long feedbackId) {
        return feedbackStatusHistoryRepository.findByFeedbackIdOrderByCreatedAtDesc(feedbackId);
    }

    // ==========================================
    // INSTRUCTOR: Search and filter
    // ==========================================

    @Transactional(readOnly = true)
    public Page<Feedback> instructorSearchAndFilter(Long instructorId, String instructorEmail,
                                                     String keyword, FeedbackStatus status,
                                                     Integer rating, String category,
                                                     Long courseId, Pageable pageable) {
        return feedbackRepository.instructorSearchAndFilter(
                instructorId, instructorEmail, keyword, status, rating, category, courseId, pageable);
    }

    // ==========================================
    // ANALYTICS
    // ==========================================

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalCount", feedbackRepository.countByDeletedFalse());
        analytics.put("averageRating", feedbackRepository.findAverageRating());
        analytics.put("newCount", feedbackRepository.countByStatusAndDeletedFalse(FeedbackStatus.NEW));
        analytics.put("underReviewCount", feedbackRepository.countByStatusAndDeletedFalse(FeedbackStatus.UNDER_REVIEW));
        analytics.put("inProgressCount", feedbackRepository.countByStatusAndDeletedFalse(FeedbackStatus.IN_PROGRESS));
        analytics.put("respondedCount", feedbackRepository.countByStatusAndDeletedFalse(FeedbackStatus.RESPONDED));
        analytics.put("resolvedCount", feedbackRepository.countByStatusAndDeletedFalse(FeedbackStatus.RESOLVED));
        analytics.put("closedCount", feedbackRepository.countByStatusAndDeletedFalse(FeedbackStatus.CLOSED));

        List<Object[]> distribution = feedbackRepository.findRatingDistribution();
        Map<Integer, Long> ratingDist = new HashMap<>();
        for (Object[] row : distribution) {
            ratingDist.put((Integer) row[0], (Long) row[1]);
        }
        analytics.put("ratingDistribution", ratingDist);

        List<Object[]> typeCounts = feedbackRepository.countByFeedbackType();
        Map<String, Long> typeMap = new HashMap<>();
        for (Object[] row : typeCounts) {
            typeMap.put(String.valueOf(row[0]), (Long) row[1]);
        }
        analytics.put("feedbackByType", typeMap);

        List<Object[]> categoryCounts = feedbackRepository.countByCategory();
        Map<String, Long> categoryMap = new HashMap<>();
        for (Object[] row : categoryCounts) {
            categoryMap.put(String.valueOf(row[0]), (Long) row[1]);
        }
        analytics.put("feedbackByCategory", categoryMap);

        long needsAttention = feedbackRepository.countByDeletedFalseAndStatusIn(
                java.util.List.of(FeedbackStatus.NEW, FeedbackStatus.UNDER_REVIEW));
        analytics.put("needsAttentionCount", needsAttention);

        return analytics;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCourseRatingSummary(Long courseId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("averageRating", feedbackRepository.findAverageRatingByCourseId(courseId));
        summary.put("totalRatings", feedbackRepository.countRatingsByCourseId(courseId));

        List<Object[]> distribution = feedbackRepository.findRatingDistributionByCourseId(courseId);
        Map<Integer, Long> ratingDist = new HashMap<>();
        for (Object[] row : distribution) {
            ratingDist.put((Integer) row[0], (Long) row[1]);
        }
        summary.put("ratingDistribution", ratingDist);
        return summary;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInstructorRatingSummary(Long instructorId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("averageRating", feedbackRepository.findAverageRatingByInstructorId(instructorId));
        summary.put("totalRatings", feedbackRepository.countRatingsByInstructorId(instructorId));
        return summary;
    }

    // ==========================================
    // Low rating alerts
    // ==========================================

    @Transactional(readOnly = true)
    public List<Feedback> getLowRatingFeedback(int maxRating, Pageable pageable) {
        return feedbackRepository.findLowRatingFeedback(maxRating, pageable);
    }

    // ==========================================
    // Get responses for feedback
    // ==========================================

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbackResponses(Long feedbackId) {
        return feedbackResponseRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId);
    }

    // ==========================================
    // Helpers
    // ==========================================

    private void recordStatusChange(Long feedbackId, FeedbackStatus from, FeedbackStatus to, String changedBy) {
        try {
            FeedbackStatusHistory history = new FeedbackStatusHistory();
            history.setFeedbackId(feedbackId);
            history.setFromStatus(from != null ? from.name() : null);
            history.setToStatus(to.name());
            history.setChangedBy(changedBy);
            feedbackStatusHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("Could not record status history: {}", e.getMessage());
        }
    }

    private void recordAudit(String actorEmail, String actorName, AuditEventType eventType,
                              String description, Long entityId, AuditStatus status) {
        try {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail, eventType, "FEEDBACK_" + eventType.name(),
                    description
            )
            .withActor(actorEmail, actorEmail, actorName, null)
            .withEntity("FEEDBACK", String.valueOf(entityId), "Feedback #" + entityId)
            .withStatus(status)
            .withSeverity(AuditSeverity.INFO);
            auditLogService.record(audit);
        } catch (Exception e) {
            log.warn("Could not record audit log: {}", e.getMessage());
        }
    }
}
