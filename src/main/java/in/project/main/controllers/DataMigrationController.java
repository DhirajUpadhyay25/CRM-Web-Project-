package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.services.DataSeederService;

@Controller
public class DataMigrationController {

    @Autowired
    private DataSeederService dataSeederService;

    @GetMapping("/admin/migrate-passwords")
    public String migratePasswords(RedirectAttributes redirectAttributes) {
        try {
            dataSeederService.migratePlaintextPasswords();
            redirectAttributes.addFlashAttribute("successMsg", "Plaintext passwords migrated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Password migration failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
