package in.project.main.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_permission", indexes = {
    @Index(name = "idx_perm_code", columnList = "code", unique = true),
    @Index(name = "idx_perm_module", columnList = "module")
})
public class AppPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // e.g. "students.view", "courses.create", "settings.update"

    @Column(nullable = false, length = 50)
    private String module; // e.g. "STUDENTS", "COURSES", "COMMERCE", "SYSTEM", "SETTINGS"

    @Column(nullable = false, length = 150)
    private String name; // e.g. "View Students"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_sensitive")
    private boolean sensitive = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public AppPermission() {}

    public AppPermission(String code, String module, String name, String description, boolean sensitive) {
        this.code = code;
        this.module = module;
        this.name = name;
        this.description = description;
        this.sensitive = sensitive;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSensitive() { return sensitive; }
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppPermission)) return false;
        AppPermission that = (AppPermission) o;
        return code != null && code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}
