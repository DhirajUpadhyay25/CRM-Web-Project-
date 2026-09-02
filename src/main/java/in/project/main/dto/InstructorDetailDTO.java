package in.project.main.dto;

import java.util.ArrayList;
import java.util.List;

import in.project.main.entities.AuditLog;
import in.project.main.entities.Course;
import in.project.main.entities.Enrollment;
import in.project.main.entities.Instructor;

/**
 * Aggregation DTO powering the Instructor Details view with 5 structured tabs.
 */
public class InstructorDetailDTO {

    private Instructor instructor;
    private List<Course> courses = new ArrayList<>();
    private List<Enrollment> enrollments = new ArrayList<>();
    private List<AuditLog> auditLogs = new ArrayList<>();
    private long totalCourses;
    private long publishedCourses;
    private long draftCourses;
    private long totalStudents;
    private long completedEnrollments;
    private long pendingSubmissions;

    public InstructorDetailDTO() {}

    public InstructorDetailDTO(Instructor instructor) {
        this.instructor = instructor;
    }

    public Instructor getInstructor() { return instructor; }
    public void setInstructor(Instructor instructor) { this.instructor = instructor; }

    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) { this.courses = courses; }

    public List<Enrollment> getEnrollments() { return enrollments; }
    public void setEnrollments(List<Enrollment> enrollments) { this.enrollments = enrollments; }

    public List<AuditLog> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<AuditLog> auditLogs) { this.auditLogs = auditLogs; }

    public long getTotalCourses() { return totalCourses; }
    public void setTotalCourses(long totalCourses) { this.totalCourses = totalCourses; }

    public long getPublishedCourses() { return publishedCourses; }
    public void setPublishedCourses(long publishedCourses) { this.publishedCourses = publishedCourses; }

    public long getDraftCourses() { return draftCourses; }
    public void setDraftCourses(long draftCourses) { this.draftCourses = draftCourses; }

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

    public long getCompletedEnrollments() { return completedEnrollments; }
    public void setCompletedEnrollments(long completedEnrollments) { this.completedEnrollments = completedEnrollments; }

    public long getPendingSubmissions() { return pendingSubmissions; }
    public void setPendingSubmissions(long pendingSubmissions) { this.pendingSubmissions = pendingSubmissions; }
}
