package in.project.main.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Category;
import in.project.main.entities.Course;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Course findByName(String name);

    Optional<Course> findBySlug(String slug);
    
    boolean existsBySlug(String slug);

    long countByStatus(CourseStatus status);

    long countByFeaturedTrue();

    @Query("SELECT COUNT(c) FROM Course c WHERE (c.discountedPrice IS NOT NULL AND c.discountedPrice = 0) OR (c.discountedPrice IS NULL AND c.originalPrice = 0)")
    long countFreeCourses();

    @Query("SELECT COUNT(c) FROM Course c WHERE (c.discountedPrice IS NOT NULL AND c.discountedPrice > 0) OR (c.discountedPrice IS NULL AND c.originalPrice > 0)")
    long countPaidCourses();

    // For public website: find published courses
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    // For public website: find featured and published courses
    Page<Course> findByFeaturedTrueAndStatus(CourseStatus status, Pageable pageable);

    // For public website: view single published course
    Optional<Course> findBySlugAndStatus(String slug, CourseStatus status);

    // For related courses
    List<Course> findTop4ByCategoryAndStatusAndIdNot(Category category, CourseStatus status, Long id);

    // Admin search and multi-filter
    @Query("SELECT c FROM Course c WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.instructor) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " (c.category IS NOT NULL AND LOWER(c.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')))) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:categoryId IS NULL OR c.category.id = :categoryId) AND " +
           "(:featured IS NULL OR c.featured = :featured) AND " +
           "(:pricingType IS NULL OR :pricingType = '' OR " +
           " (:pricingType = 'FREE' AND ((c.discountedPrice IS NOT NULL AND c.discountedPrice = 0) OR (c.discountedPrice IS NULL AND c.originalPrice = 0))) OR " +
           " (:pricingType = 'PAID' AND ((c.discountedPrice IS NOT NULL AND c.discountedPrice > 0) OR (c.discountedPrice IS NULL AND c.originalPrice > 0))))")
    Page<Course> adminSearchAndFilterCourses(
            @Param("keyword") String keyword, 
            @Param("status") CourseStatus status, 
            @Param("categoryId") Long categoryId, 
            @Param("featured") Boolean featured,
            @Param("pricingType") String pricingType,
            Pageable pageable);

    // Public storefront search and multi-filter
    @Query("SELECT c FROM Course c WHERE " +
           "c.status = in.project.main.entities.enums.CourseStatus.PUBLISHED AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.instructor) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " (c.category IS NOT NULL AND LOWER(c.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')))) AND " +
           "(:categoryId IS NULL OR c.category.id = :categoryId) AND " +
           "(:level IS NULL OR c.level = :level) AND " +
           "(:pricingType IS NULL OR :pricingType = '' OR " +
           " (:pricingType = 'FREE' AND ((c.discountedPrice IS NOT NULL AND c.discountedPrice = 0) OR (c.discountedPrice IS NULL AND c.originalPrice = 0))) OR " +
           " (:pricingType = 'PAID' AND ((c.discountedPrice IS NOT NULL AND c.discountedPrice > 0) OR (c.discountedPrice IS NULL AND c.originalPrice > 0))))")
    Page<Course> publicSearchAndFilterCourses(
            @Param("keyword") String keyword, 
            @Param("categoryId") Long categoryId, 
            @Param("level") CourseLevel level,
            @Param("pricingType") String pricingType,
            Pageable pageable);
}
