package in.project.main.entities.enums;

public enum AuditEventType {
    // Authentication & Security
    LOGIN_SUCCESS(AuditCategory.AUTHENTICATION, AuditSeverity.INFO, "User Logged In"),
    LOGIN_FAILED(AuditCategory.SECURITY, AuditSeverity.HIGH, "Failed Login Attempt"),
    LOGOUT(AuditCategory.AUTHENTICATION, AuditSeverity.INFO, "User Logged Out"),
    PASSWORD_CHANGED(AuditCategory.AUTHENTICATION, AuditSeverity.MEDIUM, "Password Changed"),
    PASSWORD_RESET(AuditCategory.AUTHENTICATION, AuditSeverity.HIGH, "Password Reset Requested"),
    ACCOUNT_LOCKED(AuditCategory.SECURITY, AuditSeverity.CRITICAL, "Account Locked"),
    ACCESS_GRANTED(AuditCategory.AUTHORIZATION, AuditSeverity.INFO, "Access Granted"),
    ACCESS_DENIED(AuditCategory.SECURITY, AuditSeverity.HIGH, "Access Denied"),

    // User & Role Operations
    USER_CREATED(AuditCategory.USER, AuditSeverity.INFO, "User Created"),
    USER_UPDATED(AuditCategory.USER, AuditSeverity.INFO, "User Updated"),
    USER_DELETED(AuditCategory.USER, AuditSeverity.HIGH, "User Deleted"),
    USER_STATUS_CHANGED(AuditCategory.USER, AuditSeverity.MEDIUM, "User Status Changed"),
    ROLE_CHANGED(AuditCategory.AUTHORIZATION, AuditSeverity.HIGH, "Role Changed"),
    ROLE_CREATED(AuditCategory.AUTHORIZATION, AuditSeverity.MEDIUM, "RBAC Role Created"),
    ROLE_UPDATED(AuditCategory.AUTHORIZATION, AuditSeverity.MEDIUM, "RBAC Role Updated"),
    ROLE_DELETED(AuditCategory.AUTHORIZATION, AuditSeverity.HIGH, "RBAC Role Deleted"),
    ROLE_ASSIGNED(AuditCategory.AUTHORIZATION, AuditSeverity.HIGH, "RBAC Role Assigned"),

    // Student Operations
    STUDENT_CREATED(AuditCategory.STUDENT, AuditSeverity.INFO, "Student Created"),
    STUDENT_UPDATED(AuditCategory.STUDENT, AuditSeverity.INFO, "Student Updated"),
    STUDENT_STATUS_CHANGED(AuditCategory.STUDENT, AuditSeverity.MEDIUM, "Student Status Changed"),
    STUDENT_DELETED(AuditCategory.STUDENT, AuditSeverity.HIGH, "Student Deleted"),

    // Instructor Operations
    INSTRUCTOR_CREATED(AuditCategory.INSTRUCTOR, AuditSeverity.INFO, "Instructor Created"),
    INSTRUCTOR_UPDATED(AuditCategory.INSTRUCTOR, AuditSeverity.INFO, "Instructor Updated"),
    INSTRUCTOR_STATUS_CHANGED(AuditCategory.INSTRUCTOR, AuditSeverity.MEDIUM, "Instructor Status Changed"),
    INSTRUCTOR_DELETED(AuditCategory.INSTRUCTOR, AuditSeverity.HIGH, "Instructor Deleted"),

    // Course Operations
    COURSE_CREATED(AuditCategory.COURSE, AuditSeverity.INFO, "Course Created"),
    COURSE_UPDATED(AuditCategory.COURSE, AuditSeverity.INFO, "Course Updated"),
    COURSE_PUBLISHED(AuditCategory.COURSE, AuditSeverity.INFO, "Course Published"),
    COURSE_UNPUBLISHED(AuditCategory.COURSE, AuditSeverity.MEDIUM, "Course Unpublished"),
    COURSE_ARCHIVED(AuditCategory.COURSE, AuditSeverity.MEDIUM, "Course Archived"),
    COURSE_DELETED(AuditCategory.COURSE, AuditSeverity.HIGH, "Course Deleted"),
    INSTRUCTOR_ASSIGNED(AuditCategory.COURSE, AuditSeverity.INFO, "Instructor Assigned to Course"),

