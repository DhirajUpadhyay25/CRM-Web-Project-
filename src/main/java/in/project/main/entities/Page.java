package in.project.main.entities;

import java.time.LocalDateTime;

import in.project.main.entities.enums.ContentStatus;
import in.project.main.entities.enums.ContentVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "page", indexes = {
    @Index(name = "idx_page_slug", columnList = "slug", unique = true),
    @Index(name = "idx_page_status", columnList = "status"),
    @Index(name = "idx_page_visibility", columnList = "visibility"),
    @Index(name = "idx_page_deleted", columnList = "deleted")
})
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 500)
    private String featuredImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContentStatus status = ContentStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ContentVisibility visibility = ContentVisibility.PUBLIC;

    @Column(length = 255)
    private String seoTitle;

    @Column(length = 500)
    private String seoDescription;

    @Column(length = 500)
    private String seoKeywords;

    @Column(length = 500)
    private String canonicalUrl;

    @Column
    private Long authorId;

    @Column(length = 255)
    private String authorEmail;

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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = ContentStatus.DRAFT;
        if (this.visibility == null) this.visibility = ContentVisibility.PUBLIC;
        if (this.deleted == null) this.deleted = Boolean.FALSE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public String getEffectiveSeoTitle() {
        return seoTitle != null && !seoTitle.isBlank() ? seoTitle : title;
    }

    public String getEffectiveSeoDescription() {
        return seoDescription != null && !seoDescription.isBlank() ? seoDescription : summary;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFeaturedImage() { return featuredImage; }
    public void setFeaturedImage(String featuredImage) { this.featuredImage = featuredImage; }

    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }

    public ContentVisibility getVisibility() { return visibility; }
    public void setVisibility(ContentVisibility visibility) { this.visibility = visibility; }

    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }

    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }

    public String getSeoKeywords() { return seoKeywords; }
    public void setSeoKeywords(String seoKeywords) { this.seoKeywords = seoKeywords; }

    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

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
}
