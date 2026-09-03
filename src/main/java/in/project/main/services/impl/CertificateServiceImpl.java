package in.project.main.services.impl;

import in.project.main.dto.*;
import in.project.main.entities.*;
import in.project.main.entities.enums.*;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.*;
import in.project.main.services.AuditLogService;
import in.project.main.services.CertificateService;
import in.project.main.services.NotificationService;
import in.project.main.utils.QRCodeGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class CertificateServiceImpl implements CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // No confusing 0/O, 1/I

    @Autowired private CertificateRepository certificateRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private LessonProgressRepository lessonProgressRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private NotificationService notificationService;
    @Autowired(required = false) private AuditLogService auditLogService;

    // =========================================================================
    // 1. ELIGIBILITY & STUDENT METHODS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CertificateDTO> getStudentCertificates(String email) {
        if (email == null || email.isBlank()) return Collections.emptyList();
        List<Certificate> list = certificateRepository.findByStudentEmailOrderByCreatedAtDesc(email.trim());
        return list.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> getEligibleEnrollmentsForStudent(String email) {
        if (email == null || email.isBlank()) return Collections.emptyList();
        List<Enrollment> enrollments = enrollmentRepository.findByUserEmailOrderByEnrolledAtDesc(email.trim());
        List<Enrollment> eligible = new ArrayList<>();

        for (Enrollment e : enrollments) {
            if (e.getCourse() == null) continue;
            // Check if certificate already claimed/issued for this course
            Optional<Certificate> existing = certificateRepository.findByStudentEmailAndCourseId(email, e.getCourse().getId());
            if (existing.isPresent()) {
                // Already in requested, approved, or issued state
                continue;
            }
            if (isEligibleForCertificate(email, e.getCourse().getId())) {
                eligible.add(e);
            }
        }
        return eligible;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEligibleForCertificate(String email, Long courseId) {
        if (email == null || courseId == null) return false;

        User user = userRepository.findByEmail(email.trim());
        if (user == null || user.isBanStatus()) return false;

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUserEmailAndCourseId(email.trim(), courseId);
        if (enrollmentOpt.isEmpty()) return false;

        Enrollment enrollment = enrollmentOpt.get();
        if (enrollment.getStatus() == EnrollmentStatus.SUSPENDED ||
            enrollment.getStatus() == EnrollmentStatus.CANCELLED ||
            enrollment.getStatus() == EnrollmentStatus.REVOKED) {
            return false;
        }

        // Check lessons progress
        List<Lesson> lessons = lessonRepository.findByCourseId(String.valueOf(courseId));
        if (lessons.isEmpty()) {
            return false; // Cannot certify an empty course
        }

        long completedLessons = lessonProgressRepository.countByUserEmailAndCourseIdAndCompleted(email, courseId, true);
        if (completedLessons < lessons.size()) {
            return false;
        }

        // Check quizzes passing
        List<Quiz> quizzes = quizRepository.findByCourseId(courseId);
        for (Quiz q : quizzes) {
            long passed = quizAttemptRepository.countByUserEmailAndQuizIdAndPassed(email, q.getId(), true);
            if (passed == 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public CertificateDTO requestCertificate(String email, Long enrollmentId, String studentNote) {
        if (email == null || enrollmentId == null) {
            throw new IllegalArgumentException("Email and enrollment ID are required.");
        }

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with ID: " + enrollmentId));

        if (!email.trim().equalsIgnoreCase(enrollment.getUserEmail())) {
            throw new SecurityException("You do not have access to this enrollment record.");
        }

        Course course = enrollment.getCourse();
        if (course == null) {
            throw new IllegalStateException("Associated course not found for enrollment.");
        }

        // Verify eligibility
        if (!isEligibleForCertificate(email, course.getId())) {
            throw new IllegalStateException("You have not met all course completion and assessment requirements yet.");
        }

        // Check existing certificate
        Optional<Certificate> existingOpt = certificateRepository.findByEnrollmentId(enrollment.getId());
        if (existingOpt.isEmpty()) {
            existingOpt = certificateRepository.findByStudentEmailAndCourseId(email, course.getId());
        }

        if (existingOpt.isPresent()) {
            Certificate existing = existingOpt.get();
            if (existing.getStatus() == CertificateStatus.ISSUED) {
                throw new IllegalStateException("Your certificate has already been issued: " + existing.getCertificateNumber());
            }
            if (existing.getStatus() == CertificateStatus.REQUESTED || existing.getStatus() == CertificateStatus.UNDER_REVIEW) {
                throw new IllegalStateException("Your certificate request is already pending administrator review.");
            }
            if (existing.getStatus() == CertificateStatus.APPROVED) {
                throw new IllegalStateException("Your certificate has already been approved and is being generated.");
            }
            // If previously rejected or eligible, update to REQUESTED
            existing.setStatus(CertificateStatus.REQUESTED);
            existing.setRequestDate(LocalDateTime.now());
            existing.setStudentRequestNote(studentNote != null ? studentNote.trim() : "");
            existing.setRejectionReason(null);
            Certificate saved = certificateRepository.save(existing);
            auditAndNotifyRequest(saved, email);
            return toDTO(saved);
        }

        // Create new Certificate in REQUESTED status
        User student = enrollment.getUser();
        Certificate cert = new Certificate();
        cert.setEnrollment(enrollment);
        cert.setStudent(student);
        cert.setCourse(course);
        cert.setStudentName(student != null && student.getName() != null && !student.getName().isBlank() ? student.getName() : email);
        cert.setStudentEmail(email);
        cert.setCourseName(course.getName());
        cert.setCourseCategory(course.getCategory() != null ? course.getCategory().getName() : "General");
        cert.setInstructorName(course.getInstructor() != null ? course.getInstructor() : "EduTake Faculty");
        cert.setStatus(CertificateStatus.REQUESTED);
        cert.setRequestDate(LocalDateTime.now());
        cert.setCompletionDate(enrollment.getCompletedAt() != null ? enrollment.getCompletedAt() : LocalDateTime.now());
        cert.setStudentRequestNote(studentNote != null ? studentNote.trim() : "");

        // Temporary tracking numbers until official issuance
        String tempCode = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        cert.setCertificateNumber(tempCode);
        cert.setVerificationCode(tempCode);
        cert.setCertificateUuid(UUID.randomUUID().toString());

        Certificate saved = certificateRepository.save(cert);
        auditAndNotifyRequest(saved, email);
        return toDTO(saved);
    }

    private void auditAndNotifyRequest(Certificate cert, String studentEmail) {
        // Notify Student
        notificationService.sendToStudent(
                studentEmail,
                NotificationType.CERTIFICATE_REQUESTED,
                "Certificate Request Received",
                "Your certificate request for '" + cert.getCourseName() + "' has been submitted for admin review.",
                "/student/certificates",
                "CERTIFICATE",
                String.valueOf(cert.getId())
        );

        // Notify Admins
        notificationService.sendToAdmins(
                NotificationType.CERTIFICATE_REQUESTED,
                "New Certificate Claim",
                "Student " + cert.getStudentName() + " requested a certificate for '" + cert.getCourseName() + "'.",
                "/admin/certificates",
                "CERTIFICATE",
                String.valueOf(cert.getId())
        );

        // Audit Log
        if (auditLogService != null) {
            try {
                auditLogService.record(
                        PlatformAuditEvent.of(
                                studentEmail,
                                AuditEventType.CERTIFICATE_REQUESTED,
                                "CERTIFICATE_CLAIM_SUBMITTED",
                                "Student " + cert.getStudentName() + " (" + studentEmail + ") requested certificate for course '" + cert.getCourseName() + "'."
                        )
                        .withActor(cert.getStudent() != null ? String.valueOf(cert.getStudent().getId()) : null, studentEmail, cert.getStudentName(), "STUDENT")
                        .withEntity("Certificate", String.valueOf(cert.getId()), cert.getCourseName())
                        .withSeverity(AuditSeverity.INFO)
                        .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // 2. ADMIN LIST & MANAGEMENT METHODS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CertificateDTO> getAdminCertificatesPage(String search, CertificateStatus status, Long courseId, Pageable pageable) {
        Page<Certificate> page = certificateRepository.findWithFilters(search, status, courseId, pageable);
        return page.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificateDTO> getPendingRequestsPage(Pageable pageable) {
        Page<Certificate> page = certificateRepository.findPendingRequests(pageable);
        return page.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateDTO getCertificateById(Long id) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found with ID: " + id));
        return toDTO(cert);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateDTO getCertificateByCode(String codeOrNumber) {
        if (codeOrNumber == null || codeOrNumber.isBlank()) return null;
        String clean = codeOrNumber.trim();
        Optional<Certificate> certOpt = certificateRepository.findByVerificationCode(clean);
        if (certOpt.isEmpty()) {
            certOpt = certificateRepository.findByCertificateNumber(clean);
        }
        return certOpt.map(this::toDTO).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateDTO getCertificateByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        return certificateRepository.findByCertificateUuid(uuid.trim()).map(this::toDTO).orElse(null);
    }

    @Override
    public CertificateDTO reviewCertificateRequest(Long id, String adminEmail, CertificateReviewDTO dto) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found with ID: " + id));

        if (cert.getStatus() == CertificateStatus.REQUESTED) {
            cert.setStatus(CertificateStatus.UNDER_REVIEW);
        }
        cert.setReviewedByAdmin(adminEmail);
        cert.setReviewedAt(LocalDateTime.now());
        if (dto != null && dto.getAdminNotes() != null) {
            cert.setAdminNotes(dto.getAdminNotes().trim());
        }

        Certificate saved = certificateRepository.save(cert);

        if (auditLogService != null) {
            try {
                auditLogService.record(
                        PlatformAuditEvent.of(
                                adminEmail,
                                AuditEventType.CERTIFICATE_REVIEW_STARTED,
                                "CERTIFICATE_REVIEW",
                                "Admin " + adminEmail + " started review of certificate request #" + cert.getId() + " for " + cert.getStudentName()
                        )
                        .withEntity("Certificate", String.valueOf(cert.getId()), cert.getCourseName())
                        .withSeverity(AuditSeverity.LOW)
                        .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception ignored) {}
        }

        return toDTO(saved);
    }

    @Override
    public CertificateDTO approveAndIssueCertificate(Long id, String adminEmail, CertificateIssueDTO dto) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found with ID: " + id));

        if (cert.getStatus() == CertificateStatus.ISSUED) {
            throw new IllegalStateException("Certificate #" + cert.getId() + " is already issued: " + cert.getCertificateNumber());
        }
        if (cert.getStatus() == CertificateStatus.REVOKED) {
            throw new IllegalStateException("Cannot issue a revoked certificate. Use reissue instead.");
        }

        // Apply custom title and template if provided
        if (dto != null) {
            if (dto.getCertificateTitle() != null && !dto.getCertificateTitle().isBlank()) {
                cert.setCertificateTitle(dto.getCertificateTitle().trim());
            }
            if (dto.getCertificateType() != null && !dto.getCertificateType().isBlank()) {
                cert.setCertificateType(dto.getCertificateType().trim());
            }
            if (dto.getTemplateCode() != null && !dto.getTemplateCode().isBlank()) {
                cert.setTemplateCode(dto.getTemplateCode().trim());
            }
            if (dto.getAdminNotes() != null && !dto.getAdminNotes().isBlank()) {
                cert.setAdminNotes(dto.getAdminNotes().trim());
            }
        }

        // Generate official unique Certificate Number & Secure Verification Code
        String certNumber = generateUniqueCertificateNumber();
        String verifyCode = generateUniqueVerificationCode();

        cert.setCertificateNumber(certNumber);
        cert.setVerificationCode(verifyCode);
        cert.setStatus(CertificateStatus.ISSUED);
        cert.setIssueDate(LocalDate.now());
        cert.setApprovedAt(LocalDateTime.now());
        cert.setApprovedByAdmin(adminEmail);
        cert.setReviewedByAdmin(adminEmail);
        cert.setReviewedAt(LocalDateTime.now());

        // Generate dynamic QR vector data
        String verificationUrl = "http://localhost:8080/verify/certificate/" + verifyCode;
        String qrDataUri = QRCodeGeneratorUtil.generateQrSvgDataUri(verificationUrl, 160);
        cert.setQrCodeData(qrDataUri);
        cert.setFileUrl("/student/certificates/" + cert.getId() + "/view");

        // Mark enrollment completed if not already
        if (cert.getEnrollment() != null) {
            Enrollment e = cert.getEnrollment();
            e.setStatus(EnrollmentStatus.COMPLETED);
            if (e.getCompletedAt() == null) {
                e.setCompletedAt(LocalDateTime.now());
            }
            enrollmentRepository.save(e);
        }

        Certificate saved = certificateRepository.save(cert);
        logger.info("Issued certificate {} to {} for course {} by admin {}", certNumber, saved.getStudentEmail(), saved.getCourseName(), adminEmail);

        // Notify Student
        notificationService.sendToStudent(
                saved.getStudentEmail(),
                NotificationType.CERTIFICATE_ISSUED,
                "Certificate Issued!",
                "Congratulations! Your official certificate for '" + saved.getCourseName() + "' has been approved and issued (Cert #" + certNumber + ").",
                "/student/certificates/" + saved.getId() + "/view",
                "CERTIFICATE",
                String.valueOf(saved.getId())
        );

        // Audit Log
        if (auditLogService != null) {
            try {
                auditLogService.record(
                        PlatformAuditEvent.of(
                                adminEmail,
                                AuditEventType.CERTIFICATE_ISSUED,
                                "CERTIFICATE_APPROVED_AND_ISSUED",
                                "Certificate " + certNumber + " officially issued to student " + saved.getStudentName() + " (" + saved.getStudentEmail() + ") for course '" + saved.getCourseName() + "' by " + adminEmail + "."
                        )
                        .withEntity("Certificate", String.valueOf(saved.getId()), certNumber)
                        .withSeverity(AuditSeverity.INFO)
                        .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception ignored) {}
        }

        return toDTO(saved);
    }

    @Override
    public CertificateDTO rejectCertificateRequest(Long id, String adminEmail, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is mandatory.");
        }

        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found with ID: " + id));

        if (cert.getStatus() == CertificateStatus.ISSUED) {
            throw new IllegalStateException("Cannot reject an already issued certificate. Use revoke instead.");
        }

        cert.setStatus(CertificateStatus.REJECTED);
        cert.setRejectionReason(rejectionReason.trim());
        cert.setReviewedByAdmin(adminEmail);
        cert.setReviewedAt(LocalDateTime.now());

        Certificate saved = certificateRepository.save(cert);
        logger.info("Rejected certificate request #{} for {} by admin {}. Reason: {}", id, saved.getStudentEmail(), adminEmail, rejectionReason);

        // Notify Student
        notificationService.sendToStudent(
                saved.getStudentEmail(),
                NotificationType.CERTIFICATE_REJECTED,
                "Certificate Request Not Approved",
                "Your certificate request for '" + saved.getCourseName() + "' was not approved: " + rejectionReason.trim(),
                "/student/certificates",
                "CERTIFICATE",
                String.valueOf(saved.getId())
        );

        // Audit Log
        if (auditLogService != null) {
            try {
                auditLogService.record(
                        PlatformAuditEvent.of(
                                adminEmail,
                                AuditEventType.CERTIFICATE_REJECTED,
                                "CERTIFICATE_REQUEST_REJECTED",
                                "Certificate request #" + id + " for " + saved.getStudentName() + " rejected by " + adminEmail + ". Reason: " + rejectionReason.trim()
                        )
                        .withEntity("Certificate", String.valueOf(saved.getId()), saved.getCourseName())
                        .withSeverity(AuditSeverity.MEDIUM)
                        .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception ignored) {}
        }

        return toDTO(saved);
    }

    @Override
    public CertificateDTO revokeCertificate(Long id, String adminEmail, String revocationReason) {
        if (revocationReason == null || revocationReason.isBlank()) {
            throw new IllegalArgumentException("Revocation reason is mandatory.");
        }

        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found with ID: " + id));

        cert.setStatus(CertificateStatus.REVOKED);
        cert.setRevokedByAdmin(adminEmail);
        cert.setRevokedAt(LocalDateTime.now());
        cert.setRevocationReason(revocationReason.trim());

        Certificate saved = certificateRepository.save(cert);
        logger.warn("Revoked certificate {} for {} by admin {}. Reason: {}", saved.getCertificateNumber(), saved.getStudentEmail(), adminEmail, revocationReason);

        // Notify Student
        notificationService.sendToStudent(
                saved.getStudentEmail(),
                NotificationType.CERTIFICATE_REVOKED,
                "Certificate Revoked",
                "Your certificate " + saved.getCertificateNumber() + " for '" + saved.getCourseName() + "' has been administratively revoked: " + revocationReason.trim(),
                "/student/certificates",
                "CERTIFICATE",
                String.valueOf(saved.getId())
        );

        // Audit Log
        if (auditLogService != null) {
            try {
                auditLogService.record(
                        PlatformAuditEvent.of(
                                adminEmail,
                                AuditEventType.CERTIFICATE_REVOKED,
                                "CERTIFICATE_REVOKED",
                                "Certificate " + saved.getCertificateNumber() + " for student " + saved.getStudentName() + " was revoked by " + adminEmail + ". Reason: " + revocationReason.trim()
                        )
                        .withEntity("Certificate", String.valueOf(saved.getId()), saved.getCertificateNumber())
                        .withSeverity(AuditSeverity.HIGH)
                        .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception ignored) {}
        }

        return toDTO(saved);
    }

    @Override
    public CertificateDTO reissueCertificate(Long id, String adminEmail, CertificateIssueDTO dto) {
        Certificate original = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found with ID: " + id));

        // Revoke / Supersede original
        original.setStatus(CertificateStatus.REVOKED);
        original.setRevokedByAdmin(adminEmail);
        original.setRevokedAt(LocalDateTime.now());
        original.setRevocationReason("Superseded by reissued certificate");
        original.setSuperseded(true);
        certificateRepository.save(original);

        // Create new corrected certificate
        Certificate newCert = new Certificate();
        newCert.setEnrollment(original.getEnrollment());
        newCert.setStudent(original.getStudent());
        newCert.setCourse(original.getCourse());
        newCert.setStudentName(original.getStudentName());
        newCert.setStudentEmail(original.getStudentEmail());
        newCert.setCourseName(original.getCourseName());
        newCert.setCourseCategory(original.getCourseCategory());
        newCert.setInstructorName(original.getInstructorName());
        newCert.setCompletionDate(original.getCompletionDate());
        newCert.setRequestDate(LocalDateTime.now());
        newCert.setReissuedFromCertificateId(original.getId());

        String certNumber = generateUniqueCertificateNumber();
        String verifyCode = generateUniqueVerificationCode();

        newCert.setCertificateNumber(certNumber);
        newCert.setVerificationCode(verifyCode);
        newCert.setStatus(CertificateStatus.ISSUED);
        newCert.setIssueDate(LocalDate.now());
        newCert.setApprovedAt(LocalDateTime.now());
        newCert.setApprovedByAdmin(adminEmail);
        newCert.setReviewedByAdmin(adminEmail);
        newCert.setReviewedAt(LocalDateTime.now());

        if (dto != null) {
            if (dto.getCertificateTitle() != null) newCert.setCertificateTitle(dto.getCertificateTitle().trim());
            if (dto.getTemplateCode() != null) newCert.setTemplateCode(dto.getTemplateCode().trim());
            if (dto.getAdminNotes() != null) newCert.setAdminNotes(dto.getAdminNotes().trim());
        }

        String verificationUrl = "http://localhost:8080/verify/certificate/" + verifyCode;
        newCert.setQrCodeData(QRCodeGeneratorUtil.generateQrSvgDataUri(verificationUrl, 160));
        newCert.setFileUrl("/student/certificates/" + newCert.getId() + "/view");

        Certificate saved = certificateRepository.save(newCert);

        // Audit Log
        if (auditLogService != null) {
            try {
                auditLogService.record(
                        PlatformAuditEvent.of(
                                adminEmail,
                                AuditEventType.CERTIFICATE_REISSUED,
                                "CERTIFICATE_REISSUED",
                                "Reissued certificate " + certNumber + " (superseding " + original.getCertificateNumber() + ") for " + saved.getStudentName() + " by " + adminEmail
                        )
                        .withEntity("Certificate", String.valueOf(saved.getId()), certNumber)
                        .withSeverity(AuditSeverity.MEDIUM)
                        .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception ignored) {}
        }

        return toDTO(saved);
    }

    // =========================================================================
    // 3. PUBLIC VERIFICATION
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public PublicCertificateVerificationDTO verifyCertificatePublicly(String codeOrNumber) {
        PublicCertificateVerificationDTO result = new PublicCertificateVerificationDTO();
        if (codeOrNumber == null || codeOrNumber.isBlank()) {
            result.setValid(false);
            result.setStatusMessage("No verification code provided.");
            return result;
        }

        String clean = codeOrNumber.trim();
        Optional<Certificate> certOpt = certificateRepository.findByVerificationCode(clean);
        if (certOpt.isEmpty()) {
            certOpt = certificateRepository.findByCertificateNumber(clean);
        }
        if (certOpt.isEmpty() && clean.matches("\\d+")) {
            certOpt = certificateRepository.findByEnrollmentId(Long.parseLong(clean));
        }

        if (certOpt.isEmpty()) {
            result.setValid(false);
            result.setVerificationCode(clean);
            result.setStatusMessage("No authentic educational credential was found matching code: " + clean);
            return result;
        }

        Certificate cert = certOpt.get();
        result.setStatus(cert.getStatus());
        result.setCertificateNumber(cert.getCertificateNumber());
        result.setVerificationCode(cert.getVerificationCode());
        result.setStudentName(cert.getStudentName());
        result.setCourseName(cert.getCourseName());
        result.setCourseCategory(cert.getCourseCategory());
        result.setInstructorName(cert.getInstructorName());
        result.setCertificateTitle(cert.getCertificateTitle());
        result.setIssueDate(cert.getIssueDate());
        result.setCompletionDate(cert.getCompletionDate());
        result.setIssuerOrganization("EduTake Learning Academy");
        result.setVerificationUrl("http://localhost:8080/verify/certificate/" + cert.getVerificationCode());

        if (cert.getStatus() == CertificateStatus.ISSUED) {
            result.setValid(true);
            result.setStatusMessage("Authentic credential officially issued and verified by EduTake.");
        } else if (cert.getStatus() == CertificateStatus.REVOKED) {
            result.setValid(false);
            result.setStatusMessage("Notice: This credential has been administratively REVOKED and is no longer valid.");
        } else {
            result.setValid(false);
            result.setStatusMessage("This credential is in " + cert.getStatus().getDisplayName() + " state and cannot be verified as active.");
        }

        return result;
    }

    // =========================================================================
    // 4. METRICS, ANALYTICS & EXPORT
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public CertificateStatsDTO getAdminCertificateStats() {
        CertificateStatsDTO stats = new CertificateStatsDTO();
        stats.setTotalCertificates(certificateRepository.count());
        stats.setIssuedCertificates(certificateRepository.countByStatus(CertificateStatus.ISSUED));
        stats.setPendingRequests(certificateRepository.countByStatus(CertificateStatus.REQUESTED));
        stats.setUnderReview(certificateRepository.countByStatus(CertificateStatus.UNDER_REVIEW));
        stats.setRejectedRequests(certificateRepository.countByStatus(CertificateStatus.REJECTED));
        stats.setRevokedCertificates(certificateRepository.countByStatus(CertificateStatus.REVOKED));

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        stats.setIssuedThisMonth(certificateRepository.countIssuedSince(startOfMonth));

        // Count eligible completions awaiting claims
        List<Enrollment> completedEnrollments = enrollmentRepository.findByStatus(EnrollmentStatus.COMPLETED);
        long eligibleCount = 0;
        for (Enrollment e : completedEnrollments) {
            if (e.getCourse() != null && !certificateRepository.findByEnrollmentId(e.getId()).isPresent()) {
                eligibleCount++;
            }
        }
        stats.setEligiblePendingClaim(eligibleCount);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateAnalyticsDTO getAdminCertificateAnalytics() {
        CertificateAnalyticsDTO analytics = new CertificateAnalyticsDTO();
        CertificateStatsDTO stats = getAdminCertificateStats();
        analytics.setStats(stats);

        long totalDecided = stats.getIssuedCertificates() + stats.getRejectedRequests();
        if (totalDecided > 0) {
            analytics.setApprovalRate(Math.round((stats.getIssuedCertificates() * 100.0 / totalDecided) * 10.0) / 10.0);
            analytics.setRejectionRate(Math.round((stats.getRejectedRequests() * 100.0 / totalDecided) * 10.0) / 10.0);
        } else {
            analytics.setApprovalRate(100.0);
            analytics.setRejectionRate(0.0);
        }

        if (stats.getIssuedCertificates() > 0) {
            analytics.setRevocationRate(Math.round((stats.getRevokedCertificates() * 100.0 / stats.getIssuedCertificates()) * 10.0) / 10.0);
        } else {
            analytics.setRevocationRate(0.0);
        }

        // Top certified courses breakdown
        List<Map<String, Object>> courseBreakdown = new ArrayList<>();
        List<Course> courses = courseRepository.findAll();
        for (Course c : courses) {
            long count = certificateRepository.findAll().stream()
                    .filter(cert -> cert.getCourse() != null && cert.getCourse().getId().equals(c.getId()) && cert.getStatus() == CertificateStatus.ISSUED)
                    .count();
            if (count > 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("courseName", c.getName());
                map.put("category", c.getCategory() != null ? c.getCategory().getName() : "General");
                map.put("certifiedCount", count);
                courseBreakdown.add(map);
            }
        }
        courseBreakdown.sort((a, b) -> Long.compare((Long) b.get("certifiedCount"), (Long) a.get("certifiedCount")));
        analytics.setTopCertifiedCourses(courseBreakdown.stream().limit(8).toList());

        // Template distribution
        Map<String, Long> templateCounts = new HashMap<>();
        for (Certificate c : certificateRepository.findAll()) {
            if (c.getStatus() == CertificateStatus.ISSUED) {
                String t = c.getTemplateCode() != null ? c.getTemplateCode() : "CLASSIC_GOLD";
                templateCounts.put(t, templateCounts.getOrDefault(t, 0L) + 1);
            }
        }
        List<Map<String, Object>> templatesList = new ArrayList<>();
        templateCounts.forEach((k, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("templateCode", k);
            m.put("count", v);
            templatesList.add(m);
        });
        analytics.setTemplateDistribution(templatesList);

        return analytics;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCertificatesCsv(String search, CertificateStatus status, Long courseId) {
        Page<Certificate> page = certificateRepository.findWithFilters(search, status, courseId, Pageable.unpaged());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        // CSV Header
        writer.println("Certificate ID,Certificate Number,Verification Code,Status,Student Name,Student Email,Course Name,Instructor,Issue Date,Completion Date,Template");

        for (Certificate c : page.getContent()) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    c.getId(),
                    escapeCsv(c.getCertificateNumber()),
                    escapeCsv(c.getVerificationCode()),
                    c.getStatus() != null ? c.getStatus().name() : "",
                    escapeCsv(c.getStudentName()),
                    escapeCsv(c.getStudentEmail()),
                    escapeCsv(c.getCourseName()),
                    escapeCsv(c.getInstructorName()),
                    c.getIssueDate() != null ? c.getIssueDate().toString() : "",
                    c.getCompletionDate() != null ? c.getCompletionDate().toString() : "",
                    escapeCsv(c.getTemplateCode())
            );
        }
        writer.flush();
        return out.toByteArray();
    }

    @Override
    public void recordDownload(Long id, String studentEmail) {
        Certificate cert = certificateRepository.findById(id).orElse(null);
        if (cert != null) {
            cert.setDownloadCount(cert.getDownloadCount() + 1);
            certificateRepository.save(cert);

            if (auditLogService != null) {
                try {
                    auditLogService.record(
                            PlatformAuditEvent.of(
                                    studentEmail,
                                    AuditEventType.CERTIFICATE_DOWNLOADED,
                                    "CERTIFICATE_DOWNLOAD",
                                    "Student " + studentEmail + " downloaded certificate #" + cert.getCertificateNumber()
                            )
                            .withEntity("Certificate", String.valueOf(cert.getId()), cert.getCertificateNumber())
                            .withSeverity(AuditSeverity.LOW)
                            .withStatus(AuditStatus.SUCCESS)
                    );
                } catch (Exception ignored) {}
            }
        }
    }

    // =========================================================================
    // 5. HELPER GENERATORS & MAPPERS
    // =========================================================================

    private synchronized String generateUniqueCertificateNumber() {
        int currentYear = Year.now().getValue();
        List<Certificate> topList = certificateRepository.findTop1OrderByIdDesc();
        long nextSeq = 1001;
        if (!topList.isEmpty() && topList.get(0).getId() != null) {
            nextSeq = 1000 + topList.get(0).getId() + 1;
        }

        String candidate = String.format("EDU-%d-%06d", currentYear, nextSeq);
        int attempts = 0;
        while (certificateRepository.findByCertificateNumber(candidate).isPresent() && attempts < 20) {
            nextSeq++;
            candidate = String.format("EDU-%d-%06d", currentYear, nextSeq);
            attempts++;
        }
        return candidate;
    }

    private String generateUniqueVerificationCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        String candidate = sb.toString();
        while (certificateRepository.findByVerificationCode(candidate).isPresent()) {
            sb.setLength(0);
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            candidate = sb.toString();
        }
        return candidate;
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    private CertificateDTO toDTO(Certificate c) {
        if (c == null) return null;
        CertificateDTO dto = new CertificateDTO();
        dto.setId(c.getId());
        dto.setCertificateNumber(c.getCertificateNumber());
        dto.setVerificationCode(c.getVerificationCode());
        dto.setCertificateUuid(c.getCertificateUuid());
        dto.setStatus(c.getStatus());
        dto.setEnrollmentId(c.getEnrollment() != null ? c.getEnrollment().getId() : null);
        dto.setStudentId(c.getStudent() != null ? c.getStudent().getId() : null);
        dto.setStudentName(c.getStudentName());
        dto.setStudentEmail(c.getStudentEmail());
        dto.setCourseId(c.getCourse() != null ? c.getCourse().getId() : null);
        dto.setCourseName(c.getCourseName());
        dto.setCourseCategory(c.getCourseCategory());
        dto.setInstructorName(c.getInstructorName());
        dto.setCertificateTitle(c.getCertificateTitle());
        dto.setCertificateType(c.getCertificateType());
        dto.setTemplateCode(c.getTemplateCode() != null ? c.getTemplateCode() : "CLASSIC_GOLD");
        dto.setIssueDate(c.getIssueDate());
        dto.setCompletionDate(c.getCompletionDate());
        dto.setExpiryDate(c.getExpiryDate());
        dto.setRequestDate(c.getRequestDate());
        dto.setReviewedAt(c.getReviewedAt());
        dto.setApprovedAt(c.getApprovedAt());
        dto.setRevokedAt(c.getRevokedAt());
        dto.setStudentRequestNote(c.getStudentRequestNote());
        dto.setReviewedByAdmin(c.getReviewedByAdmin());
        dto.setApprovedByAdmin(c.getApprovedByAdmin());
        dto.setRejectionReason(c.getRejectionReason());
        dto.setRevokedByAdmin(c.getRevokedByAdmin());
        dto.setRevocationReason(c.getRevocationReason());
        dto.setAdminNotes(c.getAdminNotes());
        dto.setReissuedFromCertificateId(c.getReissuedFromCertificateId());
        dto.setSuperseded(c.isSuperseded());
        dto.setQrCodeData(c.getQrCodeData());
        dto.setFileUrl(c.getFileUrl());
        dto.setDownloadCount(c.getDownloadCount());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }
}
