package in.project.main.controllers;

import in.project.main.services.DataSeederService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Development-only endpoints for populating demo data.
 *
 * These are POST-only and restricted to the dev profile. They were previously GET routes
 * linked as plain anchors, which meant a crawler, a prefetching browser or a stray link
 * click could rewrite application data.
 */
@Controller
@RequestMapping("/admin")
@Profile("dev")
public class DataSeederController {

    @Autowired
    private DataSeederService dataSeederService;

    @PostMapping("/seed-test-data")
    public String seedData(RedirectAttributes redirectAttributes) {
        try {
            dataSeederService.seedAll();
            redirectAttributes.addFlashAttribute("successMsg", "Test data seeded successfully for all new modules!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to seed data: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/seed-enrollments")
    public String seedEnrollments(RedirectAttributes redirectAttributes) {
        try {
            dataSeederService.seedStudentPanelData();
            redirectAttributes.addFlashAttribute("successMsg", "Student enrollments, orders, and notifications seeded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to seed enrollment data: " + e.getMessage());
        }
        return "redirect:/student/dashboard";
    }
}
