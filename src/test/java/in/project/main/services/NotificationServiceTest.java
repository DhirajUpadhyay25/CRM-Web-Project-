package in.project.main.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.EducationApplication;
import in.project.main.entities.Notification;
import in.project.main.entities.enums.NotificationCategory;
import in.project.main.entities.enums.NotificationPriority;
import in.project.main.entities.enums.NotificationType;
import in.project.main.events.PlatformNotificationEvent;
import in.project.main.repositories.NotificationRepository;

@SpringBootTest(classes = EducationApplication.class)
@Transactional
public class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private static final String TEST_STUDENT = "test_student_" + System.currentTimeMillis() + "@edutake.com";
    private static final String OTHER_STUDENT = "other_student_" + System.currentTimeMillis() + "@edutake.com";

    @BeforeEach
    void setUp() {
        notificationService.clearAllNotifications(TEST_STUDENT);
        notificationService.clearAllNotifications(OTHER_STUDENT);
    }

    @Test
    void testCreateNotification_PersistsCorrectly() {
        Notification notification = notificationService.createNotification(
                TEST_STUDENT,
                NotificationType.COURSE_ENROLLED,
                NotificationCategory.ENROLLMENT,
                NotificationPriority.HIGH,
                "Enrolled in Full Stack Java",
                "You now have access to Full Stack Java Development course.",
                "/student/courses/10/player",
                "COURSE",
                "10",
                "admin@edutake.com",
                "Admin"
        );

        assertNotNull(notification);
        assertNotNull(notification.getId());
        assertEquals(TEST_STUDENT, notification.getRecipientEmail());
        assertEquals(NotificationType.COURSE_ENROLLED, notification.getType());
        assertEquals(NotificationCategory.ENROLLMENT, notification.getCategory());
        assertEquals(NotificationPriority.HIGH, notification.getPriority());
        assertFalse(notification.isRead());
        assertNotNull(notification.getCreatedAt());
        assertEquals("/student/courses/10/player", notification.getTargetUrl());

        long unread = notificationService.getUnreadCount(TEST_STUDENT);
        assertEquals(1, unread);
    }

    @Test
    void testMarkAsRead_WithOwnershipIsolation() {
        Notification notif = notificationService.createNotification(
                TEST_STUDENT,
                NotificationType.PAYMENT_SUCCESS,
                "Payment Success",
                "Payment of INR 2499 received.",
                "/student/orders"
        );

        // Attempt to mark as read with WRONG email (other student) -> should fail / be ignored
        boolean wrongUserMark = notificationService.markAsRead(notif.getId(), OTHER_STUDENT);
        assertFalse(wrongUserMark);

        // Verify still unread
        long unreadBefore = notificationService.getUnreadCount(TEST_STUDENT);
        assertEquals(1, unreadBefore);

        // Mark as read with CORRECT email
        boolean correctUserMark = notificationService.markAsRead(notif.getId(), TEST_STUDENT);
        assertTrue(correctUserMark);

        long unreadAfter = notificationService.getUnreadCount(TEST_STUDENT);
        assertEquals(0, unreadAfter);
    }

    @Test
    void testMarkAllAsRead() {
        notificationService.createNotification(TEST_STUDENT, NotificationType.NEW_LESSON, "Lesson 1", "Message 1", null);
        notificationService.createNotification(TEST_STUDENT, NotificationType.NEW_LESSON, "Lesson 2", "Message 2", null);
        notificationService.createNotification(TEST_STUDENT, NotificationType.NEW_LESSON, "Lesson 3", "Message 3", null);

        assertEquals(3, notificationService.getUnreadCount(TEST_STUDENT));

        notificationService.markAllAsRead(TEST_STUDENT);

        assertEquals(0, notificationService.getUnreadCount(TEST_STUDENT));
        assertEquals(3, notificationService.getTotalCount(TEST_STUDENT));
    }

    @Test
    void testDeleteNotification_WithOwnershipIsolation() {
        Notification notif = notificationService.createNotification(
                TEST_STUDENT,
                NotificationType.ASSIGNMENT_GRADED,
                "Assignment Graded",
                "Score: 95/100",
                "/student/assignments"
        );

        // Other student cannot delete
        boolean wrongDelete = notificationService.deleteNotification(notif.getId(), OTHER_STUDENT);
        assertFalse(wrongDelete);
        assertEquals(1, notificationService.getTotalCount(TEST_STUDENT));

        // Correct student deletes
        boolean correctDelete = notificationService.deleteNotification(notif.getId(), TEST_STUDENT);
        assertTrue(correctDelete);
        assertEquals(0, notificationService.getTotalCount(TEST_STUDENT));
    }

    @Test
    void testFilteringByCategoryAndReadStatus() {
        notificationService.createNotification(TEST_STUDENT, NotificationType.COURSE_ENROLLED, "Course Alert", "Enrolled", null);
        notificationService.createNotification(TEST_STUDENT, NotificationType.PAYMENT_SUCCESS, "Payment Alert", "Paid", null);
        notificationService.createNotification(TEST_STUDENT, NotificationType.SYSTEM_ANNOUNCEMENT, "System Alert", "Maintenance", null);

        // Mark one as read
        List<Notification> recent = notificationService.getRecentNotifications(TEST_STUDENT, 10);
        notificationService.markAsRead(recent.get(0).getId(), TEST_STUDENT);

        // Filter by unread
        Page<Notification> unreadPage = notificationService.getFilteredNotifications(
                TEST_STUDENT, false, null, null, PageRequest.of(0, 10));
        assertEquals(2, unreadPage.getTotalElements());

        // Filter by category
        Page<Notification> paymentPage = notificationService.getFilteredNotifications(
                TEST_STUDENT, null, NotificationCategory.PAYMENT, null, PageRequest.of(0, 10));
        assertEquals(1, paymentPage.getTotalElements());
    }

    @Test
    void testDuplicateNotificationSuppression_Idempotency() {
        Notification first = notificationService.createNotification(
                TEST_STUDENT,
                NotificationType.COURSE_ENROLLED,
                NotificationCategory.ENROLLMENT,
                NotificationPriority.NORMAL,
                "Enrolled in Java",
                "Enrollment confirmed",
                null,
                "COURSE",
                "501",
                null,
                null
        );
        assertNotNull(first);

        // Duplicate call with same entityId and type within 5 minutes
        Notification duplicate = notificationService.createNotification(
                TEST_STUDENT,
                NotificationType.COURSE_ENROLLED,
                NotificationCategory.ENROLLMENT,
                NotificationPriority.NORMAL,
                "Enrolled in Java",
                "Enrollment confirmed",
                null,
                "COURSE",
                "501",
                null,
                null
        );

        assertNull(duplicate, "Duplicate notification must be suppressed by idempotency check");
        assertEquals(1, notificationService.getTotalCount(TEST_STUDENT));
    }

    @Test
    void testDomainEventPublishingAndListener() {
        PlatformNotificationEvent event = new PlatformNotificationEvent(
                TEST_STUDENT,
                NotificationType.NEW_ANNOUNCEMENT,
                "Domain Event Announcement",
                "This notification was delivered via decoupled Spring Event Listener",
                "/student/announcements"
        ).withEntity("ANNOUNCEMENT", "99");

        notificationService.publishEvent(event);

        long count = notificationService.getTotalCount(TEST_STUDENT);
        assertEquals(1, count);
        List<Notification> notifs = notificationService.getRecentNotifications(TEST_STUDENT, 5);
        assertEquals("Domain Event Announcement", notifs.get(0).getTitle());
    }
}
