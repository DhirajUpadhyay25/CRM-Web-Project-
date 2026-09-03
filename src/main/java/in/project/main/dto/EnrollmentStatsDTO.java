package in.project.main.dto;

public class EnrollmentStatsDTO {

    private long totalEnrollments;
    private long activeEnrollments;
    private long pendingEnrollments;
    private long completedEnrollments;
    private long suspendedEnrollments;
    private long cancelledEnrollments;
    private long revokedEnrollments;
    private long expiredEnrollments;

    private long freeEnrollments;
    private long paidEnrollments;

    private long todayEnrollments;
    private long thisWeekEnrollments;
    private long thisMonthEnrollments;

    private double completionRate;
    private double cancellationRate;

    public EnrollmentStatsDTO() {}

    public long getTotalEnrollments() { return totalEnrollments; }
    public void setTotalEnrollments(long totalEnrollments) { this.totalEnrollments = totalEnrollments; }

    public long getActiveEnrollments() { return activeEnrollments; }
    public void setActiveEnrollments(long activeEnrollments) { this.activeEnrollments = activeEnrollments; }

    public long getPendingEnrollments() { return pendingEnrollments; }
    public void setPendingEnrollments(long pendingEnrollments) { this.pendingEnrollments = pendingEnrollments; }

    public long getCompletedEnrollments() { return completedEnrollments; }
    public void setCompletedEnrollments(long completedEnrollments) { this.completedEnrollments = completedEnrollments; }

    public long getSuspendedEnrollments() { return suspendedEnrollments; }
    public void setSuspendedEnrollments(long suspendedEnrollments) { this.suspendedEnrollments = suspendedEnrollments; }

    public long getCancelledEnrollments() { return cancelledEnrollments; }
    public void setCancelledEnrollments(long cancelledEnrollments) { this.cancelledEnrollments = cancelledEnrollments; }

    public long getRevokedEnrollments() { return revokedEnrollments; }
    public void setRevokedEnrollments(long revokedEnrollments) { this.revokedEnrollments = revokedEnrollments; }

    public long getExpiredEnrollments() { return expiredEnrollments; }
    public void setExpiredEnrollments(long expiredEnrollments) { this.expiredEnrollments = expiredEnrollments; }

    public long getFreeEnrollments() { return freeEnrollments; }
    public void setFreeEnrollments(long freeEnrollments) { this.freeEnrollments = freeEnrollments; }

    public long getPaidEnrollments() { return paidEnrollments; }
    public void setPaidEnrollments(long paidEnrollments) { this.paidEnrollments = paidEnrollments; }

    public long getTodayEnrollments() { return todayEnrollments; }
    public void setTodayEnrollments(long todayEnrollments) { this.todayEnrollments = todayEnrollments; }

    public long getThisWeekEnrollments() { return thisWeekEnrollments; }
    public void setThisWeekEnrollments(long thisWeekEnrollments) { this.thisWeekEnrollments = thisWeekEnrollments; }

    public long getThisMonthEnrollments() { return thisMonthEnrollments; }
    public void setThisMonthEnrollments(long thisMonthEnrollments) { this.thisMonthEnrollments = thisMonthEnrollments; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public double getCancellationRate() { return cancellationRate; }
    public void setCancellationRate(double cancellationRate) { this.cancellationRate = cancellationRate; }
}
