package in.project.main.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import in.project.main.services.AuditLogService;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    @Autowired
    private AuditLogService auditLogService;

    public AuditEventListener() {}

    public AuditEventListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void handlePlatformAuditEvent(PlatformAuditEvent event) {
        if (event == null) return;
        try {
            auditLogService.record(event);
        } catch (Exception e) {
            log.error("Failed to process audit log event [{}]: {}", event.getAction(), e.getMessage());
        }
    }
}
