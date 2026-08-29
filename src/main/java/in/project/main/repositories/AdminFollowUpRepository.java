package in.project.main.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.AdminFollowUp;
import in.project.main.entities.enums.FollowUpStatus;

@Repository
public interface AdminFollowUpRepository extends JpaRepository<AdminFollowUp, Long> {

    Page<AdminFollowUp> findAllByOrderByFollowUpDateAsc(Pageable pageable);

    List<AdminFollowUp> findByStatusAndFollowUpDateLessThanEqual(FollowUpStatus status, LocalDate date);

    long countByStatus(FollowUpStatus status);

    Page<AdminFollowUp> findByStatus(FollowUpStatus status, Pageable pageable);
}
