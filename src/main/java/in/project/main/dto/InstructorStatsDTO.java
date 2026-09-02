package in.project.main.dto;

/**
 * Summary metrics and statistics for the Instructor Management dashboard.
 */
public class InstructorStatsDTO {

    private long totalInstructors;
    private long activeInstructors;
    private long inactiveInstructors;
    private long pendingInstructors;
    private long suspendedInstructors;
    private long bannedInstructors;
    private long totalAssignedCourses;
    private long totalStudentsTaught;

    public InstructorStatsDTO() {}

    public InstructorStatsDTO(long totalInstructors, long activeInstructors, long inactiveInstructors,
                              long pendingInstructors, long suspendedInstructors, long bannedInstructors,
                              long totalAssignedCourses, long totalStudentsTaught) {
        this.totalInstructors = totalInstructors;
        this.activeInstructors = activeInstructors;
        this.inactiveInstructors = inactiveInstructors;
        this.pendingInstructors = pendingInstructors;
        this.suspendedInstructors = suspendedInstructors;
        this.bannedInstructors = bannedInstructors;
        this.totalAssignedCourses = totalAssignedCourses;
        this.totalStudentsTaught = totalStudentsTaught;
    }

    public long getSuspendedOrBannedCount() {
        return suspendedInstructors + bannedInstructors;
    }

    public long getTotalInstructors() { return totalInstructors; }
    public void setTotalInstructors(long totalInstructors) { this.totalInstructors = totalInstructors; }

    public long getActiveInstructors() { return activeInstructors; }
    public void setActiveInstructors(long activeInstructors) { this.activeInstructors = activeInstructors; }

    public long getInactiveInstructors() { return inactiveInstructors; }
    public void setInactiveInstructors(long inactiveInstructors) { this.inactiveInstructors = inactiveInstructors; }

    public long getPendingInstructors() { return pendingInstructors; }
    public void setPendingInstructors(long pendingInstructors) { this.pendingInstructors = pendingInstructors; }

    public long getSuspendedInstructors() { return suspendedInstructors; }
    public void setSuspendedInstructors(long suspendedInstructors) { this.suspendedInstructors = suspendedInstructors; }

    public long getBannedInstructors() { return bannedInstructors; }
    public void setBannedInstructors(long bannedInstructors) { this.bannedInstructors = bannedInstructors; }

    public long getTotalAssignedCourses() { return totalAssignedCourses; }
    public void setTotalAssignedCourses(long totalAssignedCourses) { this.totalAssignedCourses = totalAssignedCourses; }

    public long getTotalStudentsTaught() { return totalStudentsTaught; }
    public void setTotalStudentsTaught(long totalStudentsTaught) { this.totalStudentsTaught = totalStudentsTaught; }
}
