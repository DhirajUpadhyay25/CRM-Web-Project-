package in.project.main.entities;

import in.project.main.entities.enums.CertificateStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates", indexes = {
    @Index(name = "idx_cert_number", columnList = "certificateNumber", unique = true),
    @Index(name = "idx_cert_verify_code", columnList = "verificationCode", unique = true),
    @Index(name = "idx_cert_uuid", columnList = "certificateUuid", unique = true),
    @Index(name = "idx_cert_status", columnList = "status"),
    @Index(name = "idx_cert_student_email", columnList = "studentEmail")
})
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String certificateNumber;

    @Column(nullable = false, unique = true, length = 32)
    private String verificationCode;

    @Column(nullable = false, unique = true, length = 64)
    private String certificateUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CertificateStatus status = CertificateStatus.ELIGIBLE;

    // Many-to-One Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // Snapshot Fields
    @Column(length = 150)
    private String studentName;

    @Column(length = 150)
    private String studentEmail;

    @Column(length = 200)
    private String courseName;

    @Column(length = 100)
    private String courseCategory;

    @Column(length = 150)
    private String instructorName;

    // Credential Metadata
    @Column(length = 150)
    private String certificateTitle = "Certificate of Completion";

    @Column(length = 50)
    private String certificateType = "COMPLETION"; // COMPLETION, HONORS, PROFESSIONAL

    @Column(length = 50)
    private String templateCode = "CLASSIC_GOLD"; // CLASSIC_GOLD, MODERN_EMERALD, TECH_INDIGO

    // Dates
    private LocalDate issueDate;
    private LocalDateTime completionDate;
    private LocalDateTime expiryDate;
    private LocalDateTime requestDate;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime revokedAt;

    // Workflow and Audit Details
    @Column(length = 500)
    private String studentRequestNote;

    @Column(length = 150)
    private String reviewedByAdmin;

    @Column(length = 150)
    private String approvedByAdmin;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 150)
    private String revokedByAdmin;

    @Column(length = 500)
    private String revocationReason;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    private Long reissuedFromCertificateId;
    private boolean isSuperseded = false;

    // File / Visual Asset Info
    @Column(columnDefinition = "TEXT")
    private String qrCodeData;

    @Column(length = 255)
    private String fileUrl;

    private Long fileSize = 0L;
    private int downloadCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Legacy compatibility helper field
    @Transient
    public String getCertificateCode() {
        return verificationCode != null ? verificationCode : certificateNumber;
    }

    @Transient
    public String getEnrollmentIdStr() {
        return enrollment != null ? String.valueOf(enrollment.getId()) : "";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.certificateUuid == null || this.certificateUuid.isBlank()) {
            this.certificateUuid = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public Enrollment getEnrollment() { return enrollment; }
    public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

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

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
