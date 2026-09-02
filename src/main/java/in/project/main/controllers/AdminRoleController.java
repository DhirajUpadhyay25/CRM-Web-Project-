package in.project.main.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Employee;
import in.project.main.entities.SystemRole;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.services.RbacService;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminRoleController {

    @Autowired
    private RbacService rbacService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping
    public String listRoles(Model model) {
        List<SystemRole> roles = rbacService.getAllRoles();
        model.addAttribute("roles", roles);
        model.addAttribute("modulesMap", rbacService.getPermissionsGroupedByModule());
        model.addAttribute("roleUserCounts", rbacService.getRoleUserCounts());
        model.addAttribute("totalPermissionsCount", rbacService.getTotalPermissionsCount());
        return "admin/system/roles/list";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleRoleStatus(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            SystemRole updated = rbacService.toggleRoleStatus(id, userEmail);
            ra.addFlashAttribute("successMsg", "Role '" + updated.getDisplayName() + "' is now " + (updated.isActive() ? "Active" : "Inactive") + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update role status: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/create")
    public String createRole(
            @RequestParam String roleName,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) List<String> permissions,
            Principal principal,
            RedirectAttributes ra) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            SystemRole created = rbacService.createRole(roleName, displayName, description, permissions, userEmail);
            ra.addFlashAttribute("successMsg", "Role '" + created.getDisplayName() + "' created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to create role: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/edit")
    public String editRole(
            @PathVariable Long id,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false) List<String> permissions,
            Principal principal,
            RedirectAttributes ra) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            SystemRole updated = rbacService.updateRole(id, displayName, description, active, permissions, userEmail);
            ra.addFlashAttribute("successMsg", "Role '" + updated.getDisplayName() + "' updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update role: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/duplicate")
    public String duplicateRole(
            @PathVariable Long id,
            @RequestParam String newRoleName,
            Principal principal,
            RedirectAttributes ra) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            SystemRole dup = rbacService.duplicateRole(id, newRoleName, userEmail);
            ra.addFlashAttribute("successMsg", "Role duplicated successfully into '" + dup.getDisplayName() + "'.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to duplicate role: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/delete")
    public String deleteRole(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            rbacService.deleteRole(id, userEmail);
            ra.addFlashAttribute("successMsg", "Role deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to delete role: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @GetMapping("/{id}/permissions")
    public String managePermissions(@PathVariable Long id, Model model, RedirectAttributes ra) {
        SystemRole role = rbacService.getRoleById(id).orElse(null);
        if (role == null) {
            ra.addFlashAttribute("errorMsg", "Role not found with ID: " + id);
            return "redirect:/admin/roles";
        }

        model.addAttribute("role", role);
        model.addAttribute("modulesMap", rbacService.getPermissionsGroupedByModule());
        return "admin/system/roles/permissions";
    }

    @PostMapping("/{id}/permissions")
    public String savePermissions(
            @PathVariable Long id,
            @RequestParam(required = false) List<String> permissions,
            Principal principal,
            RedirectAttributes ra) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            SystemRole updated = rbacService.updateRole(id, null, null, null, permissions, userEmail);
            ra.addFlashAttribute("successMsg", "Permissions for '" + updated.getDisplayName() + "' updated successfully (" + (updated.getPermissions() != null ? updated.getPermissions().size() : 0) + " granted).");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to save permissions: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<Employee> employees = employeeRepository.findAll();
        List<SystemRole> roles = rbacService.getAllRoles();
        model.addAttribute("employees", employees);
        model.addAttribute("roles", roles);
        return "admin/system/roles/users";
    }

    @PostMapping("/users/{employeeId}/assign")
    public String assignUserRole(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Long roleId,
            Principal principal,
            RedirectAttributes ra) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            rbacService.assignRoleToEmployee(employeeId, roleId, userEmail);
            ra.addFlashAttribute("successMsg", "User role assignment updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to assign role: " + e.getMessage());
        }
        return "redirect:/admin/roles/users";
    }
}
