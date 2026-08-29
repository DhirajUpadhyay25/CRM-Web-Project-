package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Batch;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
}
