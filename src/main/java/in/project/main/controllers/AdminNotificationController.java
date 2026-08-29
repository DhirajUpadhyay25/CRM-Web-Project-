package in.project.main.controllers;

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
import in.project.main.services.NotificationService;

@Controller
@RequestMapping("/admin")
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;

    private static final String ADMIN_EMAIL = "admin@edutake.com";

    @GetMapping("/notifications")
    public String listNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notifications = notificationService.getAllNotifications(ADMIN_EMAIL, pageable);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(ADMIN_EMAIL));

        return "admin/communication/notifications/list";
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(id);
            redirectAttributes.addFlashAttribute("successMsg", "Notification marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to mark notification as read: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllAsRead(RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAllAsRead(ADMIN_EMAIL);
            redirectAttributes.addFlashAttribute("successMsg", "All notifications marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to mark all as read: " + e.getMessage());
        }
        return "redirect:/admin/notifications";
    }
}
