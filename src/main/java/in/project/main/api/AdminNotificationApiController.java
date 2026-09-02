package in.project.main.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.entities.Notification;
import in.project.main.services.NotificationService;

@RestController
@RequestMapping("/admin/api/notifications")
public class AdminNotificationApiController {

    @Autowired
    private NotificationService notificationService;

    private String getEmail(Principal principal) {
        return principal != null ? principal.getName() : NotificationService.DEFAULT_ADMIN_EMAIL;
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        long count = notificationService.getUnreadCount(getEmail(principal));
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentNotifications(
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            Principal principal) {

        List<Notification> notifications = notificationService.getRecentNotifications(getEmail(principal), limit);
        List<Map<String, Object>> result = notifications.stream().map(n -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("type", n.getType() != null ? n.getType().name() : "SYSTEM_ANNOUNCEMENT");
            item.put("category", n.getCategory() != null ? n.getCategory().name() : "SYSTEM");
            item.put("priority", n.getPriority() != null ? n.getPriority().name() : "NORMAL");
            item.put("title", n.getTitle());
            item.put("message", n.getMessage());
            item.put("read", n.isRead());
            item.put("targetUrl", n.getTargetUrl() != null ? n.getTargetUrl() : "#");
            item.put("timeAgo", n.getTimeAgo());
            item.put("iconClass", n.getIconClass());
            item.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable("id") Long id,
            Principal principal) {

        boolean updated = notificationService.markAsRead(id, getEmail(principal));
        Map<String, Object> result = new HashMap<>();
        result.put("status", updated ? "success" : "ignored");
        result.put("unreadCount", notificationService.getUnreadCount(getEmail(principal)));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Principal principal) {
        notificationService.markAllAsRead(getEmail(principal));
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("unreadCount", 0L);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable("id") Long id,
            Principal principal) {

        boolean deleted = notificationService.deleteNotification(id, getEmail(principal));
        Map<String, Object> result = new HashMap<>();
        result.put("status", deleted ? "success" : "ignored");
        result.put("unreadCount", notificationService.getUnreadCount(getEmail(principal)));
        return ResponseEntity.ok(result);
    }
}
