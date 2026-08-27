package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import in.project.main.entities.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    Optional<Category> findByName(String name);
    List<Category> findByActiveTrue();
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
}
