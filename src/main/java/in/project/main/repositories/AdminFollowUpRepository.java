package in.project.main.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.AdminFollowUp;
import in.project.main.entities.enums.FollowUpStatus;

@Repository
public interface AdminFollowUpRepository extends JpaRepository<AdminFollowUp, Long> {

    Page<AdminFollowUp> findAllByOrderByFollowUpDateAsc(Pageable pageable);

    List<AdminFollowUp> findByStatusAndFollowUpDateLessThanEqual(FollowUpStatus status, LocalDate date);

    long countByStatus(FollowUpStatus status);

    Page<AdminFollowUp> findByStatus(FollowUpStatus status, Pageable pageable);

    /**
     * List queries for the admin follow-ups page, fetch-joining the lead.
     *
     * The list shows the lead's name for each row, but AdminFollowUp.lead is LAZY. Loading it
     * during template rendering depends on the session still being open, and issues one extra
     * query per row. Fetching it up front makes the page correct regardless of the
     * open-in-view setting. The association is many-to-one, so paging in SQL stays valid.
     */
    @Query(value = "SELECT f FROM AdminFollowUp f LEFT JOIN FETCH f.lead",
           countQuery = "SELECT COUNT(f) FROM AdminFollowUp f")
    Page<AdminFollowUp> findAllWithLead(Pageable pageable);

    @Query(value = "SELECT f FROM AdminFollowUp f LEFT JOIN FETCH f.lead WHERE f.status = :status",
           countQuery = "SELECT COUNT(f) FROM AdminFollowUp f WHERE f.status = :status")
    Page<AdminFollowUp> findByStatusWithLead(@Param("status") FollowUpStatus status, Pageable pageable);
}
