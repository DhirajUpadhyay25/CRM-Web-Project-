package in.project.main.controllers;

import java.security.Principal;
import java.util.Map;

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

import in.project.main.services.AppSettingService;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminSettingsController {

    @Autowired
    private AppSettingService settingService;

    @GetMapping
    public String settings(Model model) {
        model.addAttribute("generalSettings", settingService.getCategorySettingsMap("GENERAL"));
        model.addAttribute("platformSettings", settingService.getCategorySettingsMap("PLATFORM"));
        model.addAttribute("securitySettings", settingService.getCategorySettingsMap("SECURITY"));
        model.addAttribute("emailSettings", settingService.getCategorySettingsMap("EMAIL"));
        model.addAttribute("fileSettings", settingService.getCategorySettingsMap("FILE"));
        model.addAttribute("maintenanceSettings", settingService.getCategorySettingsMap("MAINTENANCE"));
        
        model.addAttribute("isMaintenanceActive", settingService.getBoolean("maintenance.enabled", false));
        return "admin/system/settings/list";
    }

    @PostMapping("/update/{category}")
    public String updateCategory(
            @PathVariable String category,
            @RequestParam Map<String, String> allParams,
            Principal principal,
            RedirectAttributes ra) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            settingService.updateCategorySettings(category.toUpperCase(), allParams, userEmail);
            ra.addFlashAttribute("successMsg", "Settings for " + category + " updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update " + category + " settings: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/email/test")
    public String sendTestEmail(
            @RequestParam(required = false, defaultValue = "admin@edutake.com") String testRecipient,
            Principal principal,
            RedirectAttributes ra) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            settingService.sendTestEmail(testRecipient, userEmail);
            ra.addFlashAttribute("successMsg", "Test email successfully sent to " + testRecipient);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to send test email: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }
}
