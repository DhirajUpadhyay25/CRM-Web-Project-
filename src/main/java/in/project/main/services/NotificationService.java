package in.project.main.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Notification;
import in.project.main.entities.enums.NotificationType;
import in.project.main.repositories.NotificationRepository;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public long getUnreadCount(String adminEmail) {
        return notificationRepository.countByRecipientEmailAndIsReadFalse(adminEmail);
    }

    public List<Notification> getRecentNotifications(String adminEmail, int limit) {
        return notificationRepository.findTop10ByRecipientEmailOrderByCreatedAtDesc(adminEmail);
    }

    public Page<Notification> getAllNotifications(String adminEmail, Pageable pageable) {
        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(adminEmail, pageable);
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void markAllAsRead(String adminEmail) {
        notificationRepository.markAllAsRead(adminEmail);
    }

    @Transactional
    public Notification createNotification(String recipientEmail, NotificationType type,
                                           String title, String message, String targetUrl) {
        Notification notification = new Notification();
        notification.setRecipientEmail(recipientEmail);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetUrl(targetUrl);
        return notificationRepository.save(notification);
    }
}
