package in.project.main.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.AssignmentSubmission;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    List<AssignmentSubmission> findByUserEmail(String userEmail);
    Optional<AssignmentSubmission> findByUserEmailAndAssignmentId(String userEmail, Long assignmentId);
    List<AssignmentSubmission> findByUserEmailAndAssignmentIdIn(String userEmail, List<Long> assignmentIds);
    List<AssignmentSubmission> findByStatus(String status);
}
