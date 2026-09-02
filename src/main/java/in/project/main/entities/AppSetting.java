package in.project.main.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_setting", indexes = {
    @Index(name = "idx_setting_key", columnList = "setting_key", unique = true),
    @Index(name = "idx_setting_category", columnList = "setting_category")
})
public class AppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 120)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "setting_category", nullable = false, length = 50)
    private String settingCategory;

    @Column(name = "setting_type", nullable = false, length = 30)
    private String settingType = "STRING"; // STRING, BOOLEAN, NUMBER, ENCRYPTED, JSON

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_encrypted")
    private boolean encrypted = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    public AppSetting() {}

    public AppSetting(String settingKey, String settingValue, String settingCategory, String settingType, String displayName, String description, boolean encrypted) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.settingCategory = settingCategory;
        this.settingType = settingType;
        this.displayName = displayName;
        this.description = description;
        this.encrypted = encrypted;
    }

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }

    public String getSettingCategory() { return settingCategory; }
    public void setSettingCategory(String settingCategory) { this.settingCategory = settingCategory; }

    public String getSettingType() { return settingType; }
    public void setSettingType(String settingType) { this.settingType = settingType; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
