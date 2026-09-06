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

    @Column(columnDefinition = "TEXT", nullable = false)
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

    @Column(length = 32)
    private String deliveryStatus = "DELIVERED"; // DELIVERED, SENT, FAILED, PENDING

    @Column(length = 32)
    private String messageType = "DIRECT"; // DIRECT, BROADCAST, SYSTEM_ALERT, COURSE_NOTICE, SUPPORT_REPLY

    @Column(length = 32)
    private String targetAudience = "INDIVIDUAL"; // INDIVIDUAL, ALL_STUDENTS, COURSE_STUDENTS, ALL_INSTRUCTORS, ALL_STAFF

    @Column
    private Long courseId;

    @Column
    private String courseTitle;

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

    public String getDeliveryStatus() { return deliveryStatus != null ? deliveryStatus : "DELIVERED"; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public String getMessageType() { return messageType != null ? messageType : "DIRECT"; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getTargetAudience() { return targetAudience != null ? targetAudience : "INDIVIDUAL"; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    // ==========================================
    // UI & Helper Methods
    // ==========================================

    public String getSenderInitials() {
        if (senderName != null && !senderName.isBlank()) {
            String[] parts = senderName.trim().split("\\s+");
            if (parts.length >= 2) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            }
            return senderName.substring(0, Math.min(2, senderName.length())).toUpperCase();
        }
        return "AD";
    }

    public String getRecipientInitials() {
        if (recipientName != null && !recipientName.isBlank()) {
            String[] parts = recipientName.trim().split("\\s+");
            if (parts.length >= 2) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            }
            return recipientName.substring(0, Math.min(2, recipientName.length())).toUpperCase();
        }
        return "ST";
    }

    public boolean isFromAdmin() {
        return "ADMIN".equalsIgnoreCase(getSenderRole()) || "SUPER_ADMIN".equalsIgnoreCase(getSenderRole());
    }

    public String getPriorityBadgeClass() {
        if ("URGENT".equalsIgnoreCase(priority)) {
            return "bg-rose-500/10 text-rose-600 border-rose-500/20";
        }
        if ("HIGH".equalsIgnoreCase(priority)) {
            return "bg-amber-500/10 text-amber-600 border-amber-500/20";
        }
        return "bg-surface-100 text-surface-600 border-surface-200";
    }

    public String getShortSnippet() {
        if (body == null || body.isBlank()) return "";
        String plain = body.replaceAll("<[^>]*>", "").trim();
        return plain.length() > 90 ? plain.substring(0, 87) + "..." : plain;
    }
}
