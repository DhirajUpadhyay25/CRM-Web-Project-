package in.project.main.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import in.project.main.entities.enums.TicketCategory;
import in.project.main.entities.enums.TicketPriority;
import in.project.main.entities.enums.TicketStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_tickets", indexes = {
    @Index(name = "idx_ticket_number", columnList = "ticketNumber", unique = true),
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_category", columnList = "category"),
    @Index(name = "idx_ticket_priority", columnList = "priority"),
    @Index(name = "idx_ticket_user_email", columnList = "userEmail"),
    @Index(name = "idx_ticket_created_at", columnList = "createdAt")
})
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String userName;

    @Column(nullable = false, length = 150)
    private String userEmail;

    @Column(length = 32)
    private String userRole = "GUEST";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketCategory category = TicketCategory.OTHER;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketPriority priority = TicketPriority.MEDIUM;

    // Captured safe technical context
    @Column(length = 500)
    private String pageUrl;

    @Column(length = 200)
    private String deviceInfo;

    @Column(length = 200)
    private String browserInfo;

    // Optional LMS academic context
    @Column
    private Long courseId;

    @Column(length = 255)
    private String courseName;

    @Column
    private Long lessonId;

    @Column(length = 255)
    private String lessonTitle;

    // Assignment / Staff handling
    @Column(length = 150)
    private String assignedTo;

    @Column(length = 150)
    private String assignedToEmail;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column
    private LocalDateTime resolvedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<SupportTicketReply> replies = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.ticketNumber == null || this.ticketNumber.isBlank()) {
            this.ticketNumber = "TCK-" + System.currentTimeMillis() % 1000000;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Constructors ---
    public SupportTicket() {}

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public TicketCategory getCategory() { return category; }
    public void setCategory(TicketCategory category) { this.category = category; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public String getLessonTitle() { return lessonTitle; }
    public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getAssignedToEmail() { return assignedToEmail; }
    public void setAssignedToEmail(String assignedToEmail) { this.assignedToEmail = assignedToEmail; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<SupportTicketReply> getReplies() { return replies; }
    public void setReplies(List<SupportTicketReply> replies) { this.replies = replies; }

    public void addReply(SupportTicketReply reply) {
        replies.add(reply);
        reply.setTicket(this);
    }
}
