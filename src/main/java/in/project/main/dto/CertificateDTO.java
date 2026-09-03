package in.project.main.dto;

import in.project.main.entities.enums.CertificateStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CertificateDTO {
    private Long id;
    private String certificateNumber;
    private String verificationCode;
    private String certificateUuid;
    private CertificateStatus status;
    private Long enrollmentId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long courseId;
    private String courseName;
    private String courseCategory;
    private String instructorName;
    private String certificateTitle;
    private String certificateType;
    private String templateCode;
    private LocalDate issueDate;
    private LocalDateTime completionDate;
    private LocalDateTime expiryDate;
    private LocalDateTime requestDate;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime revokedAt;
    private String studentRequestNote;
    private String reviewedByAdmin;
    private String approvedByAdmin;
    private String rejectionReason;
    private String revokedByAdmin;
    private String revocationReason;
    private String adminNotes;
    private Long reissuedFromCertificateId;
    private boolean isSuperseded;
    private String qrCodeData;
    private String fileUrl;
    private int downloadCount;
    private LocalDateTime createdAt;

    // Helper formatting getters
    public String getFormattedIssueDate() {
        return issueDate != null ? issueDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) : "Pending";
    }

    public String getFormattedCompletionDate() {
        return completionDate != null ? completionDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A";
    }

    public String getFormattedRequestDate() {
        return requestDate != null ? requestDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "N/A";
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public String getCertificateUuid() { return certificateUuid; }
    public void setCertificateUuid(String certificateUuid) { this.certificateUuid = certificateUuid; }

    public CertificateStatus getStatus() { return status; }
    public void setStatus(CertificateStatus status) { this.status = status; }

    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCategory() { return courseCategory; }
    public void setCourseCategory(String courseCategory) { this.courseCategory = courseCategory; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getCertificateTitle() { return certificateTitle; }
    public void setCertificateTitle(String certificateTitle) { this.certificateTitle = certificateTitle; }

    public String getCertificateType() { return certificateType; }
    public void setCertificateType(String certificateType) { this.certificateType = certificateType; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDateTime getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDateTime completionDate) { this.completionDate = completionDate; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getStudentRequestNote() { return studentRequestNote; }
    public void setStudentRequestNote(String studentRequestNote) { this.studentRequestNote = studentRequestNote; }

    public String getReviewedByAdmin() { return reviewedByAdmin; }
    public void setReviewedByAdmin(String reviewedByAdmin) { this.reviewedByAdmin = reviewedByAdmin; }

    public String getApprovedByAdmin() { return approvedByAdmin; }
    public void setApprovedByAdmin(String approvedByAdmin) { this.approvedByAdmin = approvedByAdmin; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getRevokedByAdmin() { return revokedByAdmin; }
    public void setRevokedByAdmin(String revokedByAdmin) { this.revokedByAdmin = revokedByAdmin; }

    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public Long getReissuedFromCertificateId() { return reissuedFromCertificateId; }
    public void setReissuedFromCertificateId(Long reissuedFromCertificateId) { this.reissuedFromCertificateId = reissuedFromCertificateId; }

    public boolean isSuperseded() { return isSuperseded; }
    public void setSuperseded(boolean superseded) { isSuperseded = superseded; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
