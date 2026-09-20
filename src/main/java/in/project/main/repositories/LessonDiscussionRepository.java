package in.project.main.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.LessonDiscussion;

@Repository
public interface LessonDiscussionRepository extends JpaRepository<LessonDiscussion, Long> {
    List<LessonDiscussion> findByLessonIdAndParentIdIsNullOrderByCreatedAtDesc(Long lessonId);
    List<LessonDiscussion> findByParentIdOrderByCreatedAtAsc(Long parentId);
    List<LessonDiscussion> findByCourseIdAndParentIdIsNullOrderByCreatedAtDesc(Long courseId);
    long countByLessonIdAndParentIdIsNull(Long lessonId);
}
