package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Refund;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
}
