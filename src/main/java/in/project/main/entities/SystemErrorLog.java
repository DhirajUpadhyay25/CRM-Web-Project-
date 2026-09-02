package in.project.main.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_error_log", indexes = {
    @Index(name = "idx_err_created", columnList = "createdAt"),
    @Index(name = "idx_err_signature", columnList = "errorSignature"),
    @Index(name = "idx_err_type", columnList = "errorType"),
    @Index(name = "idx_err_status", columnList = "status"),
    @Index(name = "idx_err_endpoint", columnList = "endpoint")
})
public class SystemErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_type", nullable = false, length = 255)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_signature", nullable = false, length = 64)
    private String errorSignature;

    @Column(name = "service_module", length = 64)
    private String serviceModule = "CORE";

    @Column(name = "endpoint", length = 255)
    private String endpoint;

    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "status_code")
    private Integer statusCode = 500;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "occurrence_count")
    private int occurrenceCount = 1;

    @Column(name = "status", length = 32)
    private String status = "UNRESOLVED"; // UNRESOLVED, INVESTIGATING, RESOLVED, IGNORED

    @Column(name = "last_occurred_at")
    private LocalDateTime lastOccurredAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.lastOccurredAt == null) {
            this.lastOccurredAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "UNRESOLVED";
        }
    }

    public SystemErrorLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorSignature() { return errorSignature; }
    public void setErrorSignature(String errorSignature) { this.errorSignature = errorSignature; }
    public String getServiceModule() { return serviceModule; }
    public void setServiceModule(String serviceModule) { this.serviceModule = serviceModule; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    public int getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastOccurredAt() { return lastOccurredAt; }
    public void setLastOccurredAt(LocalDateTime lastOccurredAt) { this.lastOccurredAt = lastOccurredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
