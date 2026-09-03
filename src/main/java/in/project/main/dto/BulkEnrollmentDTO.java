package in.project.main.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class BulkEnrollmentDTO {

    private List<Long> studentIds = new ArrayList<>();
    private List<String> studentEmails = new ArrayList<>();
    private Long courseId;
    private String enrollmentType = "ADMIN_ASSIGNED";
    private String enrollmentSource = "BULK_ENROLLMENT";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expiryDate;

    private String adminNote;
    private boolean notifyStudents = true;

    public BulkEnrollmentDTO() {}

    public List<Long> getStudentIds() { return studentIds; }
    public void setStudentIds(List<Long> studentIds) { this.studentIds = studentIds; }

    public List<String> getStudentEmails() { return studentEmails; }
    public void setStudentEmails(List<String> studentEmails) { this.studentEmails = studentEmails; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getEnrollmentType() { return enrollmentType; }
    public void setEnrollmentType(String enrollmentType) { this.enrollmentType = enrollmentType; }

    public String getEnrollmentSource() { return enrollmentSource; }
    public void setEnrollmentSource(String enrollmentSource) { this.enrollmentSource = enrollmentSource; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public boolean isNotifyStudents() { return notifyStudents; }
    public void setNotifyStudents(boolean notifyStudents) { this.notifyStudents = notifyStudents; }
}
