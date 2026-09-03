package in.project.main.services;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import in.project.main.entities.Message;

public interface MessageService {

    Page<Message> getMessagesPage(String userEmail, boolean isAdmin, String folder, String search, Pageable pageable);

    Map<String, Object> getMailboxStats(String userEmail, boolean isAdmin);

    Message getMessageById(Long id);

    Message sendMessage(
            String senderEmail,
            String senderName,
            String senderRole,
            String recipientEmail,
            String subject,
            String body,
            String priority,
            String attachmentUrl);

    Message replyToMessage(
            Long parentMessageId,
            String senderEmail,
            String senderName,
            String senderRole,
            String body);

    List<Message> getConversationThread(String threadId);

    void markAsRead(Long id, String userEmail);

    void toggleStar(Long id, String userEmail);

    void moveToTrash(Long id, String userEmail);

    void restoreFromTrash(Long id, String userEmail);

    void deleteMessage(Long id, String userEmail);
}
