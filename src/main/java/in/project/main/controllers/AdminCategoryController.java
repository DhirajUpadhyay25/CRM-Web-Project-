package in.project.main.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.dto.CategoryDTO;
import in.project.main.entities.Category;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.services.AuditLogService;
import in.project.main.services.CategoryService;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @GetMapping
    public String listCategories(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            Model model) {

        List<Category> categories;

        if (search != null && !search.trim().isEmpty()) {
            categories = categoryService.searchCategories(search.trim());
            model.addAttribute("search", search.trim());
        } else if ("active".equalsIgnoreCase(status)) {
            categories = categoryService.getCategoriesByStatus(true);
            model.addAttribute("status", "active");
        } else if ("inactive".equalsIgnoreCase(status)) {
            categories = categoryService.getCategoriesByStatus(false);
            model.addAttribute("status", "inactive");
        } else {
            categories = categoryService.getAllCategories();
            model.addAttribute("status", "all");
        }

        model.addAttribute("categories", categories);
        model.addAttribute("categoryDTO", new CategoryDTO());

        // Statistics
        model.addAttribute("totalCategories", categoryService.getTotalCategories());
        model.addAttribute("activeCategories", categoryService.getActiveCategoriesCount());
        model.addAttribute("inactiveCategories", categoryService.getInactiveCategoriesCount());
        model.addAttribute("categoriesWithCourses", categoryService.getCategoriesWithCoursesCount());

        return "admin/categories/list";
    }

    @PostMapping("/add")
    public String addCategory(@ModelAttribute("categoryDTO") CategoryDTO categoryDTO, Principal principal, RedirectAttributes redirectAttributes) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Category created = categoryService.createCategory(categoryDTO);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "CATEGORY_CREATED",
                    "Admin created category '" + created.getName() + "'."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("CATEGORY", String.valueOf(created.getId()), created.getName())
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.INFO);

                auditLogService.record(audit);
            }

            redirectAttributes.addFlashAttribute("successMsg", "Category created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to create category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/edit")
    public String editCategory(@RequestParam("id") Long id, @ModelAttribute("categoryDTO") CategoryDTO categoryDTO, Principal principal, RedirectAttributes redirectAttributes) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Category updated = categoryService.updateCategory(id, categoryDTO);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "CATEGORY_UPDATED",
                    "Admin updated category #" + id + " ('" + (updated != null ? updated.getName() : categoryDTO.getName()) + "')."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("CATEGORY", String.valueOf(id), categoryDTO.getName())
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.INFO);

                auditLogService.record(audit);
            }

            redirectAttributes.addFlashAttribute("successMsg", "Category updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to update category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            categoryService.toggleStatus(id);
            Category cat = categoryService.getCategoryById(id).orElse(null);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "CATEGORY_STATUS_TOGGLED",
                    "Admin changed category #" + id + " status to " + (cat != null && cat.isActive() ? "ACTIVE" : "INACTIVE") + "."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("CATEGORY", String.valueOf(id), cat != null ? cat.getName() : "Category #" + id)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.LOW);

                auditLogService.record(audit);
            }

            redirectAttributes.addFlashAttribute("successMsg", "Category status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to update status: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable("id") Long id, Principal principal, RedirectAttributes redirectAttributes) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            categoryService.deleteCategory(id);

            if (auditLogService != null) {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "CATEGORY_DELETED",
                    "Admin deleted category ID #" + id + "."
                )
                .withActor(null, actorEmail, "Admin", "ADMIN")
                .withEntity("CATEGORY", String.valueOf(id), "Category #" + id)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.MEDIUM);

                auditLogService.record(audit);
            }

            redirectAttributes.addFlashAttribute("successMsg", "Category deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
