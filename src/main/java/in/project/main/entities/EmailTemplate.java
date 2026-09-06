package in.project.main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String templateKey;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 255)
    private String subjectTemplate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    @Column(length = 64)
    private String category = "GENERAL"; // ONBOARDING, ACADEMIC, BILLING, SYSTEM, SUPPORT

    @Column(length = 255)
    private String description;

    @Column
    private Boolean isSystemTemplate = false;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    public EmailTemplate() {}

    public EmailTemplate(String templateKey, String name, String category, String subjectTemplate, String bodyTemplate, String description, boolean isSystemTemplate) {
        this.templateKey = templateKey;
        this.name = name;
        this.category = category;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.description = description;
        this.isSystemTemplate = isSystemTemplate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }

    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }

    public String getCategory() { return category != null ? category : "GENERAL"; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsSystemTemplate() { return isSystemTemplate != null ? isSystemTemplate : false; }
    public void setIsSystemTemplate(Boolean isSystemTemplate) { this.isSystemTemplate = isSystemTemplate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCategoryBadgeClass() {
        if ("ONBOARDING".equalsIgnoreCase(category)) return "bg-emerald-500/10 text-emerald-600 border-emerald-500/20";
        if ("ACADEMIC".equalsIgnoreCase(category)) return "bg-brand-500/10 text-brand-600 border-brand-500/20";
        if ("BILLING".equalsIgnoreCase(category)) return "bg-amber-500/10 text-amber-600 border-amber-500/20";
        if ("SUPPORT".equalsIgnoreCase(category)) return "bg-sky-500/10 text-sky-600 border-sky-500/20";
        return "bg-surface-100 text-surface-600 border-surface-200";
    }
}
