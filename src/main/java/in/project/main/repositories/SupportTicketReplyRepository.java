package in.project.main.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import in.project.main.entities.SupportTicketReply;

public interface SupportTicketReplyRepository extends JpaRepository<SupportTicketReply, Long> {

    List<SupportTicketReply> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    long countByTicketId(Long ticketId);
}
