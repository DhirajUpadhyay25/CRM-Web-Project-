package in.project.main.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_role", indexes = {
    @Index(name = "idx_role_name", columnList = "role_name", unique = true)
})
public class SystemRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true, length = 60)
    private String roleName; // e.g. "SUPER_ADMIN", "ADMIN", "INSTRUCTOR", "STUDENT", "STAFF"

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_system_role")
    private Boolean systemRole = false; // Protected from deletion

    @Column(name = "is_active")
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"),
        indexes = {
            @Index(name = "idx_role_perm_role", columnList = "role_id"),
            @Index(name = "idx_role_perm_perm", columnList = "permission_id")
        }
    )
    private Set<AppPermission> permissions = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SystemRole() {}

    public SystemRole(String roleName, String displayName, String description, boolean systemRole, boolean active) {
        this.roleName = roleName;
        this.displayName = displayName;
        this.description = description;
        this.systemRole = systemRole;
        this.active = active;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDisplayName() { return displayName != null ? displayName : roleName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSystemRole() { return systemRole != null && systemRole; }
    public void setSystemRole(Boolean systemRole) { this.systemRole = systemRole != null ? systemRole : false; }

    public boolean isActive() { return active == null || active; }
    public void setActive(Boolean active) { this.active = active != null ? active : true; }

    public Set<AppPermission> getPermissions() { return permissions; }
    public void setPermissions(Set<AppPermission> permissions) { this.permissions = permissions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public void addPermission(AppPermission permission) {
        if (this.permissions == null) {
            this.permissions = new HashSet<>();
        }
        this.permissions.add(permission);
    }

    public void removePermission(AppPermission permission) {
        if (this.permissions != null) {
            this.permissions.remove(permission);
        }
    }
}
