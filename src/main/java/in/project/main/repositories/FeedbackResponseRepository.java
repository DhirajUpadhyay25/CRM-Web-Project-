package in.project.main.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import in.project.main.entities.FeedbackResponse;

public interface FeedbackResponseRepository extends JpaRepository<FeedbackResponse, Long> {

    List<FeedbackResponse> findByFeedbackIdOrderByCreatedAtAsc(Long feedbackId);

    long countByFeedbackId(Long feedbackId);

    long countByResponderEmail(String responderEmail);
}
