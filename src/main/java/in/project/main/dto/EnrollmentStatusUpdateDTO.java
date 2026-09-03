package in.project.main.dto;

import java.time.LocalDateTime;
import in.project.main.entities.enums.EnrollmentStatus;
import org.springframework.format.annotation.DateTimeFormat;

public class EnrollmentStatusUpdateDTO {

    private EnrollmentStatus status;
    private String reason;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expiryDate;

    private boolean notifyStudent = true;

    public EnrollmentStatusUpdateDTO() {}

    public EnrollmentStatusUpdateDTO(EnrollmentStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public boolean isNotifyStudent() { return notifyStudent; }
    public void setNotifyStudent(boolean notifyStudent) { this.notifyStudent = notifyStudent; }
}
