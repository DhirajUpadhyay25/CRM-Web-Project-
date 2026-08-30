package in.project.main.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.LessonProgress;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    List<LessonProgress> findByUserEmail(String userEmail);
    List<LessonProgress> findByUserEmailAndCourseId(String userEmail, Long courseId);
    Optional<LessonProgress> findByUserEmailAndLessonId(String userEmail, Long lessonId);
    long countByUserEmailAndCourseIdAndCompleted(String userEmail, Long courseId, boolean completed);
    
    // Find the latest active lesson progress record for Continue Learning engine
    LessonProgress findFirstByUserEmailAndCourseIdOrderByLastAccessedAtDesc(String userEmail, Long courseId);
}