    // Curriculum Operations
    MODULE_CREATED(AuditCategory.CURRICULUM, AuditSeverity.INFO, "Module Created"),
    MODULE_UPDATED(AuditCategory.CURRICULUM, AuditSeverity.INFO, "Module Updated"),
    MODULE_DELETED(AuditCategory.CURRICULUM, AuditSeverity.MEDIUM, "Module Deleted"),
    LESSON_CREATED(AuditCategory.CURRICULUM, AuditSeverity.INFO, "Lesson Created"),
    LESSON_UPDATED(AuditCategory.CURRICULUM, AuditSeverity.INFO, "Lesson Updated"),
    LESSON_DELETED(AuditCategory.CURRICULUM, AuditSeverity.MEDIUM, "Lesson Deleted"),

    // Enrollment & Payment Operations
    ENROLLMENT_CREATED(AuditCategory.ENROLLMENT, AuditSeverity.INFO, "Enrollment Created"),
    ENROLLMENT_ACTIVATED(AuditCategory.ENROLLMENT, AuditSeverity.INFO, "Enrollment Activated"),
    ENROLLMENT_SUSPENDED(AuditCategory.ENROLLMENT, AuditSeverity.HIGH, "Enrollment Suspended"),
    ENROLLMENT_RESUMED(AuditCategory.ENROLLMENT, AuditSeverity.INFO, "Enrollment Resumed"),
    ENROLLMENT_CANCELLED(AuditCategory.ENROLLMENT, AuditSeverity.MEDIUM, "Enrollment Cancelled"),
    ENROLLMENT_REVOKED(AuditCategory.ENROLLMENT, AuditSeverity.HIGH, "Enrollment Revoked"),
    ENROLLMENT_EXTENDED(AuditCategory.ENROLLMENT, AuditSeverity.INFO, "Enrollment Extended"),
    ENROLLMENT_COMPLETED(AuditCategory.ENROLLMENT, AuditSeverity.INFO, "Enrollment Completed"),
    BULK_ENROLLMENT_CREATED(AuditCategory.ENROLLMENT, AuditSeverity.MEDIUM, "Bulk Enrollment Created"),
    ENROLLMENT_EXPORTED(AuditCategory.ADMIN, AuditSeverity.LOW, "Enrollments Exported"),
    PAYMENT_INITIATED(AuditCategory.PAYMENT, AuditSeverity.INFO, "Payment Initiated"),
    PAYMENT_SUCCESS(AuditCategory.PAYMENT, AuditSeverity.INFO, "Payment Completed"),
    PAYMENT_FAILED(AuditCategory.PAYMENT, AuditSeverity.HIGH, "Payment Failed"),
    PAYMENT_REFUNDED(AuditCategory.PAYMENT, AuditSeverity.HIGH, "Payment Refunded"),

    // Quiz & Assessment
    QUIZ_CREATED(AuditCategory.QUIZ, AuditSeverity.INFO, "Quiz Created"),
    QUIZ_UPDATED(AuditCategory.QUIZ, AuditSeverity.INFO, "Quiz Updated"),
    QUIZ_DELETED(AuditCategory.QUIZ, AuditSeverity.MEDIUM, "Quiz Deleted"),
    QUIZ_SUBMITTED(AuditCategory.QUIZ, AuditSeverity.INFO, "Quiz Submitted"),

    // Communication & Administration
    ANNOUNCEMENT_CREATED(AuditCategory.ADMIN, AuditSeverity.INFO, "Announcement Created"),
    ANNOUNCEMENT_DELETED(AuditCategory.ADMIN, AuditSeverity.LOW, "Announcement Deleted"),
    ENQUIRY_CREATED(AuditCategory.ADMIN, AuditSeverity.INFO, "Enquiry Created"),
    ENQUIRY_UPDATED(AuditCategory.ADMIN, AuditSeverity.INFO, "Enquiry Updated"),
    SETTINGS_CHANGED(AuditCategory.ADMIN, AuditSeverity.HIGH, "System Settings Changed"),
    SETTING_UPDATED(AuditCategory.SYSTEM, AuditSeverity.MEDIUM, "Platform Setting Updated"),
    MAINTENANCE_MODE_ENABLED(AuditCategory.SYSTEM, AuditSeverity.HIGH, "Maintenance Mode Enabled"),
    MAINTENANCE_MODE_DISABLED(AuditCategory.SYSTEM, AuditSeverity.HIGH, "Maintenance Mode Disabled"),
    TEST_EMAIL_SENT(AuditCategory.SYSTEM, AuditSeverity.LOW, "Test Email Sent"),
    AUDIT_EXPORTED(AuditCategory.ADMIN, AuditSeverity.MEDIUM, "Audit Logs Exported"),

