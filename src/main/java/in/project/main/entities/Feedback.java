package in.project.main.entities;

import java.time.LocalDateTime;

import in.project.main.entities.enums.FeedbackPriority;
import in.project.main.entities.enums.FeedbackStatus;
import in.project.main.entities.enums.FeedbackType;
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
import in.project.main.entities.Enrollment;

@Entity
@Table(name = "feedback", indexes = {
    @Index(name = "idx_feedback_student", columnList = "student_id"),
    @Index(name = "idx_feedback_course", columnList = "course_id"),
    @Index(name = "idx_feedback_instructor", columnList = "instructor_id"),
    @Index(name = "idx_feedback_status", columnList = "status"),
    @Index(name = "idx_feedback_type", columnList = "feedbackType"),
    @Index(name = "idx_feedback_priority", columnList = "priority"),
    @Index(name = "idx_feedback_created", columnList = "createdAt"),
    @Index(name = "idx_feedback_deleted", columnList = "deleted")
})
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Student / Legacy fields ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @Column
    private String userName;

    @Column
    private String userEmail;

    // --- Feedback Type & Target ---
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private FeedbackType feedbackType;

    @Column(length = 64)
    private String targetType;

    @Column
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private Instructor instructor;

    @Column
    private Long batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    // --- Ratings ---
    @Column
    private Integer rating;

    @Column
    private Integer courseQualityRating;

    @Column
    private Integer instructorQualityRating;

    @Column
    private Integer contentQualityRating;

    @Column
    private Integer learningExperienceRating;

    @Column
    private Integer platformUsabilityRating;

    @Column
    private Integer supportExperienceRating;

    // --- Content ---
    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String message;

    // Legacy field kept for backward compatibility
    @Column(length = 3000)
    private String userFeedback;

    @Column(length = 64)
    private String category;

    // --- Workflow ---
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private FeedbackPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private FeedbackStatus status;

    // Legacy readStatus field kept for backward compatibility
    @Column
    private String readStatus;

    @Column
    private Boolean isAnonymous = Boolean.FALSE;

    @Column
    private Boolean isPublic = Boolean.FALSE;

    @Column
    private Boolean allowTestimonial = Boolean.FALSE;

    @Column
    private Boolean contactMe = Boolean.FALSE;

    // --- Admin Response ---
    @Column(columnDefinition = "TEXT")
    private String adminResponse;

    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    @Column
    private Long assignedAdminId;

    @Column(length = 255)
    private String assignedAdminEmail;

    // --- Attachments / Context ---
    @Column(length = 500)
    private String attachmentPath;

    @Column(length = 500)
    private String pageUrl;

    @Column(length = 255)
    private String deviceInfo;

    @Column(length = 255)
    private String browserInfo;

    // --- Timestamps ---
    @Column
    private String dateOfFeedback;

    @Column
    private String timeOfFeedback;

    @Column
    private LocalDateTime resolvedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private Boolean deleted = Boolean.FALSE;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = FeedbackStatus.NEW;
        }
        if (this.priority == null) {
            this.priority = FeedbackPriority.MEDIUM;
        }
        if (this.isAnonymous == null) this.isAnonymous = Boolean.FALSE;
        if (this.isPublic == null) this.isPublic = Boolean.FALSE;
        if (this.allowTestimonial == null) this.allowTestimonial = Boolean.FALSE;
        if (this.contactMe == null) this.contactMe = Boolean.FALSE;
        if (this.deleted == null) this.deleted = Boolean.FALSE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.isAnonymous == null) this.isAnonymous = Boolean.FALSE;
        if (this.isPublic == null) this.isPublic = Boolean.FALSE;
        if (this.allowTestimonial == null) this.allowTestimonial = Boolean.FALSE;
        if (this.contactMe == null) this.contactMe = Boolean.FALSE;
        if (this.deleted == null) this.deleted = Boolean.FALSE;
    }

    // --- Convenience Methods ---
    public String getStudentName() {
        if (student != null && student.getName() != null) return student.getName();
        return userName;
    }

    public String getStudentEmail() {
        if (student != null && student.getEmail() != null) return student.getEmail();
        return userEmail;
    }

    public String getDisplayName() {
        if (Boolean.TRUE.equals(isAnonymous)) return "Anonymous";
        return getStudentName();
    }

    public String getCourseName() {
        return course != null ? course.getName() : null;
    }

    public String getInstructorName() {
        return instructor != null ? instructor.getName() : null;
    }

    public String getStatusBadgeClass() {
        return status != null ? status.getBadgeClass() : "bg-gray-100 text-gray-600";
    }

    public String getPriorityBadgeClass() {
        return priority != null ? priority.getBadgeClass() : "bg-gray-100 text-gray-600";
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public FeedbackType getFeedbackType() { return feedbackType; }
    public void setFeedbackType(FeedbackType feedbackType) { this.feedbackType = feedbackType; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Instructor getInstructor() { return instructor; }
    public void setInstructor(Instructor instructor) { this.instructor = instructor; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Enrollment getEnrollment() { return enrollment; }
    public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }

    public Long getEnrollmentId() { return enrollment != null ? enrollment.getId() : null; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Integer getCourseQualityRating() { return courseQualityRating; }
    public void setCourseQualityRating(Integer courseQualityRating) { this.courseQualityRating = courseQualityRating; }

    public Integer getInstructorQualityRating() { return instructorQualityRating; }
    public void setInstructorQualityRating(Integer instructorQualityRating) { this.instructorQualityRating = instructorQualityRating; }

    public Integer getContentQualityRating() { return contentQualityRating; }
    public void setContentQualityRating(Integer contentQualityRating) { this.contentQualityRating = contentQualityRating; }

    public Integer getLearningExperienceRating() { return learningExperienceRating; }
    public void setLearningExperienceRating(Integer learningExperienceRating) { this.learningExperienceRating = learningExperienceRating; }

    public Integer getPlatformUsabilityRating() { return platformUsabilityRating; }
    public void setPlatformUsabilityRating(Integer platformUsabilityRating) { this.platformUsabilityRating = platformUsabilityRating; }

    public Integer getSupportExperienceRating() { return supportExperienceRating; }
    public void setSupportExperienceRating(Integer supportExperienceRating) { this.supportExperienceRating = supportExperienceRating; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUserFeedback() { return userFeedback; }
    public void setUserFeedback(String userFeedback) { this.userFeedback = userFeedback; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public FeedbackPriority getPriority() { return priority; }
    public void setPriority(FeedbackPriority priority) { this.priority = priority; }

    public FeedbackStatus getStatus() { return status; }
    public void setStatus(FeedbackStatus status) { this.status = status; }

    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }

    public boolean isAnonymous() { return Boolean.TRUE.equals(isAnonymous); }
    public void setAnonymous(Boolean anonymous) { this.isAnonymous = (anonymous != null && anonymous); }

    public boolean isPublic() { return Boolean.TRUE.equals(isPublic); }
    public void setPublic(Boolean isPublic) { this.isPublic = (isPublic != null && isPublic); }

    public boolean isAllowTestimonial() { return Boolean.TRUE.equals(allowTestimonial); }
    public void setAllowTestimonial(Boolean allowTestimonial) { this.allowTestimonial = (allowTestimonial != null && allowTestimonial); }

    public boolean isContactMe() { return Boolean.TRUE.equals(contactMe); }
    public void setContactMe(Boolean contactMe) { this.contactMe = (contactMe != null && contactMe); }

    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }

    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }

    public Long getAssignedAdminId() { return assignedAdminId; }
    public void setAssignedAdminId(Long assignedAdminId) { this.assignedAdminId = assignedAdminId; }

    public String getAssignedAdminEmail() { return assignedAdminEmail; }
    public void setAssignedAdminEmail(String assignedAdminEmail) { this.assignedAdminEmail = assignedAdminEmail; }

    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }

    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }

    public String getDateOfFeedback() { return dateOfFeedback; }
    public void setDateOfFeedback(String dateOfFeedback) { this.dateOfFeedback = dateOfFeedback; }

    public String getTimeOfFeedback() { return timeOfFeedback; }
    public void setTimeOfFeedback(String timeOfFeedback) { this.timeOfFeedback = timeOfFeedback; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return Boolean.TRUE.equals(deleted); }
    public void setDeleted(Boolean deleted) { this.deleted = (deleted != null && deleted); }
}
