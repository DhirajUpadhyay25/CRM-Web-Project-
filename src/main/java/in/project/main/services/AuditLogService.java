package in.project.main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import in.project.main.entities.AuditLog;
import in.project.main.repositories.AuditLogRepository;

/**
 * Service for recording admin audit trail.
 * Never stores passwords, tokens, secret API keys, or sensitive payment credentials.
 */
@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String adminEmail, String action, String entityType, String entityId,
                    String details, String result) {
        AuditLog log = new AuditLog();
        log.setAdminEmail(adminEmail);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setResult(result);
        auditLogRepository.save(log);
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<AuditLog> getAuditLogsByEntity(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
    }
}
