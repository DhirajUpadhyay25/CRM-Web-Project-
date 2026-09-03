package in.project.main.dto;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class ManualEnrollmentDTO {

    private Long studentId;
    private String studentEmail;
    private Long courseId;
    private String enrollmentType = "ADMIN_ASSIGNED";
    private String enrollmentSource = "ADMIN_ASSIGNMENT";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expiryDate;

    private String adminNote;
    private boolean notifyStudent = true;

    public ManualEnrollmentDTO() {}

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

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

    public boolean isNotifyStudent() { return notifyStudent; }
    public void setNotifyStudent(boolean notifyStudent) { this.notifyStudent = notifyStudent; }
}
