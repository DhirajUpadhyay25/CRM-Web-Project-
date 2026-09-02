package in.project.main.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Notification;
import in.project.main.entities.enums.NotificationCategory;
import in.project.main.entities.enums.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);

    Page<Notification> findByRecipientEmailAndIsReadOrderByCreatedAtDesc(String recipientEmail, boolean isRead, Pageable pageable);

    Page<Notification> findByRecipientEmailAndCategoryOrderByCreatedAtDesc(String recipientEmail, NotificationCategory category, Pageable pageable);

    Page<Notification> findByRecipientEmailAndCategoryAndIsReadOrderByCreatedAtDesc(String recipientEmail, NotificationCategory category, boolean isRead, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipientEmail = :email AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(n.message) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> searchByKeyword(@Param("email") String email, @Param("keyword") String keyword, Pageable pageable);

    List<Notification> findTop10ByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    long countByRecipientEmailAndIsReadFalse(String recipientEmail);

    long countByRecipientEmail(String recipientEmail);

    long countByRecipientEmailAndCategory(String recipientEmail, NotificationCategory category);

    Optional<Notification> findByIdAndRecipientEmail(Long id, String recipientEmail);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipientEmail = :email AND n.isRead = false")
    int markAllAsRead(@Param("email") String recipientEmail);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :id AND n.recipientEmail = :email")
    int markAsRead(@Param("id") Long id, @Param("email") String recipientEmail);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.id = :id AND n.recipientEmail = :email")
    int deleteByIdAndRecipientEmail(@Param("id") Long id, @Param("email") String recipientEmail);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientEmail = :email")
    int deleteAllByRecipientEmail(@Param("email") String recipientEmail);

    boolean existsByRecipientEmailAndTypeAndEntityIdAndCreatedAtAfter(
            String recipientEmail, NotificationType type, String entityId, LocalDateTime after);
}
