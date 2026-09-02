package in.project.main.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.SystemRole;

@Repository
public interface SystemRoleRepository extends JpaRepository<SystemRole, Long> {

    Optional<SystemRole> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);
}
