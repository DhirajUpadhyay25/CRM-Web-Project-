package in.project.main.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.project.main.entities.Blog;
import in.project.main.entities.enums.ContentStatus;

public interface BlogRepository extends JpaRepository<Blog, Long>, JpaSpecificationExecutor<Blog> {

    Optional<Blog> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Page<Blog> findByDeletedFalse(Pageable pageable);

    Page<Blog> findByStatusAndDeletedFalse(ContentStatus status, Pageable pageable);

    Page<Blog> findByBlogCategoryIdAndDeletedFalse(Long categoryId, Pageable pageable);

    @Query("SELECT b FROM Blog b WHERE b.deleted = false AND (b.status = in.project.main.entities.enums.ContentStatus.PUBLISHED OR (b.status = in.project.main.entities.enums.ContentStatus.SCHEDULED AND b.scheduledAt <= CURRENT_TIMESTAMP)) ORDER BY b.publishedAt DESC")
    Page<Blog> findPublished(Pageable pageable);

    @Query("SELECT b FROM Blog b WHERE b.deleted = false AND b.isFeatured = true AND (b.status = in.project.main.entities.enums.ContentStatus.PUBLISHED OR (b.status = in.project.main.entities.enums.ContentStatus.SCHEDULED AND b.scheduledAt <= CURRENT_TIMESTAMP)) ORDER BY b.publishedAt DESC")
    List<Blog> findFeaturedPublished(Pageable pageable);

    @Query("SELECT b FROM Blog b WHERE b.deleted = false AND b.blogCategory.id = :categoryId AND (b.status = in.project.main.entities.enums.ContentStatus.PUBLISHED OR (b.status = in.project.main.entities.enums.ContentStatus.SCHEDULED AND b.scheduledAt <= CURRENT_TIMESTAMP)) ORDER BY b.publishedAt DESC")
    Page<Blog> findPublishedByCategory(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT b FROM Blog b WHERE b.deleted = false AND (b.status = in.project.main.entities.enums.ContentStatus.PUBLISHED OR (b.status = in.project.main.entities.enums.ContentStatus.SCHEDULED AND b.scheduledAt <= CURRENT_TIMESTAMP)) AND (LOWER(b.title) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(b.tags) LIKE LOWER(CONCAT('%',:keyword,'%'))) ORDER BY b.publishedAt DESC")
    Page<Blog> searchPublished(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Blog b WHERE b.deleted = false AND b.relatedCourse.id = :courseId AND (b.status = in.project.main.entities.enums.ContentStatus.PUBLISHED OR (b.status = in.project.main.entities.enums.ContentStatus.SCHEDULED AND b.scheduledAt <= CURRENT_TIMESTAMP)) ORDER BY b.publishedAt DESC")
    List<Blog> findPublishedByCourseId(@Param("courseId") Long courseId, Pageable pageable);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(ContentStatus status);
}
