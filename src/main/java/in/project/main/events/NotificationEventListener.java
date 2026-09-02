package in.project.main.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import in.project.main.services.NotificationService;

/**
 * Event Listener that consumes PlatformNotificationEvents and persists notifications via NotificationService.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @Autowired
    private NotificationService notificationService;

    public NotificationEventListener() {}

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void handlePlatformNotificationEvent(PlatformNotificationEvent event) {
        if (event == null || event.getRecipientEmails().isEmpty()) {
            return;
        }

        for (String recipient : event.getRecipientEmails()) {
            try {
                notificationService.createNotification(
                        recipient,
                        event.getType(),
                        event.getCategory(),
                        event.getPriority(),
                        event.getTitle(),
                        event.getMessage(),
                        event.getTargetUrl(),
                        event.getEntityType(),
                        event.getEntityId(),
                        event.getActorEmail(),
                        event.getActorName()
                );
            } catch (Exception e) {
                log.error("Failed to process notification event for recipient {}: {}", recipient, e.getMessage());
            }
        }
    }
}
