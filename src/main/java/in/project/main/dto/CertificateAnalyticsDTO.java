package in.project.main.dto;

import java.util.List;
import java.util.Map;

public class CertificateAnalyticsDTO {
    private CertificateStatsDTO stats;
    private double approvalRate;
    private double rejectionRate;
    private double revocationRate;
    private List<Map<String, Object>> monthlyIssuance;
    private List<Map<String, Object>> topCertifiedCourses;
    private List<Map<String, Object>> templateDistribution;

    public CertificateStatsDTO getStats() { return stats; }
    public void setStats(CertificateStatsDTO stats) { this.stats = stats; }

    public double getApprovalRate() { return approvalRate; }
    public void setApprovalRate(double approvalRate) { this.approvalRate = approvalRate; }

    public double getRejectionRate() { return rejectionRate; }
    public void setRejectionRate(double rejectionRate) { this.rejectionRate = rejectionRate; }

    public double getRevocationRate() { return revocationRate; }
    public void setRevocationRate(double revocationRate) { this.revocationRate = revocationRate; }

    public List<Map<String, Object>> getMonthlyIssuance() { return monthlyIssuance; }
    public void setMonthlyIssuance(List<Map<String, Object>> monthlyIssuance) { this.monthlyIssuance = monthlyIssuance; }

    public List<Map<String, Object>> getTopCertifiedCourses() { return topCertifiedCourses; }
    public void setTopCertifiedCourses(List<Map<String, Object>> topCertifiedCourses) { this.topCertifiedCourses = topCertifiedCourses; }

    public List<Map<String, Object>> getTemplateDistribution() { return templateDistribution; }
    public void setTemplateDistribution(List<Map<String, Object>> templateDistribution) { this.templateDistribution = templateDistribution; }
}
