package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import in.project.main.entities.SystemRole;
import in.project.main.repositories.SystemRoleRepository;

@Controller
@RequestMapping("/admin/roles")
public class AdminRoleController {

    @Autowired
    private SystemRoleRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/system/roles/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String roleName,
                      @RequestParam String description,
                      RedirectAttributes ra) {
        try {
            SystemRole role = new SystemRole();
            role.setRoleName(roleName);
            role.setDescription(description);
            repository.save(role);
            ra.addFlashAttribute("success", "Role created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create role: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Role deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete role: " + e.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
