package in.project.main.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import in.project.main.entities.FeedbackStatusHistory;

public interface FeedbackStatusHistoryRepository extends JpaRepository<FeedbackStatusHistory, Long> {

    List<FeedbackStatusHistory> findByFeedbackIdOrderByCreatedAtDesc(Long feedbackId);
}
