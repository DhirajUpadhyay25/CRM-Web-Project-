package in.project.main.controllers;

import in.project.main.services.DataSeederService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class DataSeederController {

    @Autowired
    private DataSeederService dataSeederService;

    @GetMapping("/seed-test-data")
    public String seedData(RedirectAttributes redirectAttributes) {
        try {
            dataSeederService.seedAll();
            redirectAttributes.addFlashAttribute("successMsg", "Test data seeded successfully for all new modules!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to seed data: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/seed-enrollments")
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
