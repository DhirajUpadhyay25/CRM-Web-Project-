package in.project.main.repositories;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.StudentActivity;

@Repository
public interface StudentActivityRepository extends JpaRepository<StudentActivity, Long> {
    List<StudentActivity> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    List<StudentActivity> findByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);
}
