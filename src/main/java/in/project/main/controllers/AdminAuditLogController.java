package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import in.project.main.entities.AuditLog;
import in.project.main.services.AuditLogService;

@Controller
@RequestMapping("/admin/audit-logs")
public class AdminAuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public String listAuditLogs(
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs;

        if (entityType != null && !entityType.trim().isEmpty()) {
            auditLogs = auditLogService.getAuditLogsByEntity(entityType.trim(), pageable);
            model.addAttribute("entityType", entityType.trim());
        } else {
            auditLogs = auditLogService.getAuditLogs(pageable);
        }

        model.addAttribute("auditLogs", auditLogs);

        return "admin/system/audit-logs/list";
    }
}
