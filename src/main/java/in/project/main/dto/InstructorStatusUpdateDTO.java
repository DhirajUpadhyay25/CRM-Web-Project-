package in.project.main.dto;

import in.project.main.entities.enums.InstructorStatus;
import jakarta.validation.constraints.NotNull;

public class InstructorStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private InstructorStatus status;

    private String reason;

    public InstructorStatus getStatus() { return status; }
    public void setStatus(InstructorStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
