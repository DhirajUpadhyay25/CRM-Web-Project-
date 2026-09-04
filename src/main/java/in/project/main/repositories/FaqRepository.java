package in.project.main.repositories;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.project.main.entities.Faq;
import in.project.main.entities.enums.ContentVisibility;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findByIsActiveTrueOrderBySortOrderAsc();

    List<Faq> findByFaqCategoryIdAndIsActiveTrueOrderBySortOrderAsc(Long categoryId);

    List<Faq> findByFaqCategoryIdOrderBySortOrderAsc(Long categoryId);

    List<Faq> findAllByOrderBySortOrderAsc();

    List<Faq> findByVisibilityAndIsActiveTrueOrderBySortOrderAsc(ContentVisibility visibility);

    @Query("SELECT f FROM Faq f WHERE f.isActive = true AND (LOWER(f.question) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(f.answer) LIKE LOWER(CONCAT('%',:keyword,'%'))) ORDER BY f.sortOrder ASC")
    List<Faq> searchActive(@Param("keyword") String keyword);

    @Query("SELECT f FROM Faq f WHERE f.isActive = true AND f.faqCategory.contextTag = :contextTag ORDER BY f.sortOrder ASC")
    List<Faq> findByContextTag(@Param("contextTag") String contextTag);

    @Query("SELECT f FROM Faq f WHERE f.isActive = true ORDER BY f.viewCount DESC")
    List<Faq> findPopular(Pageable pageable);

    long countByIsActiveTrue();
}