    // Certificate Lifecycle Operations
    CERTIFICATE_ELIGIBILITY_GRANTED(AuditCategory.COURSE, AuditSeverity.INFO, "Certificate Eligibility Achieved"),
    CERTIFICATE_REQUESTED(AuditCategory.STUDENT, AuditSeverity.INFO, "Certificate Claim Requested"),
    CERTIFICATE_REVIEW_STARTED(AuditCategory.ADMIN, AuditSeverity.INFO, "Certificate Review Initiated"),
    CERTIFICATE_APPROVED(AuditCategory.ADMIN, AuditSeverity.INFO, "Certificate Request Approved"),
    CERTIFICATE_REJECTED(AuditCategory.ADMIN, AuditSeverity.MEDIUM, "Certificate Request Rejected"),
    CERTIFICATE_ISSUED(AuditCategory.ADMIN, AuditSeverity.INFO, "Certificate Officially Issued"),
    CERTIFICATE_REVOKED(AuditCategory.ADMIN, AuditSeverity.HIGH, "Certificate Revoked"),
    CERTIFICATE_REISSUED(AuditCategory.ADMIN, AuditSeverity.MEDIUM, "Certificate Reissued"),
    CERTIFICATE_DOWNLOADED(AuditCategory.STUDENT, AuditSeverity.LOW, "Certificate Downloaded"),
    CERTIFICATE_VERIFIED(AuditCategory.SYSTEM, AuditSeverity.LOW, "Certificate Verified"),
    CERTIFICATE_EXPORTED(AuditCategory.ADMIN, AuditSeverity.LOW, "Certificates Exported"),

    // Feedback Operations
    FEEDBACK_CREATED(AuditCategory.FEEDBACK, AuditSeverity.INFO, "Feedback Submitted"),
    FEEDBACK_UPDATED(AuditCategory.FEEDBACK, AuditSeverity.INFO, "Feedback Updated"),
    FEEDBACK_VIEWED(AuditCategory.FEEDBACK, AuditSeverity.LOW, "Feedback Viewed"),
    FEEDBACK_RESPONDED(AuditCategory.FEEDBACK, AuditSeverity.INFO, "Feedback Responded"),
    FEEDBACK_STATUS_CHANGED(AuditCategory.FEEDBACK, AuditSeverity.INFO, "Feedback Status Changed"),
    FEEDBACK_RESOLVED(AuditCategory.FEEDBACK, AuditSeverity.INFO, "Feedback Resolved"),
    FEEDBACK_CLOSED(AuditCategory.FEEDBACK, AuditSeverity.INFO, "Feedback Closed"),
    FEEDBACK_DELETED(AuditCategory.FEEDBACK, AuditSeverity.MEDIUM, "Feedback Deleted"),

    // System & Errors
    SYSTEM_ERROR(AuditCategory.SYSTEM, AuditSeverity.CRITICAL, "System Error Encountered"),
    DATABASE_ERROR(AuditCategory.SYSTEM, AuditSeverity.CRITICAL, "Database Failure"),
    INTEGRATION_ERROR(AuditCategory.SYSTEM, AuditSeverity.HIGH, "External Service Error");

    private final AuditCategory defaultCategory;
    private final AuditSeverity defaultSeverity;
    private final String defaultLabel;

    AuditEventType(AuditCategory defaultCategory, AuditSeverity defaultSeverity, String defaultLabel) {
        this.defaultCategory = defaultCategory;
        this.defaultSeverity = defaultSeverity;
        this.defaultLabel = defaultLabel;
    }

    public AuditCategory getDefaultCategory() { return defaultCategory; }
    public AuditSeverity getDefaultSeverity() { return defaultSeverity; }
    public String getDefaultLabel() { return defaultLabel; }
}
