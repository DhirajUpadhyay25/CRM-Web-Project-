package in.project.main.repositories;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByThreadIdOrderByIdAsc(String threadId);

    @Query("SELECT COUNT(m) FROM Message m WHERE (LOWER(m.recipientEmail) = LOWER(:email) OR (:isAdmin = true AND m.recipientRole = 'ADMIN')) AND m.folder = 'INBOX' AND m.isRead = false")
    long countUnreadInbox(@Param("email") String email, @Param("isAdmin") boolean isAdmin);

    @Query("SELECT COUNT(m) FROM Message m WHERE (LOWER(m.recipientEmail) = LOWER(:email) OR (:isAdmin = true AND m.recipientRole = 'ADMIN')) AND m.folder = 'INBOX'")
    long countTotalInbox(@Param("email") String email, @Param("isAdmin") boolean isAdmin);

    @Query("SELECT COUNT(m) FROM Message m WHERE (LOWER(m.senderEmail) = LOWER(:email) OR (:isAdmin = true AND m.senderRole = 'ADMIN')) AND m.folder = 'SENT'")
    long countTotalSent(@Param("email") String email, @Param("isAdmin") boolean isAdmin);

    @Query("SELECT COUNT(m) FROM Message m WHERE (LOWER(m.recipientEmail) = LOWER(:email) OR LOWER(m.senderEmail) = LOWER(:email) OR (:isAdmin = true AND (m.recipientRole = 'ADMIN' OR m.senderRole = 'ADMIN'))) AND m.isStarred = true AND m.folder <> 'TRASH'")
    long countTotalStarred(@Param("email") String email, @Param("isAdmin") boolean isAdmin);

    @Query("SELECT COUNT(m) FROM Message m WHERE (LOWER(m.recipientEmail) = LOWER(:email) OR LOWER(m.senderEmail) = LOWER(:email) OR (:isAdmin = true AND (m.recipientRole = 'ADMIN' OR m.senderRole = 'ADMIN'))) AND m.folder = 'TRASH'")
    long countTotalTrash(@Param("email") String email, @Param("isAdmin") boolean isAdmin);

    @Query("SELECT m FROM Message m WHERE " +
           "((:folder = 'INBOX' AND (LOWER(m.recipientEmail) = LOWER(:email) OR (:isAdmin = true AND m.recipientRole = 'ADMIN')) AND m.folder = 'INBOX') OR " +
           " (:folder = 'SENT' AND (LOWER(m.senderEmail) = LOWER(:email) OR (:isAdmin = true AND m.senderRole = 'ADMIN')) AND m.folder = 'SENT') OR " +
           " (:folder = 'STARRED' AND (LOWER(m.recipientEmail) = LOWER(:email) OR LOWER(m.senderEmail) = LOWER(:email) OR (:isAdmin = true AND (m.recipientRole = 'ADMIN' OR m.senderRole = 'ADMIN'))) AND m.isStarred = true AND m.folder <> 'TRASH') OR " +
           " (:folder = 'TRASH' AND (LOWER(m.recipientEmail) = LOWER(:email) OR LOWER(m.senderEmail) = LOWER(:email) OR (:isAdmin = true AND (m.recipientRole = 'ADMIN' OR m.senderRole = 'ADMIN'))) AND m.folder = 'TRASH')) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(m.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(m.body) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(m.senderName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(m.senderEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(m.recipientName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(m.recipientEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Message> findMessagesByFolderAndSearch(
            @Param("email") String email,
            @Param("isAdmin") boolean isAdmin,
            @Param("folder") String folder,
            @Param("search") String search,
            Pageable pageable);
}
