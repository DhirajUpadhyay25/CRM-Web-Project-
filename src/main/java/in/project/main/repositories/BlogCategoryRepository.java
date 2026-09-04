package in.project.main.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import in.project.main.entities.BlogCategory;

public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long> {

    Optional<BlogCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByName(String name);

    List<BlogCategory> findByIsActiveTrueOrderBySortOrderAsc();

    List<BlogCategory> findAllByOrderBySortOrderAsc();
}
