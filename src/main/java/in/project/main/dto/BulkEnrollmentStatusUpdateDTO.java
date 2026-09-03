package in.project.main.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import in.project.main.entities.enums.EnrollmentStatus;
import org.springframework.format.annotation.DateTimeFormat;

public class BulkEnrollmentStatusUpdateDTO {

    private List<Long> enrollmentIds = new ArrayList<>();
    private EnrollmentStatus status;
    private String reason;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expiryDate;

    private boolean notifyStudents = true;

    public BulkEnrollmentStatusUpdateDTO() {}

    public List<Long> getEnrollmentIds() { return enrollmentIds; }
    public void setEnrollmentIds(List<Long> enrollmentIds) { this.enrollmentIds = enrollmentIds; }

    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public boolean isNotifyStudents() { return notifyStudents; }
    public void setNotifyStudents(boolean notifyStudents) { this.notifyStudents = notifyStudents; }
}
