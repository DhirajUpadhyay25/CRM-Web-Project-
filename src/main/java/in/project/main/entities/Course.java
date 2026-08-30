package in.project.main.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column
    private String instructor;

    @Column
    private String instructorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseLevel level = CourseLevel.ALL_LEVELS;

    @Column
    private String language;

    @Column
    private String duration;

    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(nullable = false)
    private boolean featured = false;

    @Column
    private String imageUrl;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == CourseStatus.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getInstructorEmail() {
        return instructorEmail;
    }

    public void setInstructorEmail(String instructorEmail) {
        this.instructorEmail = instructorEmail;
    }

    public CourseLevel getLevel() {
        return level;
    }

    public void setLevel(CourseLevel level) {
        this.level = level;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public void setStatus(CourseStatus status) {
        this.status = status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Column
    private String seoTitle;

    @Column(length = 500)
    private String seoDescription;

    public BigDecimal getEffectivePrice() {
        if (discountedPrice != null && discountedPrice.compareTo(BigDecimal.ZERO) >= 0) {
            return discountedPrice;
        }
        return originalPrice != null ? originalPrice : BigDecimal.ZERO;
    }

    public boolean isFree() {
        BigDecimal effective = getEffectivePrice();
        return effective.compareTo(BigDecimal.ZERO) == 0;
    }

    public Integer getDiscountPercentage() {
        if (originalPrice != null && discountedPrice != null &&
            originalPrice.compareTo(BigDecimal.ZERO) > 0 &&
            discountedPrice.compareTo(originalPrice) < 0) {
            BigDecimal diff = originalPrice.subtract(discountedPrice);
            return diff.multiply(BigDecimal.valueOf(100))
                       .divide(originalPrice, java.math.RoundingMode.HALF_UP)
                       .intValue();
        }
        return null;
    }

    public boolean isPublishReady() {
        return name != null && !name.trim().isEmpty() &&
               category != null &&
               originalPrice != null &&
               shortDescription != null && !shortDescription.trim().isEmpty();
    }

    public String getSeoTitle() {
        return (seoTitle != null && !seoTitle.trim().isEmpty()) ? seoTitle : name;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getSeoDescription() {
        return (seoDescription != null && !seoDescription.trim().isEmpty()) ? seoDescription : shortDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
