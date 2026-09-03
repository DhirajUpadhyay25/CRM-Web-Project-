package in.project.main.services;

import in.project.main.dto.*;
import in.project.main.entities.*;
import in.project.main.entities.enums.CertificateStatus;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.NotificationType;
import in.project.main.repositories.*;
import in.project.main.services.impl.CertificateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CertificateServiceTest {

    @Mock private CertificateRepository certificateRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CertificateServiceImpl certificateService;

    private User testStudent;
    private Course testCourse;
    private Enrollment testEnrollment;
    private Certificate testCertificate;

    @BeforeEach
    void setUp() {
        testStudent = new User();
        testStudent.setId(10L);
        testStudent.setName("Alice Cooper");
        testStudent.setEmail("alice@example.com");
        testStudent.setBanStatus(false);

        testCourse = new Course();
        testCourse.setId(100L);
        testCourse.setName("Spring Boot Microservices Masterclass");
        testCourse.setInstructor("Dr. John Doe");

        testEnrollment = new Enrollment();
        testEnrollment.setId(500L);
        testEnrollment.setUser(testStudent);
        testEnrollment.setCourse(testCourse);
        testEnrollment.setStatus(EnrollmentStatus.ACTIVE);

        testCertificate = new Certificate();
        testCertificate.setId(1L);
        testCertificate.setCertificateNumber("EDU-2026-000101");
        testCertificate.setVerificationCode("8F7K2M9P");
        testCertificate.setCertificateUuid(UUID.randomUUID().toString());
        testCertificate.setStatus(CertificateStatus.REQUESTED);
        testCertificate.setStudent(testStudent);
        testCertificate.setStudentName("Alice Cooper");
        testCertificate.setStudentEmail("alice@example.com");
        testCertificate.setCourse(testCourse);
        testCertificate.setCourseName("Spring Boot Microservices Masterclass");
        testCertificate.setEnrollment(testEnrollment);
    }

    @Test
    void testIsEligibleForCertificate_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(testStudent);
        when(enrollmentRepository.findByUserEmailAndCourseId("alice@example.com", 100L)).thenReturn(Optional.of(testEnrollment));

        Lesson l1 = new Lesson();
        Lesson l2 = new Lesson();
        when(lessonRepository.findByCourseId("100")).thenReturn(List.of(l1, l2));
        when(lessonProgressRepository.countByUserEmailAndCourseIdAndCompleted("alice@example.com", 100L, true)).thenReturn(2L);

        Quiz q1 = new Quiz();
        q1.setId(1L);
        when(quizRepository.findByCourseId(100L)).thenReturn(List.of(q1));
        when(quizAttemptRepository.countByUserEmailAndQuizIdAndPassed("alice@example.com", 1L, true)).thenReturn(1L);

        boolean eligible = certificateService.isEligibleForCertificate("alice@example.com", 100L);
        assertTrue(eligible);
    }

    @Test
    void testIsEligibleForCertificate_IncompleteLessons_ReturnsFalse() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(testStudent);
        when(enrollmentRepository.findByUserEmailAndCourseId("alice@example.com", 100L)).thenReturn(Optional.of(testEnrollment));

        Lesson l1 = new Lesson();
        Lesson l2 = new Lesson();
        when(lessonRepository.findByCourseId("100")).thenReturn(List.of(l1, l2));
        // Only 1 completed out of 2
        when(lessonProgressRepository.countByUserEmailAndCourseIdAndCompleted("alice@example.com", 100L, true)).thenReturn(1L);

        boolean eligible = certificateService.isEligibleForCertificate("alice@example.com", 100L);
        assertFalse(eligible);
    }

    @Test
    void testRequestCertificate_Success() {
        when(enrollmentRepository.findById(500L)).thenReturn(Optional.of(testEnrollment));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(testStudent);
        when(enrollmentRepository.findByUserEmailAndCourseId("alice@example.com", 100L)).thenReturn(Optional.of(testEnrollment));
        when(lessonRepository.findByCourseId("100")).thenReturn(List.of(new Lesson()));
        when(lessonProgressRepository.countByUserEmailAndCourseIdAndCompleted("alice@example.com", 100L, true)).thenReturn(1L);
        when(quizRepository.findByCourseId(100L)).thenReturn(Collections.emptyList());

        when(certificateRepository.findByEnrollmentId(500L)).thenReturn(Optional.empty());
        when(certificateRepository.findByStudentEmailAndCourseId("alice@example.com", 100L)).thenReturn(Optional.empty());
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate c = invocation.getArgument(0);
            c.setId(99L);
            return c;
        });

        CertificateDTO result = certificateService.requestCertificate("alice@example.com", 500L, "Ready for certification");

        assertNotNull(result);
        assertEquals(CertificateStatus.REQUESTED, result.getStatus());
        assertEquals("alice@example.com", result.getStudentEmail());
        verify(notificationService).sendToStudent(eq("alice@example.com"), eq(NotificationType.CERTIFICATE_REQUESTED), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testApproveAndIssueCertificate_Success() {
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CertificateIssueDTO issueDto = new CertificateIssueDTO();
        issueDto.setTemplateCode("CLASSIC_GOLD");
        issueDto.setCertificateTitle("Certificate of Academic Excellence");

        CertificateDTO result = certificateService.approveAndIssueCertificate(1L, "admin@edutake.com", issueDto);

        assertNotNull(result);
        assertEquals(CertificateStatus.ISSUED, result.getStatus());
        assertTrue(result.getCertificateNumber().startsWith("EDU-"));
        assertNotNull(result.getVerificationCode());
        assertNotNull(result.getQrCodeData());
        assertEquals(LocalDate.now(), result.getIssueDate());
        verify(notificationService).sendToStudent(eq("alice@example.com"), eq(NotificationType.CERTIFICATE_ISSUED), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testRejectCertificateRequest_Success() {
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CertificateDTO result = certificateService.rejectCertificateRequest(1L, "admin@edutake.com", "Quizzes must be retaken.");

        assertNotNull(result);
        assertEquals(CertificateStatus.REJECTED, result.getStatus());
        assertEquals("Quizzes must be retaken.", result.getRejectionReason());
        verify(notificationService).sendToStudent(eq("alice@example.com"), eq(NotificationType.CERTIFICATE_REJECTED), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testRevokeCertificate_Success() {
        testCertificate.setStatus(CertificateStatus.ISSUED);
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(testCertificate));
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CertificateDTO result = certificateService.revokeCertificate(1L, "admin@edutake.com", "Academic integrity violation.");

        assertNotNull(result);
        assertEquals(CertificateStatus.REVOKED, result.getStatus());
        assertEquals("Academic integrity violation.", result.getRevocationReason());
        verify(notificationService).sendToStudent(eq("alice@example.com"), eq(NotificationType.CERTIFICATE_REVOKED), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testVerifyCertificatePublicly_Authentic() {
        testCertificate.setStatus(CertificateStatus.ISSUED);
        testCertificate.setIssueDate(LocalDate.now());
        when(certificateRepository.findByVerificationCode("8F7K2M9P")).thenReturn(Optional.of(testCertificate));

        PublicCertificateVerificationDTO verification = certificateService.verifyCertificatePublicly("8F7K2M9P");

        assertTrue(verification.isValid());
        assertEquals("Alice Cooper", verification.getStudentName());
        assertEquals("EDU-2026-000101", verification.getCertificateNumber());
        assertEquals(CertificateStatus.ISSUED, verification.getStatus());
    }

    @Test
    void testVerifyCertificatePublicly_Revoked() {
        testCertificate.setStatus(CertificateStatus.REVOKED);
        when(certificateRepository.findByVerificationCode("8F7K2M9P")).thenReturn(Optional.of(testCertificate));

        PublicCertificateVerificationDTO verification = certificateService.verifyCertificatePublicly("8F7K2M9P");

        assertFalse(verification.isValid());
        assertEquals(CertificateStatus.REVOKED, verification.getStatus());
        assertTrue(verification.getStatusMessage().contains("REVOKED"));
    }

    @Test
    void testVerifyCertificatePublicly_NotFound() {
        when(certificateRepository.findByVerificationCode("INVALID")).thenReturn(Optional.empty());

        PublicCertificateVerificationDTO verification = certificateService.verifyCertificatePublicly("INVALID");

        assertFalse(verification.isValid());
        assertTrue(verification.getStatusMessage().contains("No authentic educational credential"));
    }

    @Test
    void testExportCertificatesCsv() {
        when(certificateRepository.findWithFilters(isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(testCertificate)));

        byte[] csv = certificateService.exportCertificatesCsv(null, null, null);
        assertNotNull(csv);
        String content = new String(csv);
        assertTrue(content.contains("Certificate ID,Certificate Number"));
        assertTrue(content.contains("EDU-2026-000101"));
        assertTrue(content.contains("Alice Cooper"));
    }
}
