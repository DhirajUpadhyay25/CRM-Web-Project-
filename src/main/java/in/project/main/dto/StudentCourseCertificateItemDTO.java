package in.project.main.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StudentCourseCertificateItemDTO {

    private Long enrollmentId;
    private Long courseId;
    private String courseName;
    private String courseCategory;
    private String instructorName;
    private String courseImageUrl;
    private LocalDateTime enrolledAt;
    private String paymentStatus;
    private String orderId;

    // Certificate Application Status: NOT_APPLIED, REQUESTED, UNDER_REVIEW, APPROVED, ISSUED, REJECTED, REVOKED
    private String certificateStatus;
    private Long certificateId;
    private String certificateNumber;
    private String verificationCode;
    private String studentName;
    private String studentRequestNote;
    private String projectUrl;
    private String rejectionReason;
    private LocalDateTime requestDate;
    private LocalDate issueDate;
    private boolean canApply;

    public StudentCourseCertificateItemDTO() {}

    public String getFormattedEnrolledDate() {
        return enrolledAt != null ? enrolledAt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A";
    }

    public String getFormattedRequestDate() {
        return requestDate != null ? requestDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "N/A";
    }

    public String getFormattedIssueDate() {
        return issueDate != null ? issueDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) : "Pending";
    }

    // Getters and Setters
    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCategory() { return courseCategory; }
    public void setCourseCategory(String courseCategory) { this.courseCategory = courseCategory; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getCourseImageUrl() { return courseImageUrl; }
    public void setCourseImageUrl(String courseImageUrl) { this.courseImageUrl = courseImageUrl; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCertificateStatus() { return certificateStatus; }
    public void setCertificateStatus(String certificateStatus) { this.certificateStatus = certificateStatus; }

    public Long getCertificateId() { return certificateId; }
    public void setCertificateId(Long certificateId) { this.certificateId = certificateId; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentRequestNote() { return studentRequestNote; }
    public void setStudentRequestNote(String studentRequestNote) { this.studentRequestNote = studentRequestNote; }

    public String getProjectUrl() { return projectUrl; }
    public void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public boolean isCanApply() { return canApply; }
    public void setCanApply(boolean canApply) { this.canApply = canApply; }
}
