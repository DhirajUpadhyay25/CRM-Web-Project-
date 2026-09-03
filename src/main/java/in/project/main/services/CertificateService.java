package in.project.main.services;

import in.project.main.dto.*;
import in.project.main.entities.Certificate;
import in.project.main.entities.Enrollment;
import in.project.main.entities.enums.CertificateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CertificateService {

    List<CertificateDTO> getStudentCertificates(String email);

    List<Enrollment> getEligibleEnrollmentsForStudent(String email);

    boolean isEligibleForCertificate(String email, Long courseId);

    CertificateDTO requestCertificate(String email, Long enrollmentId, String studentNote);

    Page<CertificateDTO> getAdminCertificatesPage(String search, CertificateStatus status, Long courseId, Pageable pageable);

    Page<CertificateDTO> getPendingRequestsPage(Pageable pageable);

    CertificateDTO getCertificateById(Long id);

    CertificateDTO getCertificateByCode(String codeOrNumber);

    CertificateDTO getCertificateByUuid(String uuid);

    CertificateDTO reviewCertificateRequest(Long id, String adminEmail, CertificateReviewDTO dto);

    CertificateDTO approveAndIssueCertificate(Long id, String adminEmail, CertificateIssueDTO dto);

    CertificateDTO rejectCertificateRequest(Long id, String adminEmail, String rejectionReason);

    CertificateDTO revokeCertificate(Long id, String adminEmail, String revocationReason);

    CertificateDTO reissueCertificate(Long id, String adminEmail, CertificateIssueDTO dto);

    PublicCertificateVerificationDTO verifyCertificatePublicly(String codeOrNumber);

    CertificateStatsDTO getAdminCertificateStats();

    CertificateAnalyticsDTO getAdminCertificateAnalytics();

    byte[] exportCertificatesCsv(String search, CertificateStatus status, Long courseId);

    void recordDownload(Long id, String studentEmail);
}
