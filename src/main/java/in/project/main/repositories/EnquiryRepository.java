package in.project.main.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Enquiry;
import in.project.main.entities.enums.EnquiryStatus;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    long countByStatus(EnquiryStatus status);

    @Query("SELECT e FROM Enquiry e WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.subject) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<Enquiry> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") EnquiryStatus status,
            Pageable pageable);
}
