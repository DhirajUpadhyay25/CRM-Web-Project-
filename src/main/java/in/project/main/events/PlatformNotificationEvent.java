package in.project.main.events;

import java.util.ArrayList;
import java.util.List;

import in.project.main.entities.enums.NotificationCategory;
import in.project.main.entities.enums.NotificationPriority;
import in.project.main.entities.enums.NotificationType;

/**
 * Domain Event representing an event that triggers in-app notifications.
 */
public class PlatformNotificationEvent {

    private final List<String> recipientEmails = new ArrayList<>();
    private final NotificationType type;
    private final String title;
    private final String message;
    private String targetUrl;
    private NotificationCategory category;
    private NotificationPriority priority;
    private String entityType;
    private String entityId;
    private String actorEmail;
    private String actorName;

    public PlatformNotificationEvent(String recipientEmail, NotificationType type, String title, String message, String targetUrl) {
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            this.recipientEmails.add(recipientEmail.trim().toLowerCase());
        }
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
        this.category = type != null ? type.getDefaultCategory() : NotificationCategory.SYSTEM;
        this.priority = type != null ? type.getDefaultPriority() : NotificationPriority.NORMAL;
    }

    public PlatformNotificationEvent(List<String> recipientEmails, NotificationType type, String title, String message, String targetUrl) {
        if (recipientEmails != null) {
            for (String email : recipientEmails) {
                if (email != null && !email.isBlank()) {
                    this.recipientEmails.add(email.trim().toLowerCase());
                }
            }
        }
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
        this.category = type != null ? type.getDefaultCategory() : NotificationCategory.SYSTEM;
        this.priority = type != null ? type.getDefaultPriority() : NotificationPriority.NORMAL;
    }

    public PlatformNotificationEvent withEntity(String entityType, String entityId) {
        this.entityType = entityType;
        this.entityId = entityId;
        return this;
    }

    public PlatformNotificationEvent withActor(String actorEmail, String actorName) {
        this.actorEmail = actorEmail;
        this.actorName = actorName;
        return this;
    }

    public PlatformNotificationEvent withPriority(NotificationPriority priority) {
        this.priority = priority;
        return this;
    }

    public PlatformNotificationEvent withCategory(NotificationCategory category) {
        this.category = category;
        return this;
    }

    // Getters
    public List<String> getRecipientEmails() { return recipientEmails; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTargetUrl() { return targetUrl; }
    public NotificationCategory getCategory() { return category; }
    public NotificationPriority getPriority() { return priority; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getActorEmail() { return actorEmail; }
    public String getActorName() { return actorName; }
}
