package in.project.main.services;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.audit.AuditRequestContextHolder;
import in.project.main.entities.AuditLog;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
        "(?i)\"(password|pwd|token|accessToken|refreshToken|secret|apiKey|card|cvv|authorization|signature)\"\\s*:\\s*\"[^\"]+\""
    );

    public AuditLogService() {}

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // ==========================================
    // Core Record Methods
    // ==========================================

    @Transactional
    public AuditLog record(PlatformAuditEvent event) {
        if (event == null) return null;

        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setActorId(event.getActorId());
            auditLog.setActorEmail(event.getActorEmail() != null ? event.getActorEmail().trim().toLowerCase() : "system@edutake.com");
            auditLog.setActorName(event.getActorName());
            auditLog.setActorRole(event.getActorRole() != null ? event.getActorRole() : "SYSTEM");

            auditLog.setAction(event.getAction() != null ? event.getAction() : "ACTION");
            auditLog.setEventType(event.getEventType());
            auditLog.setCategory(event.getCategory() != null ? event.getCategory() : (event.getEventType() != null ? event.getEventType().getDefaultCategory() : AuditCategory.SYSTEM));
            
            auditLog.setEntityType(event.getEntityType());
            auditLog.setEntityId(event.getEntityId());
            auditLog.setEntityName(event.getEntityName());
            auditLog.setDescription(event.getDescription());

            auditLog.setStatus(event.getStatus() != null ? event.getStatus() : AuditStatus.SUCCESS);
            auditLog.setSeverity(event.getSeverity() != null ? event.getSeverity() : (event.getEventType() != null ? event.getEventType().getDefaultSeverity() : AuditSeverity.INFO));

            auditLog.setIpAddress(event.getIpAddress() != null ? event.getIpAddress() : AuditRequestContextHolder.getClientIp());
            auditLog.setUserAgent(event.getUserAgent() != null ? event.getUserAgent() : AuditRequestContextHolder.getUserAgent());
            auditLog.setRequestId(event.getRequestId() != null ? event.getRequestId() : AuditRequestContextHolder.getRequestId());
            auditLog.setSessionId(event.getSessionId());
            auditLog.setSource(event.getSource() != null ? event.getSource() : "WEB");

            auditLog.setBeforeState(redact(event.getBeforeState()));
            auditLog.setAfterState(redact(event.getAfterState()));
            auditLog.setChangedFields(event.getChangedFields());
            auditLog.setFailureReason(event.getFailureReason());

            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log record: {}", e.getMessage());
            return null;
        }
    }

    public void publishEvent(PlatformAuditEvent event) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        } else {
            record(event);
        }
    }

    // Backwards-compatible log method
    @Transactional
    public void log(String adminEmail, String action, String entityType, String entityId, String details, String result) {
        PlatformAuditEvent event = new PlatformAuditEvent();
        event.setActorEmail(adminEmail);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setDescription(details);
        event.setActorRole("ADMIN");
        
        if (result != null) {
            try {
                event.setStatus(AuditStatus.valueOf(result.toUpperCase()));
            } catch (Exception e) {
                event.setStatus("SUCCESS".equalsIgnoreCase(result) ? AuditStatus.SUCCESS : AuditStatus.FAILED);
            }
        }
        
        // Categorize based on entity type
        if (entityType != null) {
            String upper = entityType.toUpperCase();
            if (upper.contains("INSTRUCTOR")) event.setCategory(AuditCategory.INSTRUCTOR);
            else if (upper.contains("STUDENT") || upper.contains("USER")) event.setCategory(AuditCategory.STUDENT);
            else if (upper.contains("COURSE") || upper.contains("CURRICULUM")) event.setCategory(AuditCategory.COURSE);
            else if (upper.contains("PAYMENT") || upper.contains("ORDER")) event.setCategory(AuditCategory.PAYMENT);
            else if (upper.contains("ENROLL")) event.setCategory(AuditCategory.ENROLLMENT);
            else if (upper.contains("AUTH") || upper.contains("LOGIN")) event.setCategory(AuditCategory.AUTHENTICATION);
            else event.setCategory(AuditCategory.ADMIN);
        }
        
        record(event);
    }

    // ==========================================
    // Filtering & Specification Queries
    // ==========================================

    @Transactional(readOnly = true)
    public Page<AuditLog> getFilteredAuditLogs(
            String search,
            String categoryStr,
            String statusStr,
            String severityStr,
            String roleStr,
            String entityTypeStr,
            String startDateStr,
            String endDateStr,
            Pageable pageable) {

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Text Search across multiple fields
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate actorEmailP = cb.like(cb.lower(root.get("actorEmail")), searchPattern);
                Predicate actorNameP = cb.like(cb.lower(root.get("actorName")), searchPattern);
                Predicate actionP = cb.like(cb.lower(root.get("action")), searchPattern);
                Predicate entityIdP = cb.like(cb.lower(root.get("entityId")), searchPattern);
                Predicate entityNameP = cb.like(cb.lower(root.get("entityName")), searchPattern);
                Predicate descP = cb.like(cb.lower(root.get("description")), searchPattern);
                Predicate reqIdP = cb.like(cb.lower(root.get("requestId")), searchPattern);
                Predicate ipP = cb.like(cb.lower(root.get("ipAddress")), searchPattern);

                predicates.add(cb.or(actorEmailP, actorNameP, actionP, entityIdP, entityNameP, descP, reqIdP, ipP));
            }

            // 2. Category Filter
            if (categoryStr != null && !categoryStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(categoryStr)) {
                try {
                    AuditCategory cat = AuditCategory.valueOf(categoryStr.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("category"), cat));
                } catch (Exception ignored) {}
            }

            // 3. Status Filter
            if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr)) {
                try {
                    AuditStatus stat = AuditStatus.valueOf(statusStr.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), stat));
                } catch (Exception ignored) {}
            }

            // 4. Severity Filter
            if (severityStr != null && !severityStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(severityStr)) {
                try {
                    AuditSeverity sev = AuditSeverity.valueOf(severityStr.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("severity"), sev));
                } catch (Exception ignored) {}
            }

            // 5. Actor Role Filter
            if (roleStr != null && !roleStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(roleStr)) {
                predicates.add(cb.equal(cb.upper(root.get("actorRole")), roleStr.trim().toUpperCase()));
            }

            // 6. Entity Type Filter
            if (entityTypeStr != null && !entityTypeStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(entityTypeStr)) {
                predicates.add(cb.equal(cb.upper(root.get("entityType")), entityTypeStr.trim().toUpperCase()));
            }

            // 7. Date Range Filter
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                try {
                    LocalDateTime start = LocalDate.parse(startDateStr.trim()).atStartOfDay();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
                } catch (Exception ignored) {}
            }

            if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                try {
                    LocalDateTime end = LocalDate.parse(endDateStr.trim()).atTime(23, 59, 59);
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
                } catch (Exception ignored) {}
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByEntity(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
    }

    @Transactional(readOnly = true)
    public AuditLog getAuditLogById(Long id) {
        return auditLogRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentActivity(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return auditLogRepository.findAll(pageable).getContent();
    }

    // ==========================================
    // KPI Metrics Aggregation
    // ==========================================

    @Transactional(readOnly = true)
    public Map<String, Object> getKpiMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        LocalDateTime last24h = LocalDateTime.now().minusHours(24);

        long totalEvents = auditLogRepository.count();
        long successCount = auditLogRepository.countByStatus(AuditStatus.SUCCESS);
        long failedCount = auditLogRepository.countByStatus(AuditStatus.FAILED);
        long deniedCount = auditLogRepository.countByStatus(AuditStatus.DENIED);
        long securityCount = auditLogRepository.countByCategory(AuditCategory.SECURITY);
        long criticalCount = auditLogRepository.countBySeverity(AuditSeverity.CRITICAL);
        long activeActors24h = auditLogRepository.countDistinctActorsSince(last24h);
        long eventsLast24h = auditLogRepository.countSince(last24h);

        metrics.put("totalEvents", totalEvents);
        metrics.put("successCount", successCount);
        metrics.put("failedCount", failedCount);
        metrics.put("deniedCount", deniedCount);
        metrics.put("securityCount", securityCount);
        metrics.put("criticalCount", criticalCount);
        metrics.put("activeActors24h", activeActors24h);
        metrics.put("eventsLast24h", eventsLast24h);

        // Success rate percentage
        double successRate = totalEvents > 0 ? ((double) successCount / totalEvents) * 100.0 : 100.0;
        metrics.put("successRate", Math.round(successRate * 10.0) / 10.0);

        return metrics;
    }

    // ==========================================
    // CSV Export
    // ==========================================

    @Transactional
    public byte[] exportCsv(
            String search,
            String categoryStr,
            String statusStr,
            String severityStr,
            String roleStr,
            String entityTypeStr,
            String startDateStr,
            String endDateStr,
            String actorEmail) {

        Pageable pageable = PageRequest.of(0, 10000, Sort.by("createdAt").descending());
        Page<AuditLog> logs = getFilteredAuditLogs(search, categoryStr, statusStr, severityStr, roleStr, entityTypeStr, startDateStr, endDateStr, pageable);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        // CSV Header
        writer.println("ID,Timestamp,Actor Email,Actor Name,Role,Action,Event Type,Category,Entity Type,Entity ID,Entity Name,Status,Severity,IP Address,Request ID,Description,Failure Reason");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (AuditLog log : logs.getContent()) {
            writer.printf(
                "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                escapeCsv(String.valueOf(log.getId())),
                escapeCsv(log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : ""),
                escapeCsv(log.getActorEmail()),
                escapeCsv(log.getActorName()),
                escapeCsv(log.getActorRole()),
                escapeCsv(log.getAction()),
                escapeCsv(log.getEventType() != null ? log.getEventType().name() : ""),
                escapeCsv(log.getCategory() != null ? log.getCategory().name() : ""),
                escapeCsv(log.getEntityType()),
                escapeCsv(log.getEntityId()),
                escapeCsv(log.getEntityName()),
                escapeCsv(log.getStatus() != null ? log.getStatus().name() : ""),
                escapeCsv(log.getSeverity() != null ? log.getSeverity().name() : ""),
                escapeCsv(log.getIpAddress()),
                escapeCsv(log.getRequestId()),
                escapeCsv(log.getDescription()),
                escapeCsv(log.getFailureReason())
            );
        }
        writer.flush();

        // Audit the export action itself
        try {
            PlatformAuditEvent exportAudit = PlatformAuditEvent.of(
                actorEmail != null ? actorEmail : "admin@edutake.com",
                AuditEventType.AUDIT_EXPORTED,
                "EXPORT_AUDIT_LOGS",
                "Exported " + logs.getTotalElements() + " audit log entries to CSV."
            ).withEntity("AUDIT", "EXPORT", "Audit Log CSV Export");
            record(exportAudit);
        } catch (Exception ignored) {}

        return out.toByteArray();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    // ==========================================
    // Sensitive Data Redaction
    // ==========================================

    public static String redact(String input) {
        if (input == null || input.isBlank()) return input;
        return SENSITIVE_PATTERN.matcher(input).replaceAll("\"$1\":\"[REDACTED]\"");
    }
}
