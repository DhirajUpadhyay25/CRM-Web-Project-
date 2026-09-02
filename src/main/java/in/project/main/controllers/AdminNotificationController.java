package in.project.main.controllers;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Notification;
import in.project.main.entities.enums.NotificationCategory;
import in.project.main.services.NotificationService;

@Controller
@RequestMapping("/admin")
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;

    private String getAdminEmail(Principal principal) {
        return principal != null ? principal.getName() : NotificationService.DEFAULT_ADMIN_EMAIL;
    }

    @GetMapping("/notifications")
    public String listNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "category", required = false) String categoryStr,
            @RequestParam(name = "search", required = false) String search,
            Principal principal,
            Model model) {

        String adminEmail = getAdminEmail(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Boolean isRead = null;
        if ("unread".equalsIgnoreCase(filter)) {
            isRead = false;
        } else if ("read".equalsIgnoreCase(filter)) {
            isRead = true;
        }

        NotificationCategory category = null;
        if (categoryStr != null && !categoryStr.isBlank() && !"ALL".equalsIgnoreCase(categoryStr)) {
            try {
                category = NotificationCategory.valueOf(categoryStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        Page<Notification> notificationPage = notificationService.getFilteredNotifications(
                adminEmail, isRead, category, search, pageable);

        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("notifications", notificationPage.getContent());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(adminEmail));
        model.addAttribute("totalNotifications", notificationService.getTotalCount(adminEmail));
        model.addAttribute("currentFilter", filter != null ? filter : "all");
        model.addAttribute("currentCategory", categoryStr != null ? categoryStr : "ALL");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("categories", NotificationCategory.values());

        return "admin/communication/notifications/list";
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(
            @PathVariable("id") Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(id, getAdminEmail(principal));
            redirectAttributes.addFlashAttribute("successMsg", "Notification marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to mark notification as read: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllAsRead(
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAllAsRead(getAdminEmail(principal));
            redirectAttributes.addFlashAttribute("successMsg", "All notifications marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to mark all as read: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/{id}/delete")
    public String deleteNotification(
            @PathVariable("id") Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            notificationService.deleteNotification(id, getAdminEmail(principal));
            redirectAttributes.addFlashAttribute("successMsg", "Notification deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete notification: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }
}
