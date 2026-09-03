package in.project.main.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Employee;
import in.project.main.entities.Enrollment;
import in.project.main.entities.Notification;
import in.project.main.entities.Role;
import in.project.main.entities.User;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.NotificationCategory;
import in.project.main.entities.enums.NotificationPriority;
import in.project.main.entities.enums.NotificationType;
import in.project.main.events.PlatformNotificationEvent;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.NotificationRepository;
import in.project.main.repositories.UserRepository;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    public static final String DEFAULT_ADMIN_EMAIL = "admin@edutake.com";

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired(required = false)
    private EmployeeRepository employeeRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public NotificationService() {}

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.eventPublisher = eventPublisher;
    }

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            EmployeeRepository employeeRepository,
            ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.employeeRepository = employeeRepository;
        this.eventPublisher = eventPublisher;
    }

    public void sendToAdmin(NotificationType type, String title, String message, String targetUrl) {
        sendToAdmin(type, title, message, targetUrl, null, null, null, null);
    }

    // ==========================================
    // Query & Fetch Operations
    // ==========================================

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        if (email == null || email.isBlank()) return 0;
        return notificationRepository.countByRecipientEmailAndIsReadFalse(email.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public long getTotalCount(String email) {
        if (email == null || email.isBlank()) return 0;
        return notificationRepository.countByRecipientEmail(email.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(String email, int limit) {
        if (email == null || email.isBlank()) return List.of();
        return notificationRepository.findTop10ByRecipientEmailOrderByCreatedAtDesc(email.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public Page<Notification> getAllNotifications(String email, Pageable pageable) {
        if (email == null || email.isBlank()) return Page.empty();
        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email.trim().toLowerCase(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getFilteredNotifications(
            String email, Boolean isRead, NotificationCategory category, String keyword, Pageable pageable) {

        if (email == null || email.isBlank()) return Page.empty();
        String normalizedEmail = email.trim().toLowerCase();

        if (keyword != null && !keyword.trim().isEmpty()) {
            return notificationRepository.searchByKeyword(normalizedEmail, keyword.trim(), pageable);
        }

        if (category != null && isRead != null) {
            return notificationRepository.findByRecipientEmailAndCategoryAndIsReadOrderByCreatedAtDesc(
                    normalizedEmail, category, isRead, pageable);
        } else if (category != null) {
            return notificationRepository.findByRecipientEmailAndCategoryOrderByCreatedAtDesc(
                    normalizedEmail, category, pageable);
        } else if (isRead != null) {
            return notificationRepository.findByRecipientEmailAndIsReadOrderByCreatedAtDesc(
                    normalizedEmail, isRead, pageable);
        }

        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(normalizedEmail, pageable);
    }

    // ==========================================
    // Mutation Operations (Read & Delete)
    // ==========================================

    @Transactional
    public void markAsRead(Long id) {
        if (id == null) return;
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public boolean markAsRead(Long id, String email) {
        if (id == null || email == null) return false;
        int updated = notificationRepository.markAsRead(id, email.trim().toLowerCase());
        return updated > 0;
    }

    @Transactional
    public void markAllAsRead(String email) {
        if (email == null || email.isBlank()) return;
        notificationRepository.markAllAsRead(email.trim().toLowerCase());
    }

    @Transactional
    public boolean deleteNotification(Long id, String email) {
        if (id == null || email == null) return false;
        int deleted = notificationRepository.deleteByIdAndRecipientEmail(id, email.trim().toLowerCase());
        return deleted > 0;
    }

    @Transactional
    public void clearAllNotifications(String email) {
        if (email == null || email.isBlank()) return;
        notificationRepository.deleteAllByRecipientEmail(email.trim().toLowerCase());
    }

    // ==========================================
    // Creation & Event Publishing
    // ==========================================

    @Transactional
    public Notification createNotification(String recipientEmail, NotificationType type,
                                           String title, String message, String targetUrl) {
        return createNotification(
                recipientEmail,
                type,
                type != null ? type.getDefaultCategory() : NotificationCategory.SYSTEM,
                type != null ? type.getDefaultPriority() : NotificationPriority.NORMAL,
                title,
                message,
                targetUrl,
                null,
                null,
                null,
                null
        );
    }

    @Transactional
    public Notification createNotification(
            String recipientEmail,
            NotificationType type,
            NotificationCategory category,
            NotificationPriority priority,
            String title,
            String message,
            String targetUrl,
            String entityType,
            String entityId,
            String actorEmail,
            String actorName) {

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Skipping notification creation: recipient email is null or blank.");
            return null;
        }

        String normalizedRecipient = recipientEmail.trim().toLowerCase();

        // Idempotency check: prevent identical notifications for the same entity within 5 minutes
        if (entityId != null && type != null) {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            boolean exists = notificationRepository.existsByRecipientEmailAndTypeAndEntityIdAndCreatedAtAfter(
                    normalizedRecipient, type, entityId, fiveMinutesAgo);
            if (exists) {
                log.info("Duplicate notification suppressed for recipient: {}, type: {}, entityId: {}",
                        normalizedRecipient, type, entityId);
                return null;
            }
        }

        Notification notification = new Notification();
        notification.setRecipientEmail(normalizedRecipient);
        notification.setType(type != null ? type : NotificationType.SYSTEM_ANNOUNCEMENT);
        notification.setCategory(category != null ? category : (type != null ? type.getDefaultCategory() : NotificationCategory.SYSTEM));
        notification.setPriority(priority != null ? priority : (type != null ? type.getDefaultPriority() : NotificationPriority.NORMAL));
        notification.setTitle(title != null ? title : "Notification");
        notification.setMessage(message);
        notification.setTargetUrl(targetUrl);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setActorEmail(actorEmail);
        notification.setActorName(actorName);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);
        log.debug("Created notification #{} for {}", saved.getId(), normalizedRecipient);
        return saved;
    }

    public void publishEvent(PlatformNotificationEvent event) {
        if (event != null && eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
    }

    // ==========================================
    // Semantic Helper Dispatchers
    // ==========================================

    public void sendToUser(String recipientEmail, NotificationType type, String title, String message, String targetUrl, String entityType, String entityId) {
        PlatformNotificationEvent event = new PlatformNotificationEvent(recipientEmail, type, title, message, targetUrl)
                .withEntity(entityType, entityId);
        publishEvent(event);
    }

    public void sendToStudent(String recipientEmail, NotificationType type, String title, String message, String targetUrl, String entityType, String entityId) {
        sendToUser(recipientEmail, type, title, message, targetUrl, entityType, entityId);
    }

    public void sendToStudent(String recipientEmail, NotificationType type, String title, String message, String targetUrl) {
        sendToUser(recipientEmail, type, title, message, targetUrl, null, null);
    }

    public void sendToInstructor(String instructorEmail, NotificationType type, String title, String message, String targetUrl) {
        sendToInstructor(instructorEmail, type, title, message, targetUrl, null, null);
    }

    public void sendToAdmins(NotificationType type, String title, String message, String targetUrl, String entityType, String entityId) {
        sendToAdmin(type, title, message, targetUrl, entityType, entityId, null, null);
    }

    public void sendToAdmin(NotificationType type, String title, String message, String targetUrl, String entityType, String entityId, String actorEmail, String actorName) {
        List<String> adminEmails = new ArrayList<>();
        adminEmails.add(DEFAULT_ADMIN_EMAIL);
        if (employeeRepository != null) {
            try {
                List<Employee> admins = employeeRepository.findByRole(Role.ADMIN);
                if (admins != null) {
                    for (Employee emp : admins) {
                        if (emp.getEmail() != null && !emp.getEmail().isBlank()) {
                            adminEmails.add(emp.getEmail().trim().toLowerCase());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not query admin employees for notification dispatch: {}", e.getMessage());
            }
        }
        List<String> uniqueAdmins = adminEmails.stream().distinct().collect(Collectors.toList());
        PlatformNotificationEvent event = new PlatformNotificationEvent(uniqueAdmins, type, title, message, targetUrl)
                .withEntity(entityType, entityId)
                .withActor(actorEmail, actorName);
        publishEvent(event);
    }

    public void sendToInstructor(String instructorEmail, NotificationType type, String title, String message, String targetUrl, String entityType, String entityId) {
        if (instructorEmail == null || instructorEmail.isBlank()) return;
        PlatformNotificationEvent event = new PlatformNotificationEvent(instructorEmail, type, title, message, targetUrl)
                .withEntity(entityType, entityId);
        publishEvent(event);
    }

    public void sendToEnrolledStudents(Long courseId, NotificationType type, String title, String message, String targetUrl) {
        if (courseId == null) return;
        List<Enrollment> enrollments = enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
        List<String> emails = enrollments.stream()
                .filter(e -> e.getUser() != null && e.getUser().getEmail() != null)
                .map(e -> e.getUser().getEmail())
                .distinct()
                .collect(Collectors.toList());

        if (!emails.isEmpty()) {
            PlatformNotificationEvent event = new PlatformNotificationEvent(emails, type, title, message, targetUrl)
                    .withEntity("COURSE", String.valueOf(courseId));
            publishEvent(event);
        }
    }

    public void sendToAllStudents(NotificationType type, String title, String message, String targetUrl) {
        List<User> users = userRepository.findAll();
        List<String> emails = users.stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .map(User::getEmail)
                .distinct()
                .collect(Collectors.toList());

        if (!emails.isEmpty()) {
            PlatformNotificationEvent event = new PlatformNotificationEvent(emails, type, title, message, targetUrl);
            publishEvent(event);
        }
    }

    @Transactional
    public void broadcastNotification(
            String title, String message, String targetAudience,
            NotificationCategory category, NotificationPriority priority,
            String targetUrl, String actorEmail) {

        List<String> recipients = new ArrayList<>();
        if ("STUDENTS".equalsIgnoreCase(targetAudience) || "STUDENT".equalsIgnoreCase(targetAudience)) {
            recipients = userRepository.findAll().stream()
                    .map(User::getEmail)
                    .filter(e -> e != null && !e.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        } else if ("INSTRUCTORS".equalsIgnoreCase(targetAudience) || "INSTRUCTOR".equalsIgnoreCase(targetAudience)) {
            if (employeeRepository != null) {
                try {
                    List<Employee> instructors = employeeRepository.findByRole(Role.INSTRUCTOR);
                    if (instructors != null) {
                        for (Employee emp : instructors) {
                            if (emp.getEmail() != null && !emp.getEmail().isBlank()) {
                                recipients.add(emp.getEmail().trim().toLowerCase());
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } else { // ALL
            recipients.addAll(userRepository.findAll().stream()
                    .map(User::getEmail)
                    .filter(e -> e != null && !e.isBlank())
                    .collect(Collectors.toList()));

            if (employeeRepository != null) {
                try {
                    for (Employee emp : employeeRepository.findAll()) {
                        if (emp.getEmail() != null && !emp.getEmail().isBlank()) {
                            recipients.add(emp.getEmail().trim().toLowerCase());
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        List<String> uniqueRecipients = recipients.stream().distinct().collect(Collectors.toList());
        if (!uniqueRecipients.isEmpty()) {
            PlatformNotificationEvent event = new PlatformNotificationEvent(
                    uniqueRecipients, NotificationType.SYSTEM_ANNOUNCEMENT, title, message, targetUrl)
                    .withCategory(category != null ? category : NotificationCategory.SYSTEM)
                    .withPriority(priority != null ? priority : NotificationPriority.NORMAL)
                    .withActor(actorEmail, "Administrator");
            publishEvent(event);
        }
    }

    @Transactional
    public void bulkMarkAsRead(List<Long> ids, String userEmail) {
        if (ids == null || ids.isEmpty() || userEmail == null) return;
        List<Notification> notifications = notificationRepository.findAllById(ids);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : notifications) {
            if (userEmail.equalsIgnoreCase(n.getRecipientEmail()) || DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(userEmail)) {
                n.setRead(true);
                n.setReadAt(now);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void bulkDelete(List<Long> ids, String userEmail) {
        if (ids == null || ids.isEmpty() || userEmail == null) return;
        List<Notification> notifications = notificationRepository.findAllById(ids);
        List<Notification> toDelete = new ArrayList<>();
        for (Notification n : notifications) {
            if (userEmail.equalsIgnoreCase(n.getRecipientEmail()) || DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(userEmail)) {
                toDelete.add(n);
            }
        }
        if (!toDelete.isEmpty()) {
            notificationRepository.deleteAll(toDelete);
        }
    }
}
