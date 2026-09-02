package in.project.main.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.entities.Notification;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.NotificationService;

@RestController
@RequestMapping("/instructor/api/notifications")
public class InstructorNotificationApiController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }
        long count = notificationService.getUnreadCount(userDetails.getUsername());
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentNotifications(
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.ok(List.of());
        }

        String email = userDetails.getUsername();
        List<Notification> notifications = notificationService.getRecentNotifications(email, limit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : notifications) {
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
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails != null ? userDetails.getUsername() : null;
        boolean updated = notificationService.markAsRead(id, email);
        Map<String, Object> result = new HashMap<>();
        result.put("status", updated ? "success" : "ignored");
        result.put("unreadCount", notificationService.getUnreadCount(email));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        notificationService.markAllAsRead(email);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("unreadCount", 0L);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails != null ? userDetails.getUsername() : null;
        boolean deleted = notificationService.deleteNotification(id, email);
        Map<String, Object> result = new HashMap<>();
        result.put("status", deleted ? "success" : "ignored");
        result.put("unreadCount", notificationService.getUnreadCount(email));
        return ResponseEntity.ok(result);
    }
}
