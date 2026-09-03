package in.project.main.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Enrollment;
import in.project.main.entities.enums.EnrollmentStatus;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    long countByStatus(EnrollmentStatus status);

    List<Enrollment> findByStatus(EnrollmentStatus status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE LOWER(e.paymentStatus) = LOWER(:paymentStatus)")
    long countByPaymentStatusIgnoreCase(@Param("paymentStatus") String paymentStatus);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.enrolledAt >= :since")
    long countByEnrolledAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.enrolledAt >= :start AND e.enrolledAt <= :end")
    long countByEnrolledAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT e FROM Enrollment e " +
                   "JOIN FETCH e.user u " +
                   "JOIN FETCH e.course c " +
                   "LEFT JOIN FETCH c.category cat " +
                   "WHERE (:keyword IS NULL OR :keyword = '' OR " +
                   "       LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "       LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "       LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                   "       CAST(e.id AS string) = :keyword) " +
                   "AND (:status IS NULL OR e.status = :status) " +
                   "AND (:courseId IS NULL OR c.id = :courseId) " +
                   "AND (:paymentStatus IS NULL OR :paymentStatus = '' OR LOWER(e.paymentStatus) = LOWER(:paymentStatus)) " +
                   "AND (:enrollmentType IS NULL OR :enrollmentType = '' OR LOWER(e.enrollmentType) = LOWER(:enrollmentType)) " +
                   "AND (:enrollmentSource IS NULL OR :enrollmentSource = '' OR LOWER(e.enrollmentSource) = LOWER(:enrollmentSource)) " +
                   "AND (:startDate IS NULL OR e.enrolledAt >= :startDate) " +
                   "AND (:endDate IS NULL OR e.enrolledAt <= :endDate)",
           countQuery = "SELECT COUNT(e) FROM Enrollment e " +
                        "JOIN e.user u " +
                        "JOIN e.course c " +
                        "WHERE (:keyword IS NULL OR :keyword = '' OR " +
                        "       LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "       LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "       LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "       CAST(e.id AS string) = :keyword) " +
                        "AND (:status IS NULL OR e.status = :status) " +
                        "AND (:courseId IS NULL OR c.id = :courseId) " +
                        "AND (:paymentStatus IS NULL OR :paymentStatus = '' OR LOWER(e.paymentStatus) = LOWER(:paymentStatus)) " +
                        "AND (:enrollmentType IS NULL OR :enrollmentType = '' OR LOWER(e.enrollmentType) = LOWER(:enrollmentType)) " +
                        "AND (:enrollmentSource IS NULL OR :enrollmentSource = '' OR LOWER(e.enrollmentSource) = LOWER(:enrollmentSource)) " +
                        "AND (:startDate IS NULL OR e.enrolledAt >= :startDate) " +
                        "AND (:endDate IS NULL OR e.enrolledAt <= :endDate)")
    Page<Enrollment> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") EnrollmentStatus status,
            @Param("courseId") Long courseId,
            @Param("paymentStatus") String paymentStatus,
            @Param("enrollmentType") String enrollmentType,
            @Param("enrollmentSource") String enrollmentSource,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT e FROM Enrollment e " +
           "JOIN FETCH e.user u " +
           "JOIN FETCH e.course c " +
           "LEFT JOIN FETCH c.category cat " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR " +
           "       LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "       CAST(e.id AS string) = :keyword) " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:courseId IS NULL OR c.id = :courseId) " +
           "AND (:paymentStatus IS NULL OR :paymentStatus = '' OR LOWER(e.paymentStatus) = LOWER(:paymentStatus)) " +
           "AND (:enrollmentType IS NULL OR :enrollmentType = '' OR LOWER(e.enrollmentType) = LOWER(:enrollmentType)) " +
           "ORDER BY e.enrolledAt DESC")
    List<Enrollment> findFilteredForExport(
            @Param("keyword") String keyword,
            @Param("status") EnrollmentStatus status,
            @Param("courseId") Long courseId,
            @Param("paymentStatus") String paymentStatus,
            @Param("enrollmentType") String enrollmentType);

    @Query("SELECT e FROM Enrollment e " +
           "JOIN FETCH e.user u " +
           "JOIN FETCH e.course c " +
           "LEFT JOIN FETCH c.category cat " +
           "WHERE e.id = :id")
    Optional<Enrollment> findByIdWithDetails(@Param("id") Long id);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.user.email = :email AND e.status = :status")
    long countByUserEmailAndStatus(@Param("email") String email, @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course c LEFT JOIN FETCH c.category WHERE e.user.email = :email ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByUserEmailOrderByEnrolledAtDesc(@Param("email") String email);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.user JOIN FETCH e.course c LEFT JOIN FETCH c.category WHERE e.user.id = :userId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByUserIdWithCourse(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT e.user.id) FROM Enrollment e")
    long countDistinctEnrolledUsers();

    @Query("SELECT COUNT(DISTINCT e.user.id) FROM Enrollment e WHERE e.status = :status")
    long countDistinctEnrolledUsersByStatus(@Param("status") EnrollmentStatus status);

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.user JOIN FETCH e.course WHERE e.user.id = :userId")
    List<Enrollment> findAllByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course c LEFT JOIN FETCH c.category JOIN FETCH e.user WHERE e.user.email = :email AND e.course.id = :courseId")
    Optional<Enrollment> findByUserEmailAndCourseId(@Param("email") String email, @Param("courseId") Long courseId);

    long countByCourseId(Long courseId);
    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
    List<Enrollment> findByCourseId(Long courseId);
    List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e " +
           "JOIN FETCH e.user u " +
           "JOIN FETCH e.course c " +
           "WHERE c.id IN :courseIds " +
           "ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByCourseIdInWithDetails(@Param("courseIds") List<Long> courseIds);

    @Query("SELECT e.course.id AS courseId, e.course.name AS courseName, COUNT(e) AS totalCount " +
           "FROM Enrollment e GROUP BY e.course.id, e.course.name ORDER BY COUNT(e) DESC")
    List<Object[]> findTopEnrolledCoursesRaw();

    @Query("SELECT DATE(e.enrolledAt) AS enrollDate, COUNT(e) AS dayCount " +
           "FROM Enrollment e WHERE e.enrolledAt >= :since " +
           "GROUP BY DATE(e.enrolledAt) ORDER BY DATE(e.enrolledAt) ASC")
    List<Object[]> findDailyEnrollmentCountsSince(@Param("since") LocalDateTime since);
}

