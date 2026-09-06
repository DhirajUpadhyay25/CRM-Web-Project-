package in.project.main.dto;

public class DirectCertificateIssueDTO {

    private String studentEmail;
    private Long courseId;
    private String certificateTitle = "Certificate of Completion";
    private String certificateType = "COMPLETION";
    private String templateCode = "CLASSIC_GOLD";
    private String instructorName;
    private String adminNotes;

    public DirectCertificateIssueDTO() {}

    public DirectCertificateIssueDTO(String studentEmail, Long courseId, String certificateTitle) {
        this.studentEmail = studentEmail;
        this.courseId = courseId;
        this.certificateTitle = certificateTitle;
    }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCertificateTitle() { return certificateTitle; }
    public void setCertificateTitle(String certificateTitle) { this.certificateTitle = certificateTitle; }

    public String getCertificateType() { return certificateType; }
    public void setCertificateType(String certificateType) { this.certificateType = certificateType; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
