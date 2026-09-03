package in.project.main.dto;

public class CertificateIssueDTO {
    private String certificateTitle = "Certificate of Completion";
    private String certificateType = "COMPLETION";
    private String templateCode = "CLASSIC_GOLD";
    private String adminNotes;

    public String getCertificateTitle() { return certificateTitle; }
    public void setCertificateTitle(String certificateTitle) { this.certificateTitle = certificateTitle; }

    public String getCertificateType() { return certificateType; }
    public void setCertificateType(String certificateType) { this.certificateType = certificateType; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
