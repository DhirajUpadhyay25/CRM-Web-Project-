package in.project.main.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import in.project.main.entities.enums.EnrollmentStatus;

public class EnrollmentDetailDTO {

    private Long id;

    // Student Profile
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private String studentCity;
    private String studentCountry;
    private String studentAvatar;
    private String studentRegisteredDate;
    private boolean studentBanned;

    // Course Information
    private Long courseId;
    private String courseName;
    private String courseSlug;
    private String courseImage;
    private String courseHeadline;
    private String categoryName;
    private String courseLevel;
    private String courseLanguage;
    private String courseDuration;
    private BigDecimal courseOriginalPrice;
    private BigDecimal courseDiscountedPrice;
    private String courseStatus;
    private String instructorName;
    private String instructorEmail;

    // Enrollment Lifecycle Details
    private EnrollmentStatus status;
    private String statusDisplayName;
    private String statusBadgeClass;
    private String statusDotClass;
    private String enrollmentType;
    private String enrollmentSource;
    private LocalDateTime enrolledAt;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime updatedAt;
    private String statusReason;
    private String adminNote;
    private boolean accessAllowed;

    // Learning Progress Breakdown
    private int progressPercent;
    private long completedLessonsCount;
    private int totalLessonsCount;
    private int completedModulesCount;
    private int totalModulesCount;
    private int quizzesPassedCount;
    private int totalQuizzesCount;
    private int assignmentsSubmittedCount;
    private int totalAssignmentsCount;
    private String certificateCode;
    private String certificateIssuedDate;

    // Financial / Payment Information
    private String orderId;
    private String paymentId;
    private String transactionAmount;
    private String paymentStatus;
    private String paymentBadgeClass;
    private String paymentMethod;
    private String paymentDate;
    private String couponCode;
    private String discountAmount;

    // Operational Audit & History Timeline
    private List<AuditTimelineItemDTO> historyTimeline = new ArrayList<>();

    // Inner DTO for Audit Timeline
    public static class AuditTimelineItemDTO {
        private String event;
        private String title;
        private String description;
        private String actor;
        private String timestamp;
        private String badgeClass;
        private String iconClass;

        public AuditTimelineItemDTO() {}

        public AuditTimelineItemDTO(String event, String title, String description, String actor, String timestamp, String badgeClass, String iconClass) {
            this.event = event;
            this.title = title;
            this.description = description;
            this.actor = actor;
            this.timestamp = timestamp;
            this.badgeClass = badgeClass;
            this.iconClass = iconClass;
        }

