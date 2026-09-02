package in.project.main.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import in.project.main.entities.Enrollment;
import in.project.main.entities.Notification;
import in.project.main.entities.User;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.NotificationCategory;
import in.project.main.entities.enums.NotificationPriority;
import in.project.main.entities.enums.NotificationType;
import in.project.main.events.NotificationEventListener;
import in.project.main.events.PlatformNotificationEvent;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.NotificationRepository;
import in.project.main.repositories.UserRepository;

public class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private EnrollmentRepository enrollmentRepository;
    private ApplicationEventPublisher eventPublisher;
    private NotificationService notificationService;
    private NotificationEventListener notificationEventListener;

    private static final String STUDENT_EMAIL = "student@edutake.com";
    private static final String ADMIN_EMAIL = "admin@edutake.com";

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        enrollmentRepository = mock(EnrollmentRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                enrollmentRepository,
                eventPublisher
        );

        notificationEventListener = new NotificationEventListener(notificationService);
    }

    @Test
    void testCreateNotification_WithFullMetadata() {
        when(notificationRepository.existsByRecipientEmailAndTypeAndEntityIdAndCreatedAtAfter(
                eq(STUDENT_EMAIL), eq(NotificationType.COURSE_ENROLLED), eq("101"), any(LocalDateTime.class)))
                .thenReturn(false);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        Notification saved = notificationService.createNotification(
                STUDENT_EMAIL,
                NotificationType.COURSE_ENROLLED,
                NotificationCategory.ENROLLMENT,
                NotificationPriority.HIGH,
                "Enrolled in Spring Boot Masterclass",
                "Your enrollment has been confirmed.",
                "/student/courses/101/player",
                "COURSE",
                "101",
                ADMIN_EMAIL,
                "System Admin"
        );

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        assertEquals(STUDENT_EMAIL, saved.getRecipientEmail());
        assertEquals(NotificationType.COURSE_ENROLLED, saved.getType());
        assertEquals(NotificationCategory.ENROLLMENT, saved.getCategory());
        assertEquals(NotificationPriority.HIGH, saved.getPriority());
        assertEquals("/student/courses/101/player", saved.getTargetUrl());
        assertEquals("COURSE", saved.getEntityType());
        assertEquals("101", saved.getEntityId());
        assertEquals(ADMIN_EMAIL, saved.getActorEmail());
        assertEquals("System Admin", saved.getActorName());
        assertFalse(saved.isRead());
    }

    @Test
    void testDuplicateSuppression_Idempotency() {
        when(notificationRepository.existsByRecipientEmailAndTypeAndEntityIdAndCreatedAtAfter(
                eq(STUDENT_EMAIL), eq(NotificationType.COURSE_ENROLLED), eq("101"), any(LocalDateTime.class)))
                .thenReturn(true);

        Notification duplicate = notificationService.createNotification(
                STUDENT_EMAIL,
                NotificationType.COURSE_ENROLLED,
                NotificationCategory.ENROLLMENT,
                NotificationPriority.NORMAL,
                "Enrolled again",
                "Duplicate message",
                null,
                "COURSE",
                "101",
                null,
                null
        );

        assertNull(duplicate, "Duplicate notification within 5 minutes must be suppressed");
        verify(notificationRepository, times(0)).save(any(Notification.class));
    }

    @Test
    void testMarkAsRead_WithStrictRecipientOwnership() {
        when(notificationRepository.markAsRead(5L, "other@edutake.com")).thenReturn(0);
        when(notificationRepository.markAsRead(5L, STUDENT_EMAIL)).thenReturn(1);

        // Wrong user attempt -> should return false
        boolean wrongUserResult = notificationService.markAsRead(5L, "other@edutake.com");
        assertFalse(wrongUserResult);

        // Correct user attempt -> should return true
        boolean correctUserResult = notificationService.markAsRead(5L, STUDENT_EMAIL);
        assertTrue(correctUserResult);
    }

    @Test
    void testMarkAllAsRead() {
        notificationService.markAllAsRead(STUDENT_EMAIL);
        verify(notificationRepository, times(1)).markAllAsRead(STUDENT_EMAIL);
    }

    @Test
    void testDeleteNotification_WithStrictRecipientOwnership() {
        when(notificationRepository.deleteByIdAndRecipientEmail(10L, "hacker@evil.com")).thenReturn(0);
        when(notificationRepository.deleteByIdAndRecipientEmail(10L, STUDENT_EMAIL)).thenReturn(1);

        // Wrong user attempt -> should return false
        boolean wrongUserDelete = notificationService.deleteNotification(10L, "hacker@evil.com");
        assertFalse(wrongUserDelete);

        // Correct user attempt -> should return true
        boolean correctUserDelete = notificationService.deleteNotification(10L, STUDENT_EMAIL);
        assertTrue(correctUserDelete);
    }

    @Test
    void testSendToAdmin_PublishesPlatformEvent() {
        notificationService.sendToAdmin(
                NotificationType.NEW_STUDENT_REGISTERED,
                "New Student Registered",
                "Dhiraj registered on the platform.",
                "/admin/students"
        );

        ArgumentCaptor<PlatformNotificationEvent> captor = ArgumentCaptor.forClass(PlatformNotificationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        PlatformNotificationEvent event = captor.getValue();
        assertEquals(List.of("admin@edutake.com"), event.getRecipientEmails());
        assertEquals(NotificationType.NEW_STUDENT_REGISTERED, event.getType());
        assertEquals("New Student Registered", event.getTitle());
    }

    @Test
    void testSendToEnrolledStudents_BroadcastsEventToActiveRecipients() {
        Enrollment e1 = new Enrollment();
        User u1 = new User();
        u1.setEmail("student1@edutake.com");
        e1.setUser(u1);

        Enrollment e2 = new Enrollment();
        User u2 = new User();
        u2.setEmail("student2@edutake.com");
        e2.setUser(u2);

        when(enrollmentRepository.findByCourseIdAndStatus(50L, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(e1, e2));

        notificationService.sendToEnrolledStudents(
                50L,
                NotificationType.NEW_LESSON_ADDED,
                "New Lesson Available",
                "Lesson 4 has been uploaded.",
                "/student/courses/50/player"
        );

        ArgumentCaptor<PlatformNotificationEvent> captor = ArgumentCaptor.forClass(PlatformNotificationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        PlatformNotificationEvent event = captor.getValue();
        assertEquals(2, event.getRecipientEmails().size());
        assertTrue(event.getRecipientEmails().contains("student1@edutake.com"));
        assertTrue(event.getRecipientEmails().contains("student2@edutake.com"));
    }

    @Test
    void testFilteredPaginationQueries() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> mockPage = new PageImpl<>(List.of(new Notification()));

        when(notificationRepository.findByRecipientEmailAndCategoryAndIsReadOrderByCreatedAtDesc(
                eq(STUDENT_EMAIL), eq(NotificationCategory.PAYMENT), eq(false), eq(pageable)))
                .thenReturn(mockPage);

        Page<Notification> result = notificationService.getFilteredNotifications(
                STUDENT_EMAIL, false, NotificationCategory.PAYMENT, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testDomainEventExecutionThroughListener() {
        when(notificationRepository.existsByRecipientEmailAndTypeAndEntityIdAndCreatedAtAfter(any(), any(), any(), any()))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        PlatformNotificationEvent event = new PlatformNotificationEvent(
                STUDENT_EMAIL,
                NotificationType.NEW_ANNOUNCEMENT,
                "Platform Maintenance Notice",
                "Scheduled maintenance this Sunday at 2 AM.",
                "/student/announcements"
        ).withCategory(NotificationCategory.ANNOUNCEMENT)
         .withPriority(NotificationPriority.CRITICAL)
         .withActor(ADMIN_EMAIL, "Platform Operations");

        notificationEventListener.handlePlatformNotificationEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(STUDENT_EMAIL, saved.getRecipientEmail());
        assertEquals("Platform Maintenance Notice", saved.getTitle());
        assertEquals(NotificationCategory.ANNOUNCEMENT, saved.getCategory());
        assertEquals(NotificationPriority.CRITICAL, saved.getPriority());
        assertEquals(ADMIN_EMAIL, saved.getActorEmail());
    }
}
