package in.project.main.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.entities.Notification;
import in.project.main.services.NotificationService;

/**
 * REST controller for admin notification actions.
 * Provides JSON endpoints for the notification dropdown and badge.
 */
@RestController
@RequestMapping("/admin/api/notifications")
public class AdminNotificationApiController {

    @Autowired
    private NotificationService notificationService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = notificationService.getUnreadCount(adminEmail);
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentNotifications() {
        List<Notification> notifications = notificationService.getRecentNotifications(adminEmail, 10);
        List<Map<String, Object>> result = notifications.stream().map(n -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("type", n.getType().name());
            item.put("title", n.getTitle());
            item.put("message", n.getMessage());
            item.put("read", n.isRead());
            item.put("targetUrl", n.getTargetUrl());
            item.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
            return item;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable("id") Long id) {
        notificationService.markAsRead(id);
        Map<String, String> result = new HashMap<>();
        result.put("status", "success");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        notificationService.markAllAsRead(adminEmail);
        Map<String, String> result = new HashMap<>();
        result.put("status", "success");
        return ResponseEntity.ok(result);
    }
}
