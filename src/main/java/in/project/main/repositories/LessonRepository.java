package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Lesson;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
}
