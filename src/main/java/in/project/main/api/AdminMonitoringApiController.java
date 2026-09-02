package in.project.main.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.entities.AuditLog;
import in.project.main.entities.SystemErrorLog;
import in.project.main.services.AuditLogService;
import in.project.main.services.SystemMonitoringService;

@RestController
@RequestMapping("/admin/api/monitoring")
public class AdminMonitoringApiController {

    @Autowired
    private SystemMonitoringService systemMonitoringService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        return ResponseEntity.ok(systemMonitoringService.getSystemHealth());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMonitoringStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("kpi", auditLogService.getKpiMetrics());
        stats.put("securityAlerts", systemMonitoringService.getSecurityAlerts(60));
        stats.put("recentActivity", auditLogService.getRecentActivity(10));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/errors")
    public ResponseEntity<Page<SystemErrorLog>> getSystemErrors(
            @RequestParam(name = "status", required = false, defaultValue = "UNRESOLVED") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("lastOccurredAt").descending());
        return ResponseEntity.ok(systemMonitoringService.getGroupedErrors(status, pageable));
    }

    @PostMapping("/errors/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveError(
            @PathVariable("id") Long id,
            Principal principal) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        boolean success = systemMonitoringService.resolveError(id, actorEmail);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", success);
        resp.put("message", success ? "Error marked as resolved." : "Error record not found.");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> auditLogs = auditLogService.getFilteredAuditLogs(
            search, category, status, severity, role, entityType, startDate, endDate, pageable
        );
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/audit-logs/{id}")
    public ResponseEntity<AuditLog> getAuditLogDetail(@PathVariable("id") Long id) {
        AuditLog auditLog = auditLogService.getAuditLogById(id);
        if (auditLog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(auditLog);
    }

    @GetMapping("/audit-logs/export")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            Principal principal) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        byte[] csvData = auditLogService.exportCsv(
            search, category, status, severity, role, entityType, startDate, endDate, actorEmail
        );

        String filename = "audit_logs_" + System.currentTimeMillis() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
