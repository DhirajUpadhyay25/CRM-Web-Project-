package in.project.main.events;

import in.project.main.audit.AuditRequestContextHolder;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;

public class PlatformAuditEvent {

    private String actorId;
    private String actorEmail;
    private String actorName;
    private String actorRole;

    private String action;
    private AuditEventType eventType;
    private AuditCategory category;

    private String entityType;
    private String entityId;
    private String entityName;
    private String description;

    private AuditStatus status = AuditStatus.SUCCESS;
    private AuditSeverity severity = AuditSeverity.INFO;

    private String ipAddress;
    private String userAgent;
    private String requestId;
    private String sessionId;
    private String source = "WEB";

    private String beforeState;
    private String afterState;
    private String changedFields;
    private String failureReason;

    public PlatformAuditEvent() {
        // Automatically populate request context if available
        this.ipAddress = AuditRequestContextHolder.getClientIp();
        this.userAgent = AuditRequestContextHolder.getUserAgent();
        this.requestId = AuditRequestContextHolder.getRequestId();
    }

    public static PlatformAuditEvent of(String actorEmail, AuditEventType eventType, String action, String description) {
        PlatformAuditEvent event = new PlatformAuditEvent();
        event.actorEmail = actorEmail;
        event.eventType = eventType;
        event.action = action;
        event.description = description;
        if (eventType != null) {
            event.category = eventType.getDefaultCategory();
            event.severity = eventType.getDefaultSeverity();
        }
        return event;
    }

    public PlatformAuditEvent withActor(String actorId, String actorEmail, String actorName, String actorRole) {
        this.actorId = actorId;
        if (actorEmail != null) this.actorEmail = actorEmail;
        this.actorName = actorName;
        this.actorRole = actorRole;
        return this;
    }

    public PlatformAuditEvent withEntity(String entityType, String entityId, String entityName) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityName = entityName;
        return this;
    }

    public PlatformAuditEvent withStatus(AuditStatus status) {
        this.status = status;
        return this;
    }

    public PlatformAuditEvent withSeverity(AuditSeverity severity) {
        this.severity = severity;
        return this;
    }

    public PlatformAuditEvent withCategory(AuditCategory category) {
        this.category = category;
        return this;
    }

    public PlatformAuditEvent withChanges(String beforeState, String afterState, String changedFields) {
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.changedFields = changedFields;
        return this;
    }

    public PlatformAuditEvent withFailure(String failureReason) {
        this.failureReason = failureReason;
        this.status = AuditStatus.FAILED;
        if (this.severity == AuditSeverity.INFO || this.severity == AuditSeverity.LOW) {
            this.severity = AuditSeverity.HIGH;
        }
        return this;
    }

    public PlatformAuditEvent withContext(String ipAddress, String userAgent, String requestId) {
        if (ipAddress != null) this.ipAddress = ipAddress;
        if (userAgent != null) this.userAgent = userAgent;
        if (requestId != null) this.requestId = requestId;
        return this;
    }

    // Getters and Setters
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public AuditEventType getEventType() { return eventType; }
    public void setEventType(AuditEventType eventType) { this.eventType = eventType; }
    public AuditCategory getCategory() { return category; }
    public void setCategory(AuditCategory category) { this.category = category; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }
    public AuditSeverity getSeverity() { return severity; }
    public void setSeverity(AuditSeverity severity) { this.severity = severity; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String beforeState) { this.beforeState = beforeState; }
    public String getAfterState() { return afterState; }
    public void setAfterState(String afterState) { this.afterState = afterState; }
    public String getChangedFields() { return changedFields; }
    public void setChangedFields(String changedFields) { this.changedFields = changedFields; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
