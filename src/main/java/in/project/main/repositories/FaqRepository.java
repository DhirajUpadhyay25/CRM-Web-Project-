package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Faq;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
}
