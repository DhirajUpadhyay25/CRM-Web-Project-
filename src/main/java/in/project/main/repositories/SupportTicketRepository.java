package in.project.main.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.project.main.entities.SupportTicket;
import in.project.main.entities.enums.TicketCategory;
import in.project.main.entities.enums.TicketPriority;
import in.project.main.entities.enums.TicketStatus;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    List<SupportTicket> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Page<SupportTicket> findByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);

    long countByCategory(TicketCategory category);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status IN ('OPEN', 'IN_PROGRESS', 'WAITING_FOR_USER')")
    long countActiveTickets();

    @Query("SELECT t FROM SupportTicket t WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(t.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(t.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(t.userEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:category IS NULL OR t.category = :category) AND " +
           "(:priority IS NULL OR t.priority = :priority)")
    Page<SupportTicket> searchTickets(
            @Param("keyword") String keyword,
            @Param("status") TicketStatus status,
            @Param("category") TicketCategory category,
            @Param("priority") TicketPriority priority,
            Pageable pageable
    );
}
