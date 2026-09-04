package in.project.main.entities;

import java.time.LocalDateTime;

import in.project.main.entities.enums.ContentStatus;
import in.project.main.entities.enums.ContentVisibility;
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
@Table(name = "blog", indexes = {
    @Index(name = "idx_blog_slug", columnList = "slug", unique = true),
    @Index(name = "idx_blog_status", columnList = "status"),
    @Index(name = "idx_blog_category", columnList = "category_id"),
    @Index(name = "idx_blog_published", columnList = "publishedAt"),
    @Index(name = "idx_blog_featured", columnList = "isFeatured"),
    @Index(name = "idx_blog_deleted", columnList = "deleted")
})
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 500)
    private String excerpt;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 500)
    private String featuredImage;

    // Legacy field kept for backward compatibility
    @Column
    private String author;

    @Column
    private Long authorId;

    @Column(length = 255)
    private String authorEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private BlogCategory blogCategory;

    @Column(length = 500)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContentStatus status = ContentStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ContentVisibility visibility = ContentVisibility.PUBLIC;

    @Column
    private Boolean isFeatured = Boolean.FALSE;

    @Column(length = 255)
    private String seoTitle;

    @Column(length = 500)
    private String seoDescription;

    @Column
    private Long viewCount = 0L;

    // Legacy field kept for backward compatibility
    @Column
    private String publishDate;

    @Column
    private LocalDateTime publishedAt;

    @Column
    private LocalDateTime scheduledAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private Boolean deleted = Boolean.FALSE;

    // Optional course relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_course_id")
    private Course relatedCourse;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = ContentStatus.DRAFT;
        if (this.visibility == null) this.visibility = ContentVisibility.PUBLIC;
        if (this.viewCount == null) this.viewCount = 0L;
        if (this.isFeatured == null) this.isFeatured = Boolean.FALSE;
        if (this.deleted == null) this.deleted = Boolean.FALSE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.isFeatured == null) this.isFeatured = Boolean.FALSE;
        if (this.deleted == null) this.deleted = Boolean.FALSE;
    }

    // --- Convenience Methods ---
    public boolean isPublished() {
        if (status == ContentStatus.SCHEDULED && scheduledAt != null && !scheduledAt.isAfter(LocalDateTime.now())) {
            return true;
        }
        return status == ContentStatus.PUBLISHED;
    }

    public String getStatusBadgeClass() {
        return status != null ? status.getBadgeClass() : "bg-gray-100 text-gray-600";
    }

    public String getCategoryName() {
        return blogCategory != null ? blogCategory.getName() : "Uncategorized";
    }

    public String getExcerptOrTruncatedContent() {
        if (excerpt != null && !excerpt.isBlank()) return excerpt;
        if (content != null && content.length() > 200) return content.substring(0, 200) + "...";
        return content;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFeaturedImage() { return featuredImage; }
    public void setFeaturedImage(String featuredImage) { this.featuredImage = featuredImage; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public BlogCategory getBlogCategory() { return blogCategory; }
    public void setBlogCategory(BlogCategory blogCategory) { this.blogCategory = blogCategory; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }

    public ContentVisibility getVisibility() { return visibility; }
    public void setVisibility(ContentVisibility visibility) { this.visibility = visibility; }

    public boolean isFeatured() { return Boolean.TRUE.equals(isFeatured); }
    public void setFeatured(Boolean isFeatured) { this.isFeatured = (isFeatured != null && isFeatured); }

    public String getSeoTitle() { return seoTitle != null && !seoTitle.isBlank() ? seoTitle : title; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoDescription() { return seoDescription != null && !seoDescription.isBlank() ? seoDescription : excerpt; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public String getPublishDate() { return publishDate; }
    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return Boolean.TRUE.equals(deleted); }
    public void setDeleted(Boolean deleted) { this.deleted = (deleted != null && deleted); }

    public Course getRelatedCourse() { return relatedCourse; }
    public void setRelatedCourse(Course relatedCourse) { this.relatedCourse = relatedCourse; }
}
