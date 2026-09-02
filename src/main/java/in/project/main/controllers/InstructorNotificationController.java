package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import in.project.main.security.CustomUserDetails;
import in.project.main.services.NotificationService;

@Controller
@RequestMapping("/instructor")
public class InstructorNotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/notifications")
    public String listNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "filter", required = false) String filter,
            @RequestParam(name = "category", required = false) String categoryStr,
            @RequestParam(name = "search", required = false) String search,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        String email = userDetails.getUsername();
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
                email, isRead, category, search, pageable);

        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("notifications", notificationPage.getContent());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(email));
        model.addAttribute("totalCount", notificationService.getTotalCount(email));
        model.addAttribute("currentFilter", filter != null ? filter : "all");
        model.addAttribute("currentCategory", categoryStr != null ? categoryStr : "ALL");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("categories", NotificationCategory.values());

        return "instructor/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        try {
            notificationService.markAsRead(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMsg", "Notification marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to mark notification as read: " + e.getMessage());
        }
        return "redirect:/instructor/notifications";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        try {
            notificationService.markAllAsRead(userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMsg", "All notifications marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to mark all as read: " + e.getMessage());
        }
        return "redirect:/instructor/notifications";
    }

    @PostMapping("/notifications/{id}/delete")
    public String deleteNotification(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        try {
            notificationService.deleteNotification(id, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMsg", "Notification deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete notification: " + e.getMessage());
        }
        return "redirect:/instructor/notifications";
    }
}
