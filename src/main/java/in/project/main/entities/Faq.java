package in.project.main.entities;

import java.time.LocalDateTime;

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
@Table(name = "faq", indexes = {
    @Index(name = "idx_faq_category", columnList = "faq_category_id"),
    @Index(name = "idx_faq_active", columnList = "isActive"),
    @Index(name = "idx_faq_sort", columnList = "sortOrder")
})
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    // Legacy field kept for backward compatibility
    @Column(length = 64)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_category_id")
    private FaqCategory faqCategory;

    @Column
    private Integer sortOrder = 0;

    @Column
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ContentVisibility visibility = ContentVisibility.PUBLIC;

    @Column
    private Long viewCount = 0L;

    @Column
    private Long helpfulCount = 0L;

    @Column
    private Long notHelpfulCount = 0L;

    @Column
    private Long createdBy;

    @Column(length = 255)
    private String createdByEmail;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isActive == null) this.isActive = true;
        if (this.viewCount == null) this.viewCount = 0L;
        if (this.helpfulCount == null) this.helpfulCount = 0L;
        if (this.notHelpfulCount == null) this.notHelpfulCount = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Convenience Methods ---
    public String getCategoryName() {
        if (faqCategory != null && faqCategory.getName() != null) return faqCategory.getName();
        if (category != null && !category.isBlank()) return category;
        return "General";
    }

    public String getContextTag() {
        if (faqCategory != null && faqCategory.getContextTag() != null) {
            return faqCategory.getContextTag();
        }
        return "GENERAL";
    }

    public int getHelpfulnessRate() {
        long total = (helpfulCount != null ? helpfulCount : 0L) + (notHelpfulCount != null ? notHelpfulCount : 0L);
        if (total == 0) return 100;
        long h = helpfulCount != null ? helpfulCount : 0L;
        return (int) Math.round(((double) h / total) * 100);
    }

    public String getAnswerSnippet() {
        if (answer == null) return "";
        String clean = answer.replaceAll("<[^>]*>", "").trim();
        if (clean.length() <= 120) return clean;
        return clean.substring(0, 117) + "...";
    }

    public boolean isIsActive() {
        return isActive != null && isActive;
    }

    public int getSortOrderSafe() {
        return sortOrder != null ? sortOrder : 0;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public FaqCategory getFaqCategory() { return faqCategory; }
    public void setFaqCategory(FaqCategory faqCategory) { this.faqCategory = faqCategory; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ContentVisibility getVisibility() { return visibility; }
    public void setVisibility(ContentVisibility visibility) { this.visibility = visibility; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public Long getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(Long helpfulCount) { this.helpfulCount = helpfulCount; }

    public Long getNotHelpfulCount() { return notHelpfulCount; }
    public void setNotHelpfulCount(Long notHelpfulCount) { this.notHelpfulCount = notHelpfulCount; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public String getCreatedByEmail() { return createdByEmail; }
    public void setCreatedByEmail(String createdByEmail) { this.createdByEmail = createdByEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
