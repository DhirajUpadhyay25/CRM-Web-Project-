package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Media;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
}
