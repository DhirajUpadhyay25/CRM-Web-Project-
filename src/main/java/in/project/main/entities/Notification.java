package in.project.main.entities;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import in.project.main.entities.enums.NotificationCategory;
import in.project.main.entities.enums.NotificationPriority;
import in.project.main.entities.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipientEmail"),
    @Index(name = "idx_notification_read", columnList = "recipientEmail, isRead"),
    @Index(name = "idx_notification_created", columnList = "createdAt"),
    @Index(name = "idx_notification_category", columnList = "recipientEmail, category"),
    @Index(name = "idx_notification_entity", columnList = "entityType, entityId")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientEmail;

    @Column
    private String actorEmail;

    @Column
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationCategory category = NotificationCategory.SYSTEM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String message;

    @Column
    private String entityType;

    @Column
    private String entityId;

    @Column
    private String targetUrl;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.category == null && this.type != null) {
            this.category = this.type.getDefaultCategory();
        }
        if (this.priority == null && this.type != null) {
            this.priority = this.type.getDefaultPriority();
        }
    }

    // ==========================================
    // Computed Transient Presentation Helpers
    // ==========================================

    @Transient
    public String getTimeAgo() {
        if (createdAt == null) return "Just now";
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "Just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days == 1) return "Yesterday";
        if (days < 7) return days + "d ago";
        return createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    @Transient
    public String getIconClass() {
        if (type != null && type.getIconClass() != null) {
            return type.getIconClass();
        }
        return "bi-bell";
    }

    @Transient
    public String getPriorityBadgeClass() {
        if (priority != null) {
            return priority.getBadgeClass();
        }
        return "bg-gray-50 text-gray-700 border-gray-200";
    }

    // ==========================================
    // Getters and Setters
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) {
        this.type = type;
        if (type != null) {
            if (this.category == null) this.category = type.getDefaultCategory();
            if (this.priority == null) this.priority = type.getDefaultPriority();
        }
    }

    public NotificationCategory getCategory() { return category; }
    public void setCategory(NotificationCategory category) { this.category = category; }

    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
