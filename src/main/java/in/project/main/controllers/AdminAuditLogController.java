package in.project.main.controllers;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import in.project.main.entities.AuditLog;
import in.project.main.entities.SystemErrorLog;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.services.AuditLogService;
import in.project.main.services.SystemMonitoringService;

@Controller
@RequestMapping("/admin")
public class AdminAuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SystemMonitoringService systemMonitoringService;

    @GetMapping("/audit-logs")
    public String listAuditLogs(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> auditLogPage = auditLogService.getFilteredAuditLogs(
            search, category, status, severity, role, entityType, startDate, endDate, pageable
        );

        Map<String, Object> kpiMetrics = auditLogService.getKpiMetrics();

        model.addAttribute("auditLogPage", auditLogPage);
        model.addAttribute("kpi", kpiMetrics);
        model.addAttribute("search", search);
        model.addAttribute("categoryFilter", category);
        model.addAttribute("statusFilter", status);
        model.addAttribute("severityFilter", severity);
        model.addAttribute("roleFilter", role);
        model.addAttribute("entityTypeFilter", entityType);
        model.addAttribute("startDateFilter", startDate);
        model.addAttribute("endDateFilter", endDate);
        model.addAttribute("categories", AuditCategory.values());
        model.addAttribute("statuses", AuditStatus.values());
        model.addAttribute("severities", AuditSeverity.values());

        return "admin/system/audit-logs/list";
    }

    @GetMapping("/audit-logs/{id}")
    public String auditLogDetail(@PathVariable("id") Long id, Model model) {
        AuditLog auditLog = auditLogService.getAuditLogById(id);
        if (auditLog == null) {
            return "redirect:/admin/audit-logs";
        }
        model.addAttribute("auditLog", auditLog);
        return "admin/system/audit-logs/detail";
    }

    @GetMapping("/monitoring")
    public String monitoringDashboard(
            @RequestParam(name = "errorPage", defaultValue = "0") int errorPage,
            Model model) {

        Map<String, Object> health = systemMonitoringService.getSystemHealth();
        Map<String, Object> kpi = auditLogService.getKpiMetrics();
        var securityAlerts = systemMonitoringService.getSecurityAlerts(60);

        Pageable errorPageable = PageRequest.of(errorPage, 10, Sort.by("lastOccurredAt").descending());
        Page<SystemErrorLog> errorPageData = systemMonitoringService.getGroupedErrors("UNRESOLVED", errorPageable);

        model.addAttribute("health", health);
        model.addAttribute("kpi", kpi);
        model.addAttribute("securityAlerts", securityAlerts);
        model.addAttribute("errorPage", errorPageData);
        model.addAttribute("recentActivity", auditLogService.getRecentActivity(8));

        return "admin/system/monitoring/dashboard";
    }
}
