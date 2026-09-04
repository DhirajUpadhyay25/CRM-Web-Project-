package in.project.main.entities;

import java.time.LocalDateTime;

import in.project.main.entities.enums.TestimonialSource;
import in.project.main.entities.enums.TestimonialStatus;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "testimonial", indexes = {
    @Index(name = "idx_testimonial_student", columnList = "student_id"),
    @Index(name = "idx_testimonial_course", columnList = "course_id"),
    @Index(name = "idx_testimonial_status", columnList = "status"),
    @Index(name = "idx_testimonial_featured", columnList = "isFeatured"),
    @Index(name = "idx_testimonial_deleted", columnList = "deleted")
})
public class Testimonial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    // Legacy fields kept for backward compatibility
    @Column
    private String studentName;

    @Column
    private String courseName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column
    private Long feedbackId;

    @Column
    private Integer rating;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    // Legacy field kept for backward compatibility
    @Column(columnDefinition = "TEXT")
    private String review;

    @Column(length = 255)
    private String designation;

    @Column(length = 255)
    private String organization;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TestimonialStatus status = TestimonialStatus.PENDING;

    // Legacy field kept for backward compatibility
    @Column
    private Boolean isApproved;

    @Column
    private Boolean isFeatured = Boolean.FALSE;

    @Column
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TestimonialSource source = TestimonialSource.STUDENT_SUBMISSION;

    @Column
    private Boolean consentName = Boolean.FALSE;

    @Column
    private Boolean consentPhoto = Boolean.FALSE;

    @Column
    private Boolean consentPublish = Boolean.FALSE;

    @Column(length = 500)
    private String studentPhoto;

    @Column
    private LocalDateTime publishedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private Boolean deleted = Boolean.FALSE;

    @Column(length = 500)
    private String moderationReason;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = TestimonialStatus.PENDING;
        if (this.source == null) this.source = TestimonialSource.STUDENT_SUBMISSION;
        if (this.isFeatured == null) this.isFeatured = Boolean.FALSE;
        if (this.consentName == null) this.consentName = Boolean.FALSE;
        if (this.consentPhoto == null) this.consentPhoto = Boolean.FALSE;
        if (this.consentPublish == null) this.consentPublish = Boolean.FALSE;
        if (this.deleted == null) this.deleted = Boolean.FALSE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.isFeatured == null) this.isFeatured = Boolean.FALSE;
        if (this.consentName == null) this.consentName = Boolean.FALSE;
        if (this.consentPhoto == null) this.consentPhoto = Boolean.FALSE;
        if (this.consentPublish == null) this.consentPublish = Boolean.FALSE;
        if (this.deleted == null) this.deleted = Boolean.FALSE;
    }

    // --- Convenience Methods ---
    public String getDisplayStudentName() {
        if (Boolean.TRUE.equals(consentName) && student != null && student.getName() != null) return student.getName();
        if (studentName != null && !studentName.isBlank()) return studentName;
        return "Anonymous Student";
    }

    public String getDisplayCourseName() {
        if (course != null) return course.getName();
        return courseName;
    }

    public String getDisplayContent() {
        if (content != null && !content.isBlank()) return content;
        return review;
    }

    public String getStatusBadgeClass() {
        return status != null ? status.getBadgeClass() : "bg-gray-100 text-gray-600";
    }

    public boolean isPublished() {
        return status == TestimonialStatus.PUBLISHED;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public TestimonialStatus getStatus() { return status; }
    public void setStatus(TestimonialStatus status) { this.status = status; }

    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }

    public boolean isFeatured() { return Boolean.TRUE.equals(isFeatured); }
    public void setFeatured(Boolean isFeatured) { this.isFeatured = (isFeatured != null && isFeatured); }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public TestimonialSource getSource() { return source; }
    public void setSource(TestimonialSource source) { this.source = source; }

    public boolean isConsentName() { return Boolean.TRUE.equals(consentName); }
    public void setConsentName(Boolean consentName) { this.consentName = (consentName != null && consentName); }

    public boolean isConsentPhoto() { return Boolean.TRUE.equals(consentPhoto); }
    public void setConsentPhoto(Boolean consentPhoto) { this.consentPhoto = (consentPhoto != null && consentPhoto); }

    public boolean isConsentPublish() { return Boolean.TRUE.equals(consentPublish); }
    public void setConsentPublish(Boolean consentPublish) { this.consentPublish = (consentPublish != null && consentPublish); }

    public String getStudentPhoto() { return studentPhoto; }
    public void setStudentPhoto(String studentPhoto) { this.studentPhoto = studentPhoto; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return Boolean.TRUE.equals(deleted); }
    public void setDeleted(Boolean deleted) { this.deleted = (deleted != null && deleted); }

    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
}
