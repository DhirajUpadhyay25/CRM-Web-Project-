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
@Table(name = "feedback_response", indexes = {
    @Index(name = "idx_fb_resp_feedback", columnList = "feedback_id"),
    @Index(name = "idx_fb_resp_created", columnList = "createdAt"),
    @Index(name = "idx_fb_resp_responder", columnList = "responderEmail")
})
public class FeedbackResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @Column(nullable = false, length = 255)
    private String responderEmail;

    @Column(length = 255)
    private String responderName;

    @Column(nullable = false, length = 32)
    private String responderRole;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Feedback getFeedback() { return feedback; }
    public void setFeedback(Feedback feedback) { this.feedback = feedback; }

    public String getResponderEmail() { return responderEmail; }
    public void setResponderEmail(String responderEmail) { this.responderEmail = responderEmail; }

    public String getResponderName() { return responderName; }
    public void setResponderName(String responderName) { this.responderName = responderName; }

    public String getResponderRole() { return responderRole; }
    public void setResponderRole(String responderRole) { this.responderRole = responderRole; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
