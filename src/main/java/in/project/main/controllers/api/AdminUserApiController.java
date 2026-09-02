package in.project.main.controllers.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import in.project.main.entities.Employee;
import in.project.main.services.EmployeeService;

@RestController
@RequestMapping("/admin/api/users")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminUserApiController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<Page<Employee>> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(employeeService.searchEmployees(q, role, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        String actorEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            String name = (String) payload.get("name");
            String email = (String) payload.get("email");
            String phoneno = (String) payload.get("phoneno");
            String city = (String) payload.get("city");
            String password = (String) payload.get("password");
            
            Long roleId = null;
            if (payload.get("roleId") != null) {
                roleId = Long.valueOf(payload.get("roleId").toString());
            }

            Employee emp = new Employee();
            emp.setName(name);
            emp.setEmail(email);
            emp.setPhoneno(phoneno);
            emp.setCity(city);
            emp.setPassword(password);

            Employee created = employeeService.createEmployeeWithRole(emp, roleId, actorEmail);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        String actorEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            String name = (String) payload.get("name");
            String phoneno = (String) payload.get("phoneno");
            String city = (String) payload.get("city");
            
            Long roleId = null;
            if (payload.get("roleId") != null) {
                roleId = Long.valueOf(payload.get("roleId").toString());
            }

            Employee updated = employeeService.updateEmployeeProfile(id, name, phoneno, city, roleId, actorEmail);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        String actorEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            String newPassword = payload.get("newPassword");
            employeeService.resetEmployeePassword(id, newPassword, actorEmail);
            Map<String, String> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "Password reset successfully.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            Principal principal) {
        String actorEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            employeeService.deleteEmployeeById(id, actorEmail);
            Map<String, String> resp = new HashMap<>();
            resp.put("status", "SUCCESS");
            resp.put("message", "User deleted successfully.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
