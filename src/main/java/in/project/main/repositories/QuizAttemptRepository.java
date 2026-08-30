package in.project.main.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.QuizAttempt;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserEmailAndQuizIdOrderByAttemptedAtDesc(String userEmail, Long quizId);
    List<QuizAttempt> findByUserEmailOrderByAttemptedAtDesc(String userEmail);
    long countByUserEmailAndQuizIdAndPassed(String userEmail, Long quizId, boolean passed);
    List<QuizAttempt> findByQuizId(Long quizId);
}
