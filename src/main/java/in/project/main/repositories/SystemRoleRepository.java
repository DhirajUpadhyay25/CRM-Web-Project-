package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.SystemRole;

@Repository
public interface SystemRoleRepository extends JpaRepository<SystemRole, Long> {
}
