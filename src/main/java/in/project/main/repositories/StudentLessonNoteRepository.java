package in.project.main.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.StudentLessonNote;

@Repository
public interface StudentLessonNoteRepository extends JpaRepository<StudentLessonNote, Long> {
    List<StudentLessonNote> findByUserEmailAndLessonIdOrderByCreatedAtDesc(String userEmail, Long lessonId);
    List<StudentLessonNote> findByUserEmailAndCourseIdOrderByCreatedAtDesc(String userEmail, Long courseId);
    void deleteByIdAndUserEmail(Long id, String userEmail);
}
