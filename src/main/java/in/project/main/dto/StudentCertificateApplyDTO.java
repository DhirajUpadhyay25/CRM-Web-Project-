package in.project.main.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StudentCertificateApplyDTO {

    @NotNull(message = "Enrollment ID is required")
    private Long enrollmentId;

    @Size(max = 150, message = "Student legal name cannot exceed 150 characters")
    private String studentLegalName;

    @Size(max = 500, message = "Student note cannot exceed 500 characters")
    private String studentNote;

    @Size(max = 500, message = "Project URL cannot exceed 500 characters")
    private String projectUrl;

    @Size(max = 50, message = "Certificate type cannot exceed 50 characters")
    private String certificateType = "COMPLETION";

    public StudentCertificateApplyDTO() {}

    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getStudentLegalName() { return studentLegalName; }
    public void setStudentLegalName(String studentLegalName) { this.studentLegalName = studentLegalName; }

    public String getStudentNote() { return studentNote; }
    public void setStudentNote(String studentNote) { this.studentNote = studentNote; }

    public String getProjectUrl() { return projectUrl; }
    public void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }

    public String getCertificateType() { return certificateType; }
    public void setCertificateType(String certificateType) { this.certificateType = certificateType; }
}
