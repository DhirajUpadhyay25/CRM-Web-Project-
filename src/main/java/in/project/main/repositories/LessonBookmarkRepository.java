package in.project.main.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.LessonBookmark;

@Repository
public interface LessonBookmarkRepository extends JpaRepository<LessonBookmark, Long> {
    Optional<LessonBookmark> findByUserEmailAndLessonId(String userEmail, Long lessonId);
    List<LessonBookmark> findByUserEmailAndCourseIdOrderByCreatedAtDesc(String userEmail, Long courseId);
    List<LessonBookmark> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    boolean existsByUserEmailAndLessonId(String userEmail, Long lessonId);
    void deleteByUserEmailAndLessonId(String userEmail, Long lessonId);
}
