package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
}
