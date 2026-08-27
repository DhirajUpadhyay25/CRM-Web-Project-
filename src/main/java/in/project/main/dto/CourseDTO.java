package in.project.main.dto;

import java.math.BigDecimal;

import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CourseDTO {

    private Long id;

    @NotBlank(message = "Course title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String name;

    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    private String shortDescription;

    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private String instructor;

    @NotNull(message = "Level is required")
    private CourseLevel level = CourseLevel.ALL_LEVELS;

    private String language;

    private String duration;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal originalPrice;

    @Min(value = 0, message = "Discounted price cannot be negative")
    private BigDecimal discountedPrice;

    @NotNull(message = "Status is required")
    private CourseStatus status = CourseStatus.DRAFT;

    private boolean featured = false;

    private String imageUrl;

    @Size(max = 255, message = "SEO Title cannot exceed 255 characters")
    private String seoTitle;

    @Size(max = 500, message = "SEO Description cannot exceed 500 characters")
    private String seoDescription;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
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

    public String getSeoTitle() {
        return seoTitle;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }
}
