package in.project.main.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.project.main.entities.Testimonial;
import in.project.main.entities.enums.TestimonialStatus;

public interface TestimonialRepository extends JpaRepository<Testimonial, Long>, JpaSpecificationExecutor<Testimonial> {

    Page<Testimonial> findByDeletedFalse(Pageable pageable);

    Page<Testimonial> findByStatusAndDeletedFalse(TestimonialStatus status, Pageable pageable);

    List<Testimonial> findByStudentIdAndDeletedFalse(Long studentId);

    List<Testimonial> findByCourseIdAndDeletedFalse(Long courseId);

    @Query("SELECT t FROM Testimonial t WHERE t.deleted = false AND t.status = in.project.main.entities.enums.TestimonialStatus.PUBLISHED ORDER BY t.displayOrder ASC, t.publishedAt DESC")
    List<Testimonial> findAllPublished();

    @Query("SELECT t FROM Testimonial t WHERE t.deleted = false AND t.status = in.project.main.entities.enums.TestimonialStatus.PUBLISHED AND t.isFeatured = true ORDER BY t.displayOrder ASC, t.publishedAt DESC")
    List<Testimonial> findFeaturedPublished();

    @Query("SELECT t FROM Testimonial t WHERE t.deleted = false AND t.status = in.project.main.entities.enums.TestimonialStatus.PUBLISHED AND t.course.id = :courseId ORDER BY t.publishedAt DESC")
    List<Testimonial> findPublishedByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT t FROM Testimonial t WHERE t.deleted = false AND t.status = in.project.main.entities.enums.TestimonialStatus.PUBLISHED ORDER BY t.publishedAt DESC")
    Page<Testimonial> findPublishedPaginated(Pageable pageable);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(TestimonialStatus status);

    boolean existsByFeedbackIdAndDeletedFalse(Long feedbackId);
}
