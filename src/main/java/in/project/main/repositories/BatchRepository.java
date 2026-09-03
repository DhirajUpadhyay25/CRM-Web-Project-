package in.project.main.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Batch;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByCourseId(String courseId);
    List<Batch> findByCourseIdIn(List<String> courseIds);
}
