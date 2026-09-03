package in.project.main.repositories;

import in.project.main.entities.Certificate;
import in.project.main.entities.enums.CertificateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    Optional<Certificate> findByVerificationCode(String verificationCode);

    Optional<Certificate> findByCertificateUuid(String certificateUuid);

    Optional<Certificate> findByEnrollmentId(Long enrollmentId);

    List<Certificate> findByStudentEmailOrderByCreatedAtDesc(String studentEmail);

    List<Certificate> findByStudentEmailAndStatusOrderByCreatedAtDesc(String studentEmail, CertificateStatus status);

    Optional<Certificate> findByStudentEmailAndCourseId(String studentEmail, Long courseId);

    Optional<Certificate> findByStudentEmailAndCourseIdAndStatus(String studentEmail, Long courseId, CertificateStatus status);

    long countByStatus(CertificateStatus status);

    long countByStudentEmailAndStatus(String studentEmail, CertificateStatus status);

    @Query("SELECT COUNT(c) FROM Certificate c WHERE c.status = 'ISSUED' AND c.approvedAt >= :since")
    long countIssuedSince(@Param("since") LocalDateTime since);

    @Query("SELECT c FROM Certificate c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:courseId IS NULL OR (c.course IS NOT NULL AND c.course.id = :courseId)) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(c.studentName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.studentEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.courseName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.certificateNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.verificationCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Certificate> findWithFilters(
            @Param("search") String search,
            @Param("status") CertificateStatus status,
            @Param("courseId") Long courseId,
            Pageable pageable);

    @Query("SELECT c FROM Certificate c WHERE c.status IN ('REQUESTED', 'UNDER_REVIEW') ORDER BY c.requestDate ASC")
    Page<Certificate> findPendingRequests(Pageable pageable);

    @Query("SELECT c FROM Certificate c ORDER BY c.id DESC")
    List<Certificate> findTop1OrderByIdDesc();
}
