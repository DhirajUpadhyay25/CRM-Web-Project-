package in.project.main.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Lead;
import in.project.main.entities.enums.LeadStatus;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    long countByStatus(LeadStatus status);

    List<Lead> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT l FROM Lead l WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(l.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(l.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(l.interestedIn) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:source IS NULL OR :source = '' OR l.source = :source)")
    Page<Lead> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") LeadStatus status,
            @Param("source") String source,
            Pageable pageable);
}
