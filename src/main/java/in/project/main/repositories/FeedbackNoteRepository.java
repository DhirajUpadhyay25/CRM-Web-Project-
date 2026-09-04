package in.project.main.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import in.project.main.entities.FeedbackNote;

public interface FeedbackNoteRepository extends JpaRepository<FeedbackNote, Long> {

    List<FeedbackNote> findByFeedbackIdOrderByCreatedAtDesc(Long feedbackId);

    long countByFeedbackId(Long feedbackId);
}
