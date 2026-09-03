package in.project.main.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.EducationApplication;
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
import in.project.main.repositories.CategoryRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.UserRepository;

@SpringBootTest(classes = EducationApplication.class)
@Transactional
public class EnrollmentServiceTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testStudent1;
    private User testStudent2;
    private User bannedStudent;
    private Course testCourse;
    private Category testCategory;

    @BeforeEach
    public void setUp() {
        testCategory = new Category();
        testCategory.setName("Engineering " + System.currentTimeMillis());
        testCategory = categoryRepository.save(testCategory);

        testCourse = new Course();
        testCourse.setName("Fullstack Java Bootcamp " + System.currentTimeMillis());
        testCourse.setSlug("fullstack-java-" + System.currentTimeMillis());
        testCourse.setCategory(testCategory);
        testCourse.setLevel(CourseLevel.BEGINNER);
        testCourse.setStatus(CourseStatus.PUBLISHED);
        testCourse.setOriginalPrice(BigDecimal.valueOf(4999));
        testCourse.setDiscountedPrice(BigDecimal.valueOf(1999));
        testCourse.setInstructor("Prof. Alan Turing");
        testCourse.setInstructorEmail("alan.turing@edutake.com");
        testCourse = courseRepository.save(testCourse);

        testStudent1 = new User();
        testStudent1.setName("Alice Walker");
        testStudent1.setEmail("alice." + System.currentTimeMillis() + "@example.com");
        testStudent1.setPassword("password123");
        testStudent1.setPhoneno("9876543210");
        testStudent1.setCity("New York");
        testStudent1.setBanStatus(false);
        testStudent1 = userRepository.save(testStudent1);

        testStudent2 = new User();
        testStudent2.setName("Bob Smith");
        testStudent2.setEmail("bob." + System.currentTimeMillis() + "@example.com");
        testStudent2.setPassword("password123");
        testStudent2.setPhoneno("9876543211");
        testStudent2.setCity("Chicago");
        testStudent2.setBanStatus(false);
        testStudent2 = userRepository.save(testStudent2);

        bannedStudent = new User();
        bannedStudent.setName("Charlie Banned");
        bannedStudent.setEmail("banned." + System.currentTimeMillis() + "@example.com");
        bannedStudent.setPassword("password123");
        bannedStudent.setPhoneno("9876543212");
        bannedStudent.setCity("Boston");
        bannedStudent.setBanStatus(true);
        bannedStudent = userRepository.save(bannedStudent);
    }

    @Test
    public void testManualEnrollStudent_Success() {
        ManualEnrollmentDTO dto = new ManualEnrollmentDTO();
        dto.setStudentId(testStudent1.getId());
        dto.setCourseId(testCourse.getId());
        dto.setEnrollmentType("ADMIN_ASSIGNED");
        dto.setAdminNote("Granted via scholarship");
        dto.setNotifyStudent(false);

        EnrollmentDTO result = enrollmentService.manualEnrollStudent(dto, "admin@edutake.com");

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
        assertEquals("Alice Walker", result.getStudentName());
        assertEquals(testCourse.getName(), result.getCourseName());
        assertEquals("ADMIN_ASSIGNED", result.getEnrollmentType());
        assertTrue(result.isAccessAllowed());
    }

    @Test
    public void testManualEnrollStudent_AlreadyEnrolled_ThrowsException() {
        ManualEnrollmentDTO dto = new ManualEnrollmentDTO();
        dto.setStudentId(testStudent1.getId());
        dto.setCourseId(testCourse.getId());

        enrollmentService.manualEnrollStudent(dto, "admin@edutake.com");

        // Attempting to enroll again while ACTIVE should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            enrollmentService.manualEnrollStudent(dto, "admin@edutake.com");
        });
    }

    @Test
    public void testManualEnrollStudent_BannedUser_ThrowsException() {
        ManualEnrollmentDTO dto = new ManualEnrollmentDTO();
        dto.setStudentId(bannedStudent.getId());
        dto.setCourseId(testCourse.getId());

        assertThrows(IllegalStateException.class, () -> {
            enrollmentService.manualEnrollStudent(dto, "admin@edutake.com");
        });
    }

    @Test
    public void testUpdateEnrollmentStatus_Suspend_RequiresReason() {
        ManualEnrollmentDTO enrollDto = new ManualEnrollmentDTO();
        enrollDto.setStudentId(testStudent1.getId());
        enrollDto.setCourseId(testCourse.getId());
        EnrollmentDTO created = enrollmentService.manualEnrollStudent(enrollDto, "admin@edutake.com");

        EnrollmentStatusUpdateDTO updateDto = new EnrollmentStatusUpdateDTO();
        updateDto.setStatus(EnrollmentStatus.SUSPENDED);
        updateDto.setReason(""); // Empty reason

        assertThrows(IllegalArgumentException.class, () -> {
            enrollmentService.updateEnrollmentStatus(created.getId(), updateDto, "admin@edutake.com");
        });
    }

    @Test
    public void testUpdateEnrollmentStatus_SuspendAndResume() {
        ManualEnrollmentDTO enrollDto = new ManualEnrollmentDTO();
        enrollDto.setStudentId(testStudent1.getId());
        enrollDto.setCourseId(testCourse.getId());
        EnrollmentDTO created = enrollmentService.manualEnrollStudent(enrollDto, "admin@edutake.com");

        // Suspend
        EnrollmentStatusUpdateDTO suspendDto = new EnrollmentStatusUpdateDTO(EnrollmentStatus.SUSPENDED, "Pending verification of credentials");
        EnrollmentDTO suspended = enrollmentService.updateEnrollmentStatus(created.getId(), suspendDto, "admin@edutake.com");
        assertEquals(EnrollmentStatus.SUSPENDED, suspended.getStatus());
        assertFalse(suspended.isAccessAllowed());

        // Resume / Reactivate
        EnrollmentStatusUpdateDTO resumeDto = new EnrollmentStatusUpdateDTO(EnrollmentStatus.ACTIVE, "Verification completed");
        EnrollmentDTO resumed = enrollmentService.updateEnrollmentStatus(created.getId(), resumeDto, "admin@edutake.com");
        assertEquals(EnrollmentStatus.ACTIVE, resumed.getStatus());
        assertTrue(resumed.isAccessAllowed());
    }

    @Test
    public void testUpdateEnrollmentStatus_Complete() {
        ManualEnrollmentDTO enrollDto = new ManualEnrollmentDTO();
        enrollDto.setStudentId(testStudent1.getId());
        enrollDto.setCourseId(testCourse.getId());
        EnrollmentDTO created = enrollmentService.manualEnrollStudent(enrollDto, "admin@edutake.com");

        EnrollmentStatusUpdateDTO completeDto = new EnrollmentStatusUpdateDTO(EnrollmentStatus.COMPLETED, "Completed all curriculum requirements");
        EnrollmentDTO completed = enrollmentService.updateEnrollmentStatus(created.getId(), completeDto, "admin@edutake.com");

        assertEquals(EnrollmentStatus.COMPLETED, completed.getStatus());
        assertTrue(completed.isAccessAllowed());
    }

    @Test
    public void testBulkEnrollStudents_SuccessAndSkipped() {
        // First enroll student 1 manually
        ManualEnrollmentDTO enrollDto = new ManualEnrollmentDTO();
        enrollDto.setStudentId(testStudent1.getId());
        enrollDto.setCourseId(testCourse.getId());
        enrollmentService.manualEnrollStudent(enrollDto, "admin@edutake.com");

        // Now run bulk enroll with student1 and student2
        BulkEnrollmentDTO bulkDto = new BulkEnrollmentDTO();
        bulkDto.setCourseId(testCourse.getId());
        bulkDto.setStudentEmails(Arrays.asList(testStudent1.getEmail(), testStudent2.getEmail()));
        bulkDto.setNotifyStudents(false);

        BulkEnrollmentResultDTO result = enrollmentService.bulkEnrollStudents(bulkDto, "admin@edutake.com");

        assertNotNull(result);
        assertEquals(2, result.getTotalRequested());
        assertEquals(1, result.getSuccessCount()); // Student 2 enrolled
        assertEquals(1, result.getSkippedAlreadyEnrolledCount()); // Student 1 skipped
        assertEquals(0, result.getFailedCount());
    }

    @Test
    public void testBulkUpdateStatus() {
        ManualEnrollmentDTO d1 = new ManualEnrollmentDTO();
        d1.setStudentId(testStudent1.getId());
        d1.setCourseId(testCourse.getId());
        EnrollmentDTO e1 = enrollmentService.manualEnrollStudent(d1, "admin@edutake.com");

        ManualEnrollmentDTO d2 = new ManualEnrollmentDTO();
        d2.setStudentId(testStudent2.getId());
        d2.setCourseId(testCourse.getId());
        EnrollmentDTO e2 = enrollmentService.manualEnrollStudent(d2, "admin@edutake.com");

        BulkEnrollmentStatusUpdateDTO bulkStatus = new BulkEnrollmentStatusUpdateDTO();
        bulkStatus.setEnrollmentIds(Arrays.asList(e1.getId(), e2.getId()));
        bulkStatus.setStatus(EnrollmentStatus.SUSPENDED);
        bulkStatus.setReason("Batch audit maintenance");
        bulkStatus.setNotifyStudents(false);

        BulkEnrollmentResultDTO result = enrollmentService.bulkUpdateStatus(bulkStatus, "admin@edutake.com");

        assertEquals(2, result.getTotalRequested());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());

        Enrollment updated1 = enrollmentRepository.findById(e1.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.SUSPENDED, updated1.getStatus());
    }

    @Test
    public void testCanAccessCourse_AccessRules() {
        // 1. Not enrolled
        assertFalse(enrollmentService.canAccessCourse(testStudent1.getEmail(), testCourse.getId()));

        // 2. Active enrollment
        ManualEnrollmentDTO d1 = new ManualEnrollmentDTO();
        d1.setStudentId(testStudent1.getId());
        d1.setCourseId(testCourse.getId());
        EnrollmentDTO e1 = enrollmentService.manualEnrollStudent(d1, "admin@edutake.com");
        assertTrue(enrollmentService.canAccessCourse(testStudent1.getEmail(), testCourse.getId()));

        // 3. Suspended enrollment
        enrollmentService.updateEnrollmentStatus(e1.getId(), new EnrollmentStatusUpdateDTO(EnrollmentStatus.SUSPENDED, "Suspended for test"), "admin@edutake.com");
        assertFalse(enrollmentService.canAccessCourse(testStudent1.getEmail(), testCourse.getId()));

        // 4. Completed enrollment
        enrollmentService.updateEnrollmentStatus(e1.getId(), new EnrollmentStatusUpdateDTO(EnrollmentStatus.COMPLETED, "Completed test"), "admin@edutake.com");
        assertTrue(enrollmentService.canAccessCourse(testStudent1.getEmail(), testCourse.getId()));

        // 5. Expired enrollment
        EnrollmentStatusUpdateDTO expireDto = new EnrollmentStatusUpdateDTO(EnrollmentStatus.ACTIVE, "Renewed");
        expireDto.setExpiryDate(LocalDateTime.now().minusDays(1)); // Expired yesterday
        enrollmentService.updateEnrollmentStatus(e1.getId(), expireDto, "admin@edutake.com");
        assertFalse(enrollmentService.canAccessCourse(testStudent1.getEmail(), testCourse.getId()));
    }

    @Test
    public void testExportEnrollmentsToCsv() {
        ManualEnrollmentDTO d1 = new ManualEnrollmentDTO();
        d1.setStudentId(testStudent1.getId());
        d1.setCourseId(testCourse.getId());
        enrollmentService.manualEnrollStudent(d1, "admin@edutake.com");

        byte[] csvBytes = enrollmentService.exportEnrollmentsToCsv(null, null, null, null, null);
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);

        String csvString = new String(csvBytes);
        assertTrue(csvString.contains("Enrollment ID"));
        assertTrue(csvString.contains("Student Name"));
        assertTrue(csvString.contains(testStudent1.getEmail()));
    }

    @Test
    public void testGetEnrollmentStatsAndAnalytics() {
        ManualEnrollmentDTO d1 = new ManualEnrollmentDTO();
        d1.setStudentId(testStudent1.getId());
        d1.setCourseId(testCourse.getId());
        enrollmentService.manualEnrollStudent(d1, "admin@edutake.com");

        EnrollmentStatsDTO stats = enrollmentService.getEnrollmentStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalEnrollments() >= 1);
        assertTrue(stats.getActiveEnrollments() >= 1);

        EnrollmentAnalyticsDTO analytics = enrollmentService.getEnrollmentAnalytics();
        assertNotNull(analytics);
        assertNotNull(analytics.getStats());
        assertFalse(analytics.getStatusBreakdown().isEmpty());
    }
}
