package in.project.main.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.entities.Notification;
import in.project.main.services.NotificationService;

@RestController
@RequestMapping("/admin/api/notifications")
public class AdminNotificationApiController {

    @Autowired
    private NotificationService notificationService;

    private String getEmail(Principal principal) {
        return principal != null ? principal.getName() : "admin@edutake.com";
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        long count = notificationService.getUnreadCount(getEmail(principal));
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentNotifications(Principal principal) {
        List<Notification> notifications = notificationService.getRecentNotifications(getEmail(principal), 10);
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
    public ResponseEntity<Map<String, String>> markAllAsRead(Principal principal) {
        notificationService.markAllAsRead(getEmail(principal));
        Map<String, String> result = new HashMap<>();
        result.put("status", "success");
        return ResponseEntity.ok(result);
    }
}
