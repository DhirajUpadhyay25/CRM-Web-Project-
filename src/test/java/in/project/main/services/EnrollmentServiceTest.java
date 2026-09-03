package in.project.main.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import in.project.main.dto.BulkEnrollmentDTO;
import in.project.main.dto.BulkEnrollmentResultDTO;
import in.project.main.dto.BulkEnrollmentStatusUpdateDTO;
import in.project.main.dto.EnrollmentAnalyticsDTO;
import in.project.main.dto.EnrollmentDTO;
import in.project.main.dto.EnrollmentDetailDTO;
import in.project.main.dto.EnrollmentStatsDTO;
import in.project.main.dto.EnrollmentStatusUpdateDTO;
import in.project.main.dto.ManualEnrollmentDTO;
import in.project.main.entities.Category;
import in.project.main.entities.Course;
import in.project.main.entities.Enrollment;
import in.project.main.entities.User;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.AssignmentRepository;
import in.project.main.repositories.AssignmentSubmissionRepository;
import in.project.main.repositories.AuditLogRepository;
import in.project.main.repositories.CertificateRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.LessonProgressRepository;
import in.project.main.repositories.LessonRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.PaymentRepository;
import in.project.main.repositories.QuizAttemptRepository;
import in.project.main.repositories.QuizRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.impl.EnrollmentServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private User testStudent1;
    private User testStudent2;
    private User bannedStudent;
    private Course testCourse;
    private Category testCategory;
    private Enrollment activeEnrollment;

    @BeforeEach
    public void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Engineering");

        testCourse = new Course();
        testCourse.setId(101L);
        testCourse.setName("Fullstack Java Bootcamp");
        testCourse.setSlug("fullstack-java");
        testCourse.setCategory(testCategory);
        testCourse.setLevel(CourseLevel.BEGINNER);
        testCourse.setStatus(CourseStatus.PUBLISHED);
        testCourse.setOriginalPrice(BigDecimal.valueOf(4999));
        testCourse.setDiscountedPrice(BigDecimal.valueOf(1999));
        testCourse.setInstructor("Prof. Alan Turing");
        testCourse.setInstructorEmail("alan.turing@edutake.com");

        testStudent1 = new User();
        testStudent1.setId(1001L);
        testStudent1.setName("Alice Walker");
        testStudent1.setEmail("alice@example.com");
        testStudent1.setPhoneno("9876543210");
        testStudent1.setCity("New York");
        testStudent1.setBanStatus(false);

        testStudent2 = new User();
        testStudent2.setId(1002L);
        testStudent2.setName("Bob Smith");
        testStudent2.setEmail("bob@example.com");
        testStudent2.setPhoneno("9876543211");
        testStudent2.setCity("Chicago");
        testStudent2.setBanStatus(false);

        bannedStudent = new User();
        bannedStudent.setId(1003L);
        bannedStudent.setName("Charlie Banned");
        bannedStudent.setEmail("banned@example.com");
        bannedStudent.setPhoneno("9876543212");
        bannedStudent.setCity("Boston");
        bannedStudent.setBanStatus(true);

        activeEnrollment = new Enrollment();
        activeEnrollment.setId(5001L);
        activeEnrollment.setUser(testStudent1);
        activeEnrollment.setCourse(testCourse);
        activeEnrollment.setStatus(EnrollmentStatus.ACTIVE);
        activeEnrollment.setEnrollmentType("MANUAL");
        activeEnrollment.setEnrollmentSource("ADMIN_PANEL");
        activeEnrollment.setEnrolledAt(LocalDateTime.now());
    }

    @Test
    public void testManualEnrollStudent_Success() {
        ManualEnrollmentDTO dto = new ManualEnrollmentDTO();
        dto.setStudentId(testStudent1.getId());
        dto.setCourseId(testCourse.getId());
        dto.setEnrollmentType("ADMIN_ASSIGNED");
        dto.setAdminNote("Granted via scholarship");
        dto.setNotifyStudent(false);

        when(userRepository.findById(testStudent1.getId())).thenReturn(Optional.of(testStudent1));
        when(courseRepository.findById(testCourse.getId())).thenReturn(Optional.of(testCourse));
        when(enrollmentRepository.findByUserIdAndCourseId(testStudent1.getId(), testCourse.getId())).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment e = invocation.getArgument(0);
            e.setId(5002L);
            return e;
        });

        EnrollmentDTO result = enrollmentService.manualEnrollStudent(dto, "admin@edutake.com");

        assertNotNull(result);
        assertEquals(5002L, result.getId());
        assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
        assertEquals("Alice Walker", result.getStudentName());
        assertEquals("Fullstack Java Bootcamp", result.getCourseName());
        assertEquals("ADMIN_ASSIGNED", result.getEnrollmentType());
        assertTrue(result.isAccessAllowed());

        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    public void testManualEnrollStudent_AlreadyEnrolled_ThrowsException() {
        ManualEnrollmentDTO dto = new ManualEnrollmentDTO();
        dto.setStudentId(testStudent1.getId());
        dto.setCourseId(testCourse.getId());

        when(userRepository.findById(testStudent1.getId())).thenReturn(Optional.of(testStudent1));
        when(courseRepository.findById(testCourse.getId())).thenReturn(Optional.of(testCourse));
        when(enrollmentRepository.findByUserIdAndCourseId(testStudent1.getId(), testCourse.getId())).thenReturn(Optional.of(activeEnrollment));

        assertThrows(IllegalStateException.class, () -> {
            enrollmentService.manualEnrollStudent(dto, "admin@edutake.com");
        });

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    public void testManualEnrollStudent_BannedUser_ThrowsException() {
        ManualEnrollmentDTO dto = new ManualEnrollmentDTO();
        dto.setStudentId(bannedStudent.getId());
        dto.setCourseId(testCourse.getId());

        when(userRepository.findById(bannedStudent.getId())).thenReturn(Optional.of(bannedStudent));

        assertThrows(IllegalStateException.class, () -> {
            enrollmentService.manualEnrollStudent(dto, "admin@edutake.com");
        });

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    public void testUpdateEnrollmentStatus_Suspend_RequiresReason() {
        when(enrollmentRepository.findByIdWithDetails(activeEnrollment.getId())).thenReturn(Optional.of(activeEnrollment));

        EnrollmentStatusUpdateDTO updateDto = new EnrollmentStatusUpdateDTO();
        updateDto.setStatus(EnrollmentStatus.SUSPENDED);
        updateDto.setReason(""); // Empty reason

        assertThrows(IllegalArgumentException.class, () -> {
            enrollmentService.updateEnrollmentStatus(activeEnrollment.getId(), updateDto, "admin@edutake.com");
        });
    }

    @Test
    public void testUpdateEnrollmentStatus_SuspendAndResume() {
        when(enrollmentRepository.findByIdWithDetails(activeEnrollment.getId())).thenReturn(Optional.of(activeEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        // Suspend
        EnrollmentStatusUpdateDTO suspendDto = new EnrollmentStatusUpdateDTO(EnrollmentStatus.SUSPENDED, "Pending verification of credentials");
        EnrollmentDTO suspended = enrollmentService.updateEnrollmentStatus(activeEnrollment.getId(), suspendDto, "admin@edutake.com");
        assertEquals(EnrollmentStatus.SUSPENDED, suspended.getStatus());
        assertFalse(suspended.isAccessAllowed());

        // Resume / Reactivate
        EnrollmentStatusUpdateDTO resumeDto = new EnrollmentStatusUpdateDTO(EnrollmentStatus.ACTIVE, "Verification completed");
        EnrollmentDTO resumed = enrollmentService.updateEnrollmentStatus(activeEnrollment.getId(), resumeDto, "admin@edutake.com");
        assertEquals(EnrollmentStatus.ACTIVE, resumed.getStatus());
        assertTrue(resumed.isAccessAllowed());
    }

    @Test
    public void testUpdateEnrollmentStatus_Complete() {
        when(enrollmentRepository.findByIdWithDetails(activeEnrollment.getId())).thenReturn(Optional.of(activeEnrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        EnrollmentStatusUpdateDTO completeDto = new EnrollmentStatusUpdateDTO(EnrollmentStatus.COMPLETED, "Completed all curriculum requirements");
        EnrollmentDTO completed = enrollmentService.updateEnrollmentStatus(activeEnrollment.getId(), completeDto, "admin@edutake.com");

        assertEquals(EnrollmentStatus.COMPLETED, completed.getStatus());
        assertTrue(completed.isAccessAllowed());
        assertNotNull(activeEnrollment.getCompletedAt());
    }

    @Test
    public void testBulkEnrollStudents_SuccessAndSkipped() {
        when(courseRepository.findById(testCourse.getId())).thenReturn(Optional.of(testCourse));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(testStudent1);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(testStudent2);

        // Alice is already active
        when(enrollmentRepository.findByUserIdAndCourseId(testStudent1.getId(), testCourse.getId())).thenReturn(Optional.of(activeEnrollment));
        // Bob is not enrolled
        when(enrollmentRepository.findByUserIdAndCourseId(testStudent2.getId(), testCourse.getId())).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(5003L);
            return e;
        });

        BulkEnrollmentDTO bulkDto = new BulkEnrollmentDTO();
        bulkDto.setCourseId(testCourse.getId());
        bulkDto.setStudentEmails(Arrays.asList("alice@example.com", "bob@example.com"));
        bulkDto.setNotifyStudents(false);

        BulkEnrollmentResultDTO result = enrollmentService.bulkEnrollStudents(bulkDto, "admin@edutake.com");

        assertNotNull(result);
        assertEquals(2, result.getTotalRequested());
        assertEquals(1, result.getSuccessCount()); // Bob enrolled
        assertEquals(1, result.getSkippedAlreadyEnrolledCount()); // Alice skipped
        assertEquals(0, result.getFailedCount());
    }

    @Test
    public void testBulkUpdateStatus() {
        Enrollment e2 = new Enrollment();
        e2.setId(5002L);
        e2.setUser(testStudent2);
        e2.setCourse(testCourse);
        e2.setStatus(EnrollmentStatus.ACTIVE);

        when(enrollmentRepository.findByIdWithDetails(5001L)).thenReturn(Optional.of(activeEnrollment));
        when(enrollmentRepository.findByIdWithDetails(5002L)).thenReturn(Optional.of(e2));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        BulkEnrollmentStatusUpdateDTO bulkStatus = new BulkEnrollmentStatusUpdateDTO();
        bulkStatus.setEnrollmentIds(Arrays.asList(5001L, 5002L));
        bulkStatus.setStatus(EnrollmentStatus.SUSPENDED);
        bulkStatus.setReason("Batch audit maintenance");
        bulkStatus.setNotifyStudents(false);

        BulkEnrollmentResultDTO result = enrollmentService.bulkUpdateStatus(bulkStatus, "admin@edutake.com");

        assertEquals(2, result.getTotalRequested());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(EnrollmentStatus.SUSPENDED, activeEnrollment.getStatus());
        assertEquals(EnrollmentStatus.SUSPENDED, e2.getStatus());
    }

    @Test
    public void testCanAccessCourse_AccessRules() {
        // 1. Not enrolled (user not found)
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(null);
        assertFalse(enrollmentService.canAccessCourse("nonexistent@example.com", testCourse.getId()));

        // 2. Active enrollment
        when(userRepository.findByEmail("alice@example.com")).thenReturn(testStudent1);
        when(enrollmentRepository.findByUserEmailAndCourseId("alice@example.com", testCourse.getId())).thenReturn(Optional.of(activeEnrollment));
        assertTrue(enrollmentService.canAccessCourse("alice@example.com", testCourse.getId()));

        // 3. Suspended enrollment
        activeEnrollment.setStatus(EnrollmentStatus.SUSPENDED);
        assertFalse(enrollmentService.canAccessCourse("alice@example.com", testCourse.getId()));

        // 4. Completed enrollment
        activeEnrollment.setStatus(EnrollmentStatus.COMPLETED);
        assertTrue(enrollmentService.canAccessCourse("alice@example.com", testCourse.getId()));

        // 5. Expired enrollment
        activeEnrollment.setStatus(EnrollmentStatus.ACTIVE);
        activeEnrollment.setExpiryDate(LocalDateTime.now().minusDays(1)); // Expired yesterday
        assertFalse(enrollmentService.canAccessCourse("alice@example.com", testCourse.getId()));
    }

    @Test
    public void testExportEnrollmentsToCsv() {
        when(enrollmentRepository.findFilteredForExport(any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(activeEnrollment));

        byte[] csvBytes = enrollmentService.exportEnrollmentsToCsv(null, null, null, null, null);
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);

        String csvString = new String(csvBytes);
        assertTrue(csvString.contains("Enrollment ID"));
        assertTrue(csvString.contains("Student Name"));
        assertTrue(csvString.contains("alice@example.com"));
        assertTrue(csvString.contains("Fullstack Java Bootcamp"));
    }

    @Test
    public void testGetEnrollmentStatsAndAnalytics() {
        when(enrollmentRepository.count()).thenReturn(10L);
        when(enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE)).thenReturn(7L);
        when(enrollmentRepository.countByStatus(EnrollmentStatus.COMPLETED)).thenReturn(2L);
        when(enrollmentRepository.countByStatus(EnrollmentStatus.SUSPENDED)).thenReturn(1L);
        when(enrollmentRepository.countByStatus(EnrollmentStatus.EXPIRED)).thenReturn(0L);
        when(enrollmentRepository.countByStatus(EnrollmentStatus.CANCELLED)).thenReturn(0L);
        when(enrollmentRepository.countByStatus(EnrollmentStatus.REVOKED)).thenReturn(0L);
        when(enrollmentRepository.countByStatus(EnrollmentStatus.PENDING)).thenReturn(0L);
        when(enrollmentRepository.countByEnrolledAtBetween(any(), any())).thenReturn(5L);
        when(enrollmentRepository.findTopEnrolledCoursesRaw()).thenReturn(Collections.emptyList());
        when(enrollmentRepository.findDailyEnrollmentCountsSince(any())).thenReturn(Collections.emptyList());

        EnrollmentStatsDTO stats = enrollmentService.getEnrollmentStats();
        assertNotNull(stats);
        assertEquals(10L, stats.getTotalEnrollments());
        assertEquals(7L, stats.getActiveEnrollments());
        assertEquals(2L, stats.getCompletedEnrollments());

        EnrollmentAnalyticsDTO analytics = enrollmentService.getEnrollmentAnalytics();
        assertNotNull(analytics);
        assertNotNull(analytics.getStats());
        assertFalse(analytics.getStatusBreakdown().isEmpty());
    }
}
