package in.project.main.repositories;

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

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.user JOIN FETCH e.course WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(e.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.course.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Enrollment> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") EnrollmentStatus status,
            Pageable pageable);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.user.email = :email AND e.status = :status")
    long countByUserEmailAndStatus(@Param("email") String email, @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course WHERE e.user.email = :email ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByUserEmailOrderByEnrolledAtDesc(@Param("email") String email);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.user JOIN FETCH e.course WHERE e.user.id = :userId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findByUserIdWithCourse(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT e.user.id) FROM Enrollment e")
    long countDistinctEnrolledUsers();

    @Query("SELECT COUNT(DISTINCT e.user.id) FROM Enrollment e WHERE e.status = :status")
    long countDistinctEnrolledUsersByStatus(@Param("status") EnrollmentStatus status);

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.user JOIN FETCH e.course WHERE e.user.id = :userId")
    List<Enrollment> findAllByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course JOIN FETCH e.user WHERE e.user.email = :email AND e.course.id = :courseId")
    Optional<Enrollment> findByUserEmailAndCourseId(@Param("email") String email, @Param("courseId") Long courseId);

    long countByCourseId(Long courseId);
    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
    List<Enrollment> findByCourseId(Long courseId);
    List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
}
