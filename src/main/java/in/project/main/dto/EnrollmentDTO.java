package in.project.main.dto;

import java.time.LocalDateTime;
import in.project.main.entities.enums.EnrollmentStatus;

public class EnrollmentDTO {

    private Long id;

    // Student Information
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private String studentAvatar;
    private boolean studentBanned;

    // Course Information
    private Long courseId;
    private String courseName;
    private String courseSlug;
    private String courseImage;
    private String categoryName;
    private String courseLevel;
    private String instructorName;
    private String instructorEmail;

    // Enrollment Lifecycle
    private EnrollmentStatus status;
    private String statusDisplayName;
    private String statusBadgeClass;
    private String statusDotClass;
    private String enrollmentType;
    private String enrollmentSource;
    private String paymentStatus;
    private String paymentBadgeClass;
    private String orderId;

    // Dates
    private LocalDateTime enrolledAt;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;

    // Learning Progress
    private int progressPercent;
    private long completedLessons;
    private int totalLessons;

    // Meta / Notes
    private String adminNote;
    private String statusReason;
    private boolean accessAllowed;

    public EnrollmentDTO() {}

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

    public String getStudentAvatar() { return studentAvatar; }
    public void setStudentAvatar(String studentAvatar) { this.studentAvatar = studentAvatar; }

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

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCourseLevel() { return courseLevel; }
    public void setCourseLevel(String courseLevel) { this.courseLevel = courseLevel; }

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

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentBadgeClass() { return paymentBadgeClass; }
    public void setPaymentBadgeClass(String paymentBadgeClass) { this.paymentBadgeClass = paymentBadgeClass; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

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

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public long getCompletedLessons() { return completedLessons; }
    public void setCompletedLessons(long completedLessons) { this.completedLessons = completedLessons; }

    public int getTotalLessons() { return totalLessons; }
    public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }

    public boolean isAccessAllowed() { return accessAllowed; }
    public void setAccessAllowed(boolean accessAllowed) { this.accessAllowed = accessAllowed; }

    public String getStudentInitials() {
        if (studentName == null || studentName.trim().isEmpty()) return "ST";
        String[] parts = studentName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
