package in.project.main.dto;

import in.project.main.entities.enums.CertificateStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PublicCertificateVerificationDTO {
    private boolean valid;
    private CertificateStatus status;
    private String certificateNumber;
    private String verificationCode;
    private String studentName;
    private String courseName;
    private String courseCategory;
    private String instructorName;
    private String certificateTitle;
    private LocalDate issueDate;
    private LocalDateTime completionDate;
    private String issuerOrganization = "EduTake Learning Academy";
    private String statusMessage;
    private String verificationUrl;

    public String getFormattedIssueDate() {
        return issueDate != null ? issueDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy")) : "N/A";
    }

    public String getFormattedCompletionDate() {
        return completionDate != null ? completionDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy")) : "N/A";
    }

    // --- Getters & Setters ---

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public CertificateStatus getStatus() { return status; }
    public void setStatus(CertificateStatus status) { this.status = status; }

    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCategory() { return courseCategory; }
    public void setCourseCategory(String courseCategory) { this.courseCategory = courseCategory; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getCertificateTitle() { return certificateTitle; }
    public void setCertificateTitle(String certificateTitle) { this.certificateTitle = certificateTitle; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDateTime getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDateTime completionDate) { this.completionDate = completionDate; }

    public String getIssuerOrganization() { return issuerOrganization; }
    public void setIssuerOrganization(String issuerOrganization) { this.issuerOrganization = issuerOrganization; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public String getVerificationUrl() { return verificationUrl; }
    public void setVerificationUrl(String verificationUrl) { this.verificationUrl = verificationUrl; }
}
