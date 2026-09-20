package in.project.main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    @Column
    private String courseId;

    @Column
    private String sectionName;

    @Column
    private Integer orderIndex;

    @Column
    private String contentType = "VIDEO"; // VIDEO, ARTICLE, PDF, QUIZ, ASSIGNMENT

    @Column(length = 1000)
    private String videoUrl; // YouTube, Vimeo, or direct MP4 URL

    @Column(columnDefinition = "LONGTEXT")
    private String textContent; // For reading guides / articles

    @Column
    private String duration; // e.g. "15 mins"

    @Column(length = 1000)
    private String resourceFileUrl; // PDF or asset download link

    @Column
    private Boolean isFreePreview = Boolean.FALSE;

    @jakarta.persistence.Transient
    private boolean locked = false;

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public String getContentType() { return contentType != null ? contentType : "VIDEO"; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getResourceFileUrl() { return resourceFileUrl; }
    public void setResourceFileUrl(String resourceFileUrl) { this.resourceFileUrl = resourceFileUrl; }

    public Boolean isFreePreview() { return Boolean.TRUE.equals(isFreePreview); }
    public void setFreePreview(Boolean freePreview) { this.isFreePreview = freePreview != null ? freePreview : false; }
    public Boolean getIsFreePreview() { return Boolean.TRUE.equals(isFreePreview); }
    public void setIsFreePreview(Boolean isFreePreview) { this.isFreePreview = isFreePreview != null ? isFreePreview : false; }
}
