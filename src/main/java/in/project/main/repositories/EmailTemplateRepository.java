package in.project.main.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.EmailTemplate;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByTemplateKey(String templateKey);

    List<EmailTemplate> findAllByOrderByCategoryAscNameAsc();

    List<EmailTemplate> findByCategoryOrderByNameAsc(String category);

    boolean existsByTemplateKey(String templateKey);
}
