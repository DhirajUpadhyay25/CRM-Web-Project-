package in.project.main.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import in.project.main.entities.FaqCategory;

public interface FaqCategoryRepository extends JpaRepository<FaqCategory, Long> {

    Optional<FaqCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByName(String name);

    List<FaqCategory> findByIsActiveTrueOrderBySortOrderAsc();

    List<FaqCategory> findAllByOrderBySortOrderAsc();

    List<FaqCategory> findByContextTagAndIsActiveTrue(String contextTag);
}
