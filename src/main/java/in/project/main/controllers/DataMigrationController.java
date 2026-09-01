package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.services.DataSeederService;

/**
 * Development-only password migration. POST-only and dev-profile-only: this rewrites the
 * password column for every user, which is not something a GET request should ever do.
 */
@Controller
@Profile("dev")
public class DataMigrationController {

    @Autowired
    private DataSeederService dataSeederService;

    @PostMapping("/admin/migrate-passwords")
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
