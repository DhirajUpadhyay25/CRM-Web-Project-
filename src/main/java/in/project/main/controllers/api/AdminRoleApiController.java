package in.project.main.controllers.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.entities.AppPermission;
import in.project.main.entities.SystemRole;
import in.project.main.services.RbacService;

@RestController
@RequestMapping("/admin/api/roles")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminRoleApiController {

    @Autowired
    private RbacService rbacService;

    @GetMapping
    public ResponseEntity<List<SystemRole>> getAllRoles() {
        return ResponseEntity.ok(rbacService.getAllRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(@PathVariable Long id) {
        return rbacService.getRoleById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRole(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            String roleName = (String) payload.get("roleName");
            String displayName = (String) payload.get("displayName");
            String description = (String) payload.get("description");
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) payload.get("permissions");

            SystemRole created = rbacService.createRole(roleName, displayName, description, permissions, userEmail);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            String displayName = (String) payload.get("displayName");
            String description = (String) payload.get("description");
            Boolean active = (Boolean) payload.get("active");
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) payload.get("permissions");

            SystemRole updated = rbacService.updateRole(id, displayName, description, active, permissions, userEmail);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<?> getRolePermissions(@PathVariable Long id) {
        return rbacService.getRoleById(id)
            .map(role -> ResponseEntity.ok(role.getPermissions()))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleRoleStatus(
            @PathVariable Long id,
            Principal principal) {
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            SystemRole updated = rbacService.toggleRoleStatus(id, userEmail);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<?> duplicateRole(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String newRoleName,
            Principal principal) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        String targetName = newRoleName;
        if (targetName == null && body != null && body.containsKey("newRoleName")) {
            targetName = (String) body.get("newRoleName");
        }
        if (targetName == null || targetName.isBlank()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "newRoleName is required");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            SystemRole duplicated = rbacService.duplicateRole(id, targetName, userEmail);
            return ResponseEntity.ok(duplicated);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(
            @PathVariable Long id,
            Principal principal) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            rbacService.deleteRole(id, userEmail);
            Map<String, String> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Role deleted successfully.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/permissions")
    public ResponseEntity<Map<String, List<AppPermission>>> getAllPermissionsGrouped() {
        return ResponseEntity.ok(rbacService.getPermissionsGroupedByModule());
    }

    @PostMapping("/users/{employeeId}/assign")
    public ResponseEntity<?> assignRole(
            @PathVariable Long employeeId,
            @RequestParam Long roleId,
            Principal principal) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            rbacService.assignRoleToEmployee(employeeId, roleId, userEmail);
            Map<String, String> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Role assigned successfully.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
