package in.project.main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000, nullable = false)
    private String content;

    @Column(nullable = false)
    private String targetAudience = "ALL"; // ALL, STUDENTS, INSTRUCTORS, COURSE_SPECIFIC

    @Column
    private String category = "GENERAL"; // GENERAL, ACADEMIC, MAINTENANCE, EVENT, DISCOUNT

    @Column
    private String priority = "NORMAL"; // NORMAL, HIGH, URGENT

    @Column
    private Boolean pinned = false;

    @Column
    private Long courseId;

    @Column
    private String courseName;

    @Column
    private String publishDate;

    @Column
    private String expiresAt;

    @Column
    private String authorEmail;

    @Column
    private String authorName;

    @Column
    private Integer viewCount = 0;

    @Column
    private Boolean isActive = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public String getCategory() { return category != null ? category : "GENERAL"; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority != null ? priority : "NORMAL"; }
    public void setPriority(String priority) { this.priority = priority; }

    public Boolean getPinned() { return pinned != null ? pinned : false; }
    public void setPinned(Boolean pinned) { this.pinned = pinned; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getPublishDate() { return publishDate; }
    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Integer getViewCount() { return viewCount != null ? viewCount : 0; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Boolean getIsActive() { return isActive != null ? isActive : true; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

}
