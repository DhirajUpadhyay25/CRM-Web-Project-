package in.project.main.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.project.main.entities.Page;
import in.project.main.entities.enums.ContentStatus;

public interface PageRepository extends JpaRepository<Page, Long>, JpaSpecificationExecutor<Page> {

    Optional<Page> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    org.springframework.data.domain.Page<Page> findByDeletedFalse(Pageable pageable);

    org.springframework.data.domain.Page<Page> findByStatusAndDeletedFalse(ContentStatus status, Pageable pageable);

    @Query("SELECT p FROM Page p WHERE p.deleted = false AND (p.status = in.project.main.entities.enums.ContentStatus.PUBLISHED OR (p.status = in.project.main.entities.enums.ContentStatus.SCHEDULED AND p.scheduledAt <= CURRENT_TIMESTAMP))")
    List<Page> findAllPublished();

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(ContentStatus status);

    @Query("SELECT p FROM Page p WHERE p.deleted = false AND (LOWER(p.title) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(p.slug) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    org.springframework.data.domain.Page<Page> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
