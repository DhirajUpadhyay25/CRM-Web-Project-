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
    
    List<Category> findByActive(boolean active);
    
    List<Category> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(String name, String slug);
    
    long countByActive(boolean active);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Category c WHERE size(c.courses) > 0")
    long countCategoriesWithCourses();
}
