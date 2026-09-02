package in.project.main.services;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.audit.AuditRequestContextHolder;
import in.project.main.entities.AuditLog;
import in.project.main.entities.SystemErrorLog;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AuditLogRepository;
import in.project.main.repositories.SystemErrorLogRepository;

@Service
public class SystemMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(SystemMonitoringService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SystemErrorLogRepository systemErrorLogRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    // ==========================================
    // System Health Diagnostics
    // ==========================================

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();

        // 1. Database Connectivity & Ping Latency
        boolean dbHealthy = false;
        long dbLatencyMs = -1;
        try {
            long start = System.currentTimeMillis();
            Integer ping = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbLatencyMs = System.currentTimeMillis() - start;
            dbHealthy = (ping != null && ping == 1);
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
        }

        Map<String, Object> dbMap = new HashMap<>();
        dbMap.put("status", dbHealthy ? (dbLatencyMs > 500 ? "DEGRADED" : "UP") : "DOWN");
        dbMap.put("latencyMs", dbLatencyMs);
        dbMap.put("database", "MySQL (CrmData)");
        health.put("database", dbMap);

        // 2. JVM Memory Usage
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMemoryMb = heapUsage.getUsed() / (1024 * 1024);
        long maxMemoryMb = heapUsage.getMax() / (1024 * 1024);
        long freeMemoryMb = maxMemoryMb - usedMemoryMb;
        double memoryUsagePercent = maxMemoryMb > 0 ? ((double) usedMemoryMb / maxMemoryMb) * 100.0 : 0.0;

        Map<String, Object> memMap = new HashMap<>();
        memMap.put("usedMb", usedMemoryMb);
        memMap.put("maxMb", maxMemoryMb);
        memMap.put("freeMb", freeMemoryMb);
        memMap.put("percentUsed", Math.round(memoryUsagePercent * 10.0) / 10.0);
        memMap.put("status", memoryUsagePercent > 90.0 ? "DEGRADED" : "UP");
        health.put("memory", memMap);

        // 3. Disk Space Usage
        File rootDrive = new File(".");
        long totalSpaceGb = rootDrive.getTotalSpace() / (1024 * 1024 * 1024);
        long freeSpaceGb = rootDrive.getFreeSpace() / (1024 * 1024 * 1024);
        long usableSpaceGb = rootDrive.getUsableSpace() / (1024 * 1024 * 1024);
        long usedSpaceGb = totalSpaceGb - freeSpaceGb;
        double diskUsagePercent = totalSpaceGb > 0 ? ((double) usedSpaceGb / totalSpaceGb) * 100.0 : 0.0;

        Map<String, Object> diskMap = new HashMap<>();
        diskMap.put("totalGb", totalSpaceGb);
        diskMap.put("usedGb", usedSpaceGb);
        diskMap.put("freeGb", usableSpaceGb);
        diskMap.put("percentUsed", Math.round(diskUsagePercent * 10.0) / 10.0);
        diskMap.put("status", diskUsagePercent > 95.0 ? "DEGRADED" : "UP");
        health.put("disk", diskMap);

        // 4. Runtime & Process Metrics
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long uptimeSeconds = runtimeBean.getUptime() / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;

        Map<String, Object> runtimeMap = new HashMap<>();
        runtimeMap.put("uptimeSeconds", uptimeSeconds);
        runtimeMap.put("uptimeFormatted", String.format("%d h, %d min, %d sec", hours, minutes, seconds));
        runtimeMap.put("threadCount", threadBean.getThreadCount());
        runtimeMap.put("peakThreadCount", threadBean.getPeakThreadCount());
        runtimeMap.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        runtimeMap.put("javaVersion", System.getProperty("java.version"));
        runtimeMap.put("startTime", runtimeBean.getStartTime());
        health.put("runtime", runtimeMap);

        // 5. Overall System Status
        String overallStatus = "HEALTHY";
        if (!dbHealthy) {
            overallStatus = "UNAVAILABLE";
        } else if (dbLatencyMs > 500 || memoryUsagePercent > 90.0 || diskUsagePercent > 95.0) {
            overallStatus = "DEGRADED";
        }
        health.put("status", overallStatus);
        health.put("timestamp", LocalDateTime.now().toString());

        return health;
    }

    // ==========================================
    // Error Logging & Grouping
    // ==========================================

    @Transactional
    public SystemErrorLog recordError(
            Throwable throwable,
            String endpoint,
            String httpMethod,
            Integer statusCode,
            String serviceModule,
            String actorEmail) {

        if (throwable == null) return null;

        try {
            String errorType = throwable.getClass().getName();
            String errorMessage = throwable.getMessage() != null ? throwable.getMessage() : "No message";
            String stackTrace = extractSanitizedStackTrace(throwable);
            String signature = computeSignature(errorType, endpoint, throwable);

            Optional<SystemErrorLog> existingOpt = systemErrorLogRepository.findByErrorSignatureAndStatus(signature, "UNRESOLVED");

            SystemErrorLog errorLog;
            if (existingOpt.isPresent()) {
                errorLog = existingOpt.get();
                errorLog.setOccurrenceCount(errorLog.getOccurrenceCount() + 1);
                errorLog.setLastOccurredAt(LocalDateTime.now());
                errorLog.setErrorMessage(errorMessage);
                errorLog.setStatusCode(statusCode != null ? statusCode : 500);
            } else {
                errorLog = new SystemErrorLog();
                errorLog.setErrorType(errorType);
                errorLog.setErrorMessage(errorMessage);
                errorLog.setErrorSignature(signature);
                errorLog.setServiceModule(serviceModule != null ? serviceModule : "CORE");
                errorLog.setEndpoint(endpoint);
                errorLog.setHttpMethod(httpMethod);
                errorLog.setStatusCode(statusCode != null ? statusCode : 500);
                errorLog.setRequestId(AuditRequestContextHolder.getRequestId());
                errorLog.setActorEmail(actorEmail != null ? actorEmail : AuditRequestContextHolder.getContext().getActorEmail());
                errorLog.setIpAddress(AuditRequestContextHolder.getClientIp());
                errorLog.setStackTrace(stackTrace);
                errorLog.setStatus("UNRESOLVED");
                errorLog.setOccurrenceCount(1);
            }

            SystemErrorLog saved = systemErrorLogRepository.save(errorLog);

            // Also record to AuditLog as SYSTEM_ERROR
            try {
                PlatformAuditEvent auditEvent = PlatformAuditEvent.of(
                    actorEmail != null ? actorEmail : "system@edutake.com",
                    AuditEventType.SYSTEM_ERROR,
                    "SYSTEM_EXCEPTION",
                    "Exception occurred at [" + (httpMethod != null ? httpMethod : "") + " " + (endpoint != null ? endpoint : "backend") + "]: " + errorMessage
                )
                .withEntity("SYSTEM", String.valueOf(saved.getId()), errorType)
                .withFailure(errorMessage)
                .withSeverity(AuditSeverity.CRITICAL);

                auditLogService.record(auditEvent);
            } catch (Exception ignored) {}

            return saved;
        } catch (Exception e) {
            log.error("Failed to record system error: {}", e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Page<SystemErrorLog> getGroupedErrors(String status, Pageable pageable) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return systemErrorLogRepository.findByStatusOrderByLastOccurredAtDesc(status.toUpperCase(), pageable);
        }
        return systemErrorLogRepository.findAllByOrderByLastOccurredAtDesc(pageable);
    }

    @Transactional
    public boolean resolveError(Long errorId, String resolvedBy) {
        Optional<SystemErrorLog> opt = systemErrorLogRepository.findById(errorId);
        if (opt.isPresent()) {
            SystemErrorLog errorLog = opt.get();
            errorLog.setStatus("RESOLVED");
            systemErrorLogRepository.save(errorLog);

            PlatformAuditEvent audit = PlatformAuditEvent.of(
                resolvedBy != null ? resolvedBy : "admin@edutake.com",
                AuditEventType.SETTINGS_CHANGED,
                "RESOLVE_SYSTEM_ERROR",
                "Marked system error #" + errorId + " (" + errorLog.getErrorType() + ") as RESOLVED."
            ).withEntity("SYSTEM_ERROR", String.valueOf(errorId), errorLog.getErrorType());
            auditLogService.record(audit);

            return true;
        }
        return false;
    }

    // ==========================================
    // Security Alerts Analysis
    // ==========================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSecurityAlerts(int windowMinutes) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);

        // 1. High count of failed logins
        long failedLogins = auditLogRepository.countBySeveritySince(AuditSeverity.HIGH, since);
        if (failedLogins > 3) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "FAILED_LOGINS");
            alert.put("severity", "HIGH");
            alert.put("title", "Elevated Login Failures Detected");
            alert.put("message", failedLogins + " failed login attempts detected in the last " + windowMinutes + " minutes.");
            alert.put("timestamp", LocalDateTime.now().toString());
            alerts.add(alert);
        }

        // 2. Access Denied occurrences
        long deniedCount = auditLogRepository.countByStatusSince(AuditStatus.DENIED, since);
        if (deniedCount > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "ACCESS_DENIED");
            alert.put("severity", "HIGH");
            alert.put("title", "Access Denied Violations");
            alert.put("message", deniedCount + " unauthorized access attempt(s) intercepted.");
            alert.put("timestamp", LocalDateTime.now().toString());
            alerts.add(alert);
        }

        // 3. Unresolved Critical System Errors
        long unresolvedErrors = systemErrorLogRepository.countByStatus("UNRESOLVED");
        if (unresolvedErrors > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "UNRESOLVED_ERRORS");
            alert.put("severity", unresolvedErrors > 5 ? "CRITICAL" : "MEDIUM");
            alert.put("title", "Unresolved System Exceptions");
            alert.put("message", unresolvedErrors + " distinct system error group(s) require attention.");
            alert.put("timestamp", LocalDateTime.now().toString());
            alerts.add(alert);
        }

        return alerts;
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private String computeSignature(String errorType, String endpoint, Throwable t) {
        try {
            String rootLocation = "";
            if (t.getStackTrace() != null && t.getStackTrace().length > 0) {
                StackTraceElement elem = t.getStackTrace()[0];
                rootLocation = elem.getClassName() + ":" + elem.getMethodName() + ":" + elem.getLineNumber();
            }
            String raw = (errorType != null ? errorType : "") + "|" + (endpoint != null ? endpoint : "") + "|" + rootLocation;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "sig_" + Math.abs((errorType + endpoint).hashCode());
        }
    }

    private String extractSanitizedStackTrace(Throwable t) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString()).append("\n");
        int count = 0;
        for (StackTraceElement elem : t.getStackTrace()) {
            if (count++ > 15) {
                sb.append("\t... (truncated)\n");
                break;
            }
            sb.append("\tat ").append(elem.toString()).append("\n");
        }
        return sb.toString();
    }
}
