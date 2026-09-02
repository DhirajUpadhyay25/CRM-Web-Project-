package in.project.main.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Announcement;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AnnouncementRepository;
import in.project.main.services.AuditLogService;
import in.project.main.services.NotificationService;

@Controller
@RequestMapping("/admin/announcements")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementRepository repository;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/communication/announcements/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String title,
                      @RequestParam String content,
                      @RequestParam String targetAudience,
                      @RequestParam String publishDate,
                      @RequestParam Boolean isActive,
                      Principal principal,
                      RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Announcement announcement = new Announcement();
            announcement.setTitle(title);
            announcement.setContent(content);
            announcement.setTargetAudience(targetAudience);
            announcement.setPublishDate(publishDate);
            announcement.setIsActive(isActive);
            Announcement saved = repository.save(announcement);

            if (Boolean.TRUE.equals(isActive)) {
                try {
                    notificationService.sendToAllStudents(
                        in.project.main.entities.enums.NotificationType.NEW_ANNOUNCEMENT,
                        "Announcement: " + title,
                        content != null && content.length() > 150 ? content.substring(0, 147) + "..." : content,
                        "/student/dashboard"
                    );
                } catch (Exception ignored) {}
            }

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.ANNOUNCEMENT_CREATED,
                    "ANNOUNCEMENT_CREATED",
                    "Admin published announcement: '" + title + "' (Target: " + targetAudience + ")."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("ANNOUNCEMENT", String.valueOf(saved.getId()), title)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.INFO);

                auditLogService.record(audit);
            }

            ra.addFlashAttribute("success", "Announcement created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam String targetAudience,
                         @RequestParam String publishDate,
                         @RequestParam Boolean isActive,
                         Principal principal,
                         RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Announcement announcement = repository.findById(id).orElseThrow(() -> new RuntimeException("Announcement not found"));
            announcement.setTitle(title);
            announcement.setContent(content);
            announcement.setTargetAudience(targetAudience);
            announcement.setPublishDate(publishDate);
            announcement.setIsActive(isActive);
            repository.save(announcement);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "ANNOUNCEMENT_UPDATED",
                    "Admin updated announcement #" + id + " ('" + title + "')."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("ANNOUNCEMENT", String.valueOf(id), title)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.INFO);

                auditLogService.record(audit);
            }

            ra.addFlashAttribute("success", "Announcement updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            repository.deleteById(id);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.ANNOUNCEMENT_DELETED,
                    "ANNOUNCEMENT_DELETED",
                    "Admin deleted announcement ID #" + id + "."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("ANNOUNCEMENT", String.valueOf(id), "Announcement #" + id)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.LOW);

                auditLogService.record(audit);
            }

            ra.addFlashAttribute("success", "Announcement deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }
}
