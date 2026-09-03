package in.project.main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String threadId;

    @Column(nullable = false)
    private String senderEmail;

    @Column
    private String senderName;

    @Column
    private String senderRole = "ADMIN"; // ADMIN, INSTRUCTOR, STUDENT, STAFF

    @Column(nullable = false)
    private String recipientEmail;

    @Column
    private String recipientName;

    @Column
    private String recipientRole = "STUDENT"; // ADMIN, INSTRUCTOR, STUDENT, STAFF

    @Column(nullable = false)
    private String subject;

    @Column(length = 4000, nullable = false)
    private String body;

    @Column
    private String sentAt;

    @Column
    private String readAt;

    @Column
    private Boolean isRead = false;

    @Column
    private Boolean isStarred = false;

    @Column
    private Boolean isArchived = false;

    @Column
    private String folder = "INBOX"; // INBOX, SENT, TRASH

    @Column
    private String priority = "NORMAL"; // NORMAL, HIGH, URGENT

    @Column
    private String attachmentUrl;

    @Column
    private Boolean isEmailDispatched = true;

    public Message() {
        this.sentAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole != null ? senderRole : "ADMIN"; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientRole() { return recipientRole != null ? recipientRole : "STUDENT"; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public String getReadAt() { return readAt; }
    public void setReadAt(String readAt) { this.readAt = readAt; }

    public Boolean getIsRead() { return isRead != null ? isRead : false; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public Boolean getIsStarred() { return isStarred != null ? isStarred : false; }
    public void setIsStarred(Boolean isStarred) { this.isStarred = isStarred; }

    public Boolean getIsArchived() { return isArchived != null ? isArchived : false; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }

    public String getFolder() { return folder != null ? folder : "INBOX"; }
    public void setFolder(String folder) { this.folder = folder; }

    public String getPriority() { return priority != null ? priority : "NORMAL"; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public Boolean getIsEmailDispatched() { return isEmailDispatched != null ? isEmailDispatched : true; }
    public void setIsEmailDispatched(Boolean isEmailDispatched) { this.isEmailDispatched = isEmailDispatched; }
}
