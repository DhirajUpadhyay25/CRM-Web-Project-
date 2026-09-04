package in.project.main.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.project.main.entities.Feedback;
import in.project.main.entities.enums.FeedbackStatus;
import in.project.main.entities.enums.FeedbackType;

public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {

    // --- Basic queries ---
    Page<Feedback> findByDeletedFalse(Pageable pageable);

    Page<Feedback> findByStudentIdAndDeletedFalse(Long studentId, Pageable pageable);

    List<Feedback> findByStudentIdAndDeletedFalse(Long studentId);

    Optional<Feedback> findByStudentIdAndCourseIdAndDeletedFalse(Long studentId, Long courseId);

    Page<Feedback> findByCourseIdAndDeletedFalse(Long courseId, Pageable pageable);

    List<Feedback> findByCourseIdAndDeletedFalse(Long courseId);

    Page<Feedback> findByInstructorIdAndDeletedFalse(Long instructorId, Pageable pageable);

    List<Feedback> findByStatusAndDeletedFalse(FeedbackStatus status);

    Page<Feedback> findByStatusAndDeletedFalse(FeedbackStatus status, Pageable pageable);

    Page<Feedback> findByFeedbackTypeAndDeletedFalse(FeedbackType type, Pageable pageable);

    // --- Analytics ---
    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(FeedbackStatus status);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.deleted = false AND f.rating IS NOT NULL")
    Double findAverageRating();

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.course.id = :courseId AND f.deleted = false AND f.rating IS NOT NULL")
    Double findAverageRatingByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.instructor.id = :instructorId AND f.deleted = false AND f.rating IS NOT NULL")
    Double findAverageRatingByInstructorId(@Param("instructorId") Long instructorId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.course.id = :courseId AND f.deleted = false AND f.rating IS NOT NULL")
    Long countRatingsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.instructor.id = :instructorId AND f.deleted = false AND f.rating IS NOT NULL")
    Long countRatingsByInstructorId(@Param("instructorId") Long instructorId);

    @Query("SELECT f.rating, COUNT(f) FROM Feedback f WHERE f.deleted = false AND f.rating IS NOT NULL GROUP BY f.rating ORDER BY f.rating")
    List<Object[]> findRatingDistribution();

    @Query("SELECT f.rating, COUNT(f) FROM Feedback f WHERE f.course.id = :courseId AND f.deleted = false AND f.rating IS NOT NULL GROUP BY f.rating ORDER BY f.rating")
    List<Object[]> findRatingDistributionByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT f.feedbackType, COUNT(f) FROM Feedback f WHERE f.deleted = false AND f.feedbackType IS NOT NULL GROUP BY f.feedbackType")
    List<Object[]> countByFeedbackType();

    @Query("SELECT f.category, COUNT(f) FROM Feedback f WHERE f.deleted = false AND f.category IS NOT NULL GROUP BY f.category")
    List<Object[]> countByCategory();

    @Query("SELECT f FROM Feedback f WHERE f.deleted = false AND f.course.id = :courseId ORDER BY f.createdAt DESC")
    List<Feedback> findRecentByCourseId(@Param("courseId") Long courseId, Pageable pageable);

    @Query("SELECT f FROM Feedback f WHERE f.deleted = false AND f.instructor.id = :instructorId ORDER BY f.createdAt DESC")
    List<Feedback> findRecentByInstructorId(@Param("instructorId") Long instructorId, Pageable pageable);

    @Query("SELECT f FROM Feedback f WHERE f.deleted = false AND f.isAnonymous = false AND f.allowTestimonial = true AND f.rating >= 4 AND f.status = in.project.main.entities.enums.FeedbackStatus.RESOLVED ORDER BY f.createdAt DESC")
    List<Feedback> findTestimonialCandidates(Pageable pageable);

    // --- Admin search/filter ---
    @Query("SELECT f FROM Feedback f " +
           "JOIN FETCH f.student s " +
           "LEFT JOIN FETCH f.course c " +
           "LEFT JOIN FETCH f.instructor i " +
           "LEFT JOIN FETCH f.enrollment e " +
           "WHERE f.deleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "  CAST(f.id AS string) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(f.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(f.message) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR f.status = :status) " +
           "AND (:rating IS NULL OR f.rating = :rating) " +
           "AND (:category IS NULL OR :category = '' OR f.category = :category) " +
           "AND (:courseId IS NULL OR c.id = :courseId) " +
           "AND (:instructorId IS NULL OR i.id = :instructorId) " +
           "AND (:minRating IS NULL OR f.rating >= :minRating) " +
           "AND (:startDate IS NULL OR f.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR f.createdAt <= :endDate)")
    Page<Feedback> adminSearchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") FeedbackStatus status,
            @Param("rating") Integer rating,
            @Param("category") String category,
            @Param("courseId") Long courseId,
            @Param("instructorId") Long instructorId,
            @Param("minRating") Integer minRating,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // --- Instructor search/filter ---
    @Query("SELECT f FROM Feedback f " +
           "JOIN FETCH f.student s " +
           "LEFT JOIN FETCH f.course c " +
           "LEFT JOIN FETCH f.instructor i " +
           "LEFT JOIN FETCH f.enrollment e " +
           "WHERE f.deleted = false " +
           "AND (i.id = :instructorId OR (i IS NULL AND c.instructorRef.id = :instructorId) OR c.instructorEmail = :instructorEmail) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "  CAST(f.id AS string) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(f.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "  LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR f.status = :status) " +
           "AND (:rating IS NULL OR f.rating = :rating) " +
           "AND (:category IS NULL OR :category = '' OR f.category = :category) " +
           "AND (:courseId IS NULL OR c.id = :courseId)")
    Page<Feedback> instructorSearchAndFilter(
            @Param("instructorId") Long instructorId,
            @Param("instructorEmail") String instructorEmail,
            @Param("keyword") String keyword,
            @Param("status") FeedbackStatus status,
            @Param("rating") Integer rating,
            @Param("category") String category,
            @Param("courseId") Long courseId,
            Pageable pageable);

    // --- Low rating alerts ---
    @Query("SELECT f FROM Feedback f JOIN FETCH f.student s LEFT JOIN FETCH f.course c WHERE f.deleted = false AND f.rating IS NOT NULL AND f.rating <= :maxRating ORDER BY f.rating ASC, f.createdAt DESC")
    List<Feedback> findLowRatingFeedback(@Param("maxRating") int maxRating, Pageable pageable);

    // --- Pending needs attention ---
    long countByDeletedFalseAndStatusIn(List<FeedbackStatus> statuses);
}
