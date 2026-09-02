package in.project.main.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.AppPermission;

@Repository
public interface AppPermissionRepository extends JpaRepository<AppPermission, Long> {

    Optional<AppPermission> findByCode(String code);

    List<AppPermission> findByModuleOrderByCodeAsc(String module);

    List<AppPermission> findAllByOrderByModuleAscCodeAsc();

    boolean existsByCode(String code);
}
