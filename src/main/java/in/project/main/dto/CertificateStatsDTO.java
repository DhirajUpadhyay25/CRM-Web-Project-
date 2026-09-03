package in.project.main.dto;

public class CertificateStatsDTO {
    private long totalCertificates;
    private long issuedCertificates;
    private long pendingRequests;
    private long underReview;
    private long rejectedRequests;
    private long revokedCertificates;
    private long eligiblePendingClaim;
    private long issuedThisMonth;

    public long getTotalCertificates() { return totalCertificates; }
    public void setTotalCertificates(long totalCertificates) { this.totalCertificates = totalCertificates; }

    public long getIssuedCertificates() { return issuedCertificates; }
    public void setIssuedCertificates(long issuedCertificates) { this.issuedCertificates = issuedCertificates; }

    public long getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(long pendingRequests) { this.pendingRequests = pendingRequests; }

    public long getUnderReview() { return underReview; }
    public void setUnderReview(long underReview) { this.underReview = underReview; }

    public long getRejectedRequests() { return rejectedRequests; }
    public void setRejectedRequests(long rejectedRequests) { this.rejectedRequests = rejectedRequests; }

    public long getRevokedCertificates() { return revokedCertificates; }
    public void setRevokedCertificates(long revokedCertificates) { this.revokedCertificates = revokedCertificates; }

    public long getEligiblePendingClaim() { return eligiblePendingClaim; }
    public void setEligiblePendingClaim(long eligiblePendingClaim) { this.eligiblePendingClaim = eligiblePendingClaim; }

    public long getIssuedThisMonth() { return issuedThisMonth; }
    public void setIssuedThisMonth(long issuedThisMonth) { this.issuedThisMonth = issuedThisMonth; }
}
