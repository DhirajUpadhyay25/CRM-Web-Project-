package in.project.main.services.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Employee;
import in.project.main.entities.Message;
import in.project.main.entities.User;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.NotificationCategory;
import in.project.main.entities.enums.NotificationPriority;
import in.project.main.entities.enums.NotificationType;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.events.PlatformNotificationEvent;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.MessageRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.AuditLogService;
import in.project.main.services.MessageService;
import in.project.main.services.NotificationService;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getMessagesPage(String userEmail, boolean isAdmin, String folder, String search, Pageable pageable) {
        String f = (folder != null && !folder.trim().isEmpty()) ? folder.trim().toUpperCase() : "INBOX";
        String q = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return messageRepository.findMessagesByFolderAndSearch(userEmail, isAdmin, f, q, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMailboxStats(String userEmail, boolean isAdmin) {
        Map<String, Object> stats = new HashMap<>();
        long unreadInbox = messageRepository.countUnreadInbox(userEmail, isAdmin);
        long totalInbox = messageRepository.countTotalInbox(userEmail, isAdmin);
        long totalSent = messageRepository.countTotalSent(userEmail, isAdmin);
        long totalStarred = messageRepository.countTotalStarred(userEmail, isAdmin);
        long totalTrash = messageRepository.countTotalTrash(userEmail, isAdmin);

        stats.put("unreadInbox", unreadInbox);
        stats.put("totalInbox", totalInbox);
        stats.put("totalSent", totalSent);
        stats.put("totalStarred", totalStarred);
        stats.put("totalTrash", totalTrash);
        return stats;
    }

    @Override
    @Transactional
    public Message getMessageById(Long id) {
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg != null && Boolean.FALSE.equals(msg.getIsRead())) {
            msg.setIsRead(true);
            msg.setReadAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            messageRepository.save(msg);
        }
        return msg;
    }

    @Override
    @Transactional
    public Message sendMessage(
            String senderEmail,
            String senderName,
            String senderRole,
            String recipientEmail,
            String subject,
            String body,
            String priority,
            String attachmentUrl) {

        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required.");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Message subject is required.");
        }
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Message body is required.");
        }

        String normalizedRecipient = recipientEmail.trim().toLowerCase();
        String normalizedSender = (senderEmail != null && !senderEmail.trim().isEmpty()) ? senderEmail.trim().toLowerCase() : "admin@edutake.com";
        String threadId = "TH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Resolve Recipient Details
        String recipientName = normalizedRecipient;
        String recipientRole = "STUDENT";

        User studentUser = userRepository.findByEmail(normalizedRecipient);
        if (studentUser != null) {
            recipientName = studentUser.getName() != null ? studentUser.getName() : normalizedRecipient;
            recipientRole = "STUDENT";
        } else if (employeeRepository != null) {
            Employee emp = employeeRepository.findByEmail(normalizedRecipient);
            if (emp != null) {
                recipientName = emp.getName() != null ? emp.getName() : normalizedRecipient;
                recipientRole = emp.getRole() != null ? emp.getRole().name() : "STAFF";
            }
        }

        // 1. Create Recipient Inbox Message
        Message inboxMsg = new Message();
        inboxMsg.setThreadId(threadId);
        inboxMsg.setSenderEmail(normalizedSender);
        inboxMsg.setSenderName(senderName != null ? senderName : "EduTake Administration");
        inboxMsg.setSenderRole(senderRole != null ? senderRole : "ADMIN");
        inboxMsg.setRecipientEmail(normalizedRecipient);
        inboxMsg.setRecipientName(recipientName);
        inboxMsg.setRecipientRole(recipientRole);
        inboxMsg.setSubject(subject.trim());
        inboxMsg.setBody(body.trim());
        inboxMsg.setFolder("INBOX");
        inboxMsg.setPriority(priority != null ? priority : "NORMAL");
        inboxMsg.setAttachmentUrl(attachmentUrl);
        inboxMsg.setSentAt(timestamp);
        inboxMsg.setIsRead(false);
        inboxMsg.setIsStarred(false);
        inboxMsg.setIsArchived(false);
        inboxMsg.setIsEmailDispatched(true);
        messageRepository.save(inboxMsg);

        // 2. Create Sender Sent Message copy
        Message sentMsg = new Message();
        sentMsg.setThreadId(threadId);
        sentMsg.setSenderEmail(normalizedSender);
        sentMsg.setSenderName(senderName != null ? senderName : "EduTake Administration");
        sentMsg.setSenderRole(senderRole != null ? senderRole : "ADMIN");
        sentMsg.setRecipientEmail(normalizedRecipient);
        sentMsg.setRecipientName(recipientName);
        sentMsg.setRecipientRole(recipientRole);
        sentMsg.setSubject(subject.trim());
        sentMsg.setBody(body.trim());
        sentMsg.setFolder("SENT");
        sentMsg.setPriority(priority != null ? priority : "NORMAL");
        sentMsg.setAttachmentUrl(attachmentUrl);
        sentMsg.setSentAt(timestamp);
        sentMsg.setIsRead(true);
        sentMsg.setIsStarred(false);
        sentMsg.setIsArchived(false);
        sentMsg.setIsEmailDispatched(true);
        Message savedSent = messageRepository.save(sentMsg);

        // 3. Dispatch in-app notification to recipient
        try {
            notificationService.sendToStudent(
                    normalizedRecipient,
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "✉️ New Message: " + subject.trim(),
                    body.length() > 150 ? body.substring(0, 147) + "..." : body,
                    "/student/messages"
            );
        } catch (Exception ex) {
            log.warn("Could not dispatch message notification: {}", ex.getMessage());
        }

        // 4. Audit Log
        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    normalizedSender,
                    AuditEventType.TEST_EMAIL_SENT,
                    "MESSAGE_SENT",
                    "Sent message '" + subject + "' to " + normalizedRecipient + " (" + recipientRole + ")."
            )
            .withEntity("MESSAGE", String.valueOf(savedSent.getId()), subject)
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);
            auditLogService.record(audit);
        }

        return savedSent;
    }

    @Override
    @Transactional
    public Message replyToMessage(
            Long parentMessageId,
            String senderEmail,
            String senderName,
            String senderRole,
            String body) {

        Message parent = messageRepository.findById(parentMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with ID: " + parentMessageId));

        String recipientEmail = parent.getSenderEmail().equalsIgnoreCase(senderEmail) ? parent.getRecipientEmail() : parent.getSenderEmail();
        String subject = parent.getSubject().startsWith("Re: ") ? parent.getSubject() : "Re: " + parent.getSubject();
        String threadId = parent.getThreadId() != null ? parent.getThreadId() : "TH-" + UUID.randomUUID().toString().substring(0, 8);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 1. Recipient Inbox copy
        Message inboxMsg = new Message();
        inboxMsg.setThreadId(threadId);
        inboxMsg.setSenderEmail(senderEmail);
        inboxMsg.setSenderName(senderName);
        inboxMsg.setSenderRole(senderRole);
        inboxMsg.setRecipientEmail(recipientEmail);
        inboxMsg.setRecipientName(parent.getSenderName());
        inboxMsg.setRecipientRole(parent.getSenderRole());
        inboxMsg.setSubject(subject);
        inboxMsg.setBody(body.trim());
        inboxMsg.setFolder("INBOX");
        inboxMsg.setPriority(parent.getPriority());
        inboxMsg.setSentAt(timestamp);
        inboxMsg.setIsRead(false);
        messageRepository.save(inboxMsg);

        // 2. Sender Sent copy
        Message sentMsg = new Message();
        sentMsg.setThreadId(threadId);
        sentMsg.setSenderEmail(senderEmail);
        sentMsg.setSenderName(senderName);
        sentMsg.setSenderRole(senderRole);
        sentMsg.setRecipientEmail(recipientEmail);
        sentMsg.setRecipientName(parent.getSenderName());
        sentMsg.setRecipientRole(parent.getSenderRole());
        sentMsg.setSubject(subject);
        sentMsg.setBody(body.trim());
        sentMsg.setFolder("SENT");
        sentMsg.setPriority(parent.getPriority());
        sentMsg.setSentAt(timestamp);
        sentMsg.setIsRead(true);
        return messageRepository.save(sentMsg);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversationThread(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) return List.of();
        return messageRepository.findByThreadIdOrderByIdAsc(threadId.trim());
    }

    @Override
    @Transactional
    public void markAsRead(Long id, String userEmail) {
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg != null) {
            msg.setIsRead(true);
            msg.setReadAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            messageRepository.save(msg);
        }
    }

    @Override
    @Transactional
    public void toggleStar(Long id, String userEmail) {
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg != null) {
            msg.setIsStarred(!Boolean.TRUE.equals(msg.getIsStarred()));
            messageRepository.save(msg);
        }
    }

    @Override
    @Transactional
    public void moveToTrash(Long id, String userEmail) {
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg != null) {
            msg.setFolder("TRASH");
            messageRepository.save(msg);
        }
    }

    @Override
    @Transactional
    public void restoreFromTrash(Long id, String userEmail) {
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg != null) {
            // Restore back to INBOX or SENT depending on original sender
            if (userEmail.equalsIgnoreCase(msg.getSenderEmail())) {
                msg.setFolder("SENT");
            } else {
                msg.setFolder("INBOX");
            }
            messageRepository.save(msg);
        }
    }

    @Override
    @Transactional
    public void deleteMessage(Long id, String userEmail) {
        Message msg = messageRepository.findById(id).orElse(null);
        if (msg != null) {
            messageRepository.delete(msg);
        }
    }
}