        public String getEvent() { return event; }
        public void setEvent(String event) { this.event = event; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getActor() { return actor; }
        public void setActor(String actor) { this.actor = actor; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getBadgeClass() { return badgeClass; }
        public void setBadgeClass(String badgeClass) { this.badgeClass = badgeClass; }
        public String getIconClass() { return iconClass; }
        public void setIconClass(String iconClass) { this.iconClass = iconClass; }
    }

    public EnrollmentDetailDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getStudentPhone() { return studentPhone; }
    public void setStudentPhone(String studentPhone) { this.studentPhone = studentPhone; }

    public String getStudentCity() { return studentCity; }
    public void setStudentCity(String studentCity) { this.studentCity = studentCity; }

    public String getStudentCountry() { return studentCountry; }
    public void setStudentCountry(String studentCountry) { this.studentCountry = studentCountry; }

    public String getStudentAvatar() { return studentAvatar; }
    public void setStudentAvatar(String studentAvatar) { this.studentAvatar = studentAvatar; }

    public String getStudentRegisteredDate() { return studentRegisteredDate; }
    public void setStudentRegisteredDate(String studentRegisteredDate) { this.studentRegisteredDate = studentRegisteredDate; }

    public boolean isStudentBanned() { return studentBanned; }
    public void setStudentBanned(boolean studentBanned) { this.studentBanned = studentBanned; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseSlug() { return courseSlug; }
    public void setCourseSlug(String courseSlug) { this.courseSlug = courseSlug; }

    public String getCourseImage() { return courseImage; }
    public void setCourseImage(String courseImage) { this.courseImage = courseImage; }

    public String getCourseHeadline() { return courseHeadline; }
    public void setCourseHeadline(String courseHeadline) { this.courseHeadline = courseHeadline; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCourseLevel() { return courseLevel; }
    public void setCourseLevel(String courseLevel) { this.courseLevel = courseLevel; }

    public String getCourseLanguage() { return courseLanguage; }
    public void setCourseLanguage(String courseLanguage) { this.courseLanguage = courseLanguage; }

    public String getCourseDuration() { return courseDuration; }
    public void setCourseDuration(String courseDuration) { this.courseDuration = courseDuration; }

    public BigDecimal getCourseOriginalPrice() { return courseOriginalPrice; }
    public void setCourseOriginalPrice(BigDecimal courseOriginalPrice) { this.courseOriginalPrice = courseOriginalPrice; }

    public BigDecimal getCourseDiscountedPrice() { return courseDiscountedPrice; }
    public void setCourseDiscountedPrice(BigDecimal courseDiscountedPrice) { this.courseDiscountedPrice = courseDiscountedPrice; }

    public String getCourseStatus() { return courseStatus; }
    public void setCourseStatus(String courseStatus) { this.courseStatus = courseStatus; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getInstructorEmail() { return instructorEmail; }
    public void setInstructorEmail(String instructorEmail) { this.instructorEmail = instructorEmail; }

    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }

    public String getStatusDisplayName() { return statusDisplayName; }
    public void setStatusDisplayName(String statusDisplayName) { this.statusDisplayName = statusDisplayName; }

    public String getStatusBadgeClass() { return statusBadgeClass; }
    public void setStatusBadgeClass(String statusBadgeClass) { this.statusBadgeClass = statusBadgeClass; }

    public String getStatusDotClass() { return statusDotClass; }
    public void setStatusDotClass(String statusDotClass) { this.statusDotClass = statusDotClass; }

    public String getEnrollmentType() { return enrollmentType; }
    public void setEnrollmentType(String enrollmentType) { this.enrollmentType = enrollmentType; }

    public String getEnrollmentSource() { return enrollmentSource; }
    public void setEnrollmentSource(String enrollmentSource) { this.enrollmentSource = enrollmentSource; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public boolean isAccessAllowed() { return accessAllowed; }
    public void setAccessAllowed(boolean accessAllowed) { this.accessAllowed = accessAllowed; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public long getCompletedLessonsCount() { return completedLessonsCount; }
    public void setCompletedLessonsCount(long completedLessonsCount) { this.completedLessonsCount = completedLessonsCount; }

    public int getTotalLessonsCount() { return totalLessonsCount; }
    public void setTotalLessonsCount(int totalLessonsCount) { this.totalLessonsCount = totalLessonsCount; }

    public int getCompletedModulesCount() { return completedModulesCount; }
    public void setCompletedModulesCount(int completedModulesCount) { this.completedModulesCount = completedModulesCount; }

    public int getTotalModulesCount() { return totalModulesCount; }
    public void setTotalModulesCount(int totalModulesCount) { this.totalModulesCount = totalModulesCount; }

    public int getQuizzesPassedCount() { return quizzesPassedCount; }
    public void setQuizzesPassedCount(int quizzesPassedCount) { this.quizzesPassedCount = quizzesPassedCount; }

    public int getTotalQuizzesCount() { return totalQuizzesCount; }
    public void setTotalQuizzesCount(int totalQuizzesCount) { this.totalQuizzesCount = totalQuizzesCount; }

    public int getAssignmentsSubmittedCount() { return assignmentsSubmittedCount; }
    public void setAssignmentsSubmittedCount(int assignmentsSubmittedCount) { this.assignmentsSubmittedCount = assignmentsSubmittedCount; }

    public int getTotalAssignmentsCount() { return totalAssignmentsCount; }
    public void setTotalAssignmentsCount(int totalAssignmentsCount) { this.totalAssignmentsCount = totalAssignmentsCount; }

    public String getCertificateCode() { return certificateCode; }
    public void setCertificateCode(String certificateCode) { this.certificateCode = certificateCode; }

    public String getCertificateIssuedDate() { return certificateIssuedDate; }
    public void setCertificateIssuedDate(String certificateIssuedDate) { this.certificateIssuedDate = certificateIssuedDate; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getTransactionAmount() { return transactionAmount; }
    public void setTransactionAmount(String transactionAmount) { this.transactionAmount = transactionAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentBadgeClass() { return paymentBadgeClass; }
    public void setPaymentBadgeClass(String paymentBadgeClass) { this.paymentBadgeClass = paymentBadgeClass; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(String discountAmount) { this.discountAmount = discountAmount; }

    public List<AuditTimelineItemDTO> getHistoryTimeline() { return historyTimeline; }
    public void setHistoryTimeline(List<AuditTimelineItemDTO> historyTimeline) { this.historyTimeline = historyTimeline; }

    public String getStudentInitials() {
        if (studentName == null || studentName.trim().isEmpty()) return "ST";
        String[] parts = studentName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
