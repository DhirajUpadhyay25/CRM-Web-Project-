package in.project.main.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_ticket_replies", indexes = {
    @Index(name = "idx_reply_ticket_id", columnList = "ticket_id"),
    @Index(name = "idx_reply_created_at", columnList = "createdAt")
})
public class SupportTicketReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(nullable = false, length = 32)
    private String senderType = "USER"; // USER, ADMIN, SUPPORT_AGENT, SYSTEM

    @Column(nullable = false, length = 100)
    private String senderName;

    @Column(nullable = false, length = 150)
    private String senderEmail;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public SupportTicketReply() {}

    public SupportTicketReply(SupportTicket ticket, String senderType, String senderName, String senderEmail, String message) {
        this.ticket = ticket;
        this.senderType = senderType;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SupportTicket getTicket() { return ticket; }
    public void setTicket(SupportTicket ticket) { this.ticket = ticket; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
