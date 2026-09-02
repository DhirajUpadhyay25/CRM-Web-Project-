package in.project.main.controllers.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.entities.AppSetting;
import in.project.main.services.AppSettingService;

@RestController
@RequestMapping("/admin/api/settings")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminSettingsApiController {

    @Autowired
    private AppSettingService settingService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSettings() {
        Map<String, Object> result = new HashMap<>();
        String[] categories = {"GENERAL", "PLATFORM", "SECURITY", "EMAIL", "FILE", "MAINTENANCE"};
        for (String cat : categories) {
            result.put(cat.toLowerCase(), settingService.getCategorySettingsMap(cat));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{category}")
    public ResponseEntity<Map<String, Object>> getCategorySettings(@PathVariable String category) {
        return ResponseEntity.ok(settingService.getCategorySettingsMap(category.toUpperCase()));
    }

    @PutMapping("/{category}")
    public ResponseEntity<Map<String, Object>> updateCategorySettings(
            @PathVariable String category,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        settingService.updateCategorySettings(category.toUpperCase(), payload, userEmail);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Settings for category '" + category + "' updated successfully.");
        response.put("data", settingService.getCategorySettingsMap(category.toUpperCase()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(
            @RequestParam(required = false, defaultValue = "admin@edutake.com") String recipientEmail,
            Principal principal) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        try {
            settingService.sendTestEmail(recipientEmail, userEmail);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Test email successfully dispatched to " + recipientEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("message", "Failed to send test email: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/maintenance/toggle")
    public ResponseEntity<Map<String, Object>> toggleMaintenance(
            @RequestParam(required = false) Boolean enabled,
            Principal principal) {
        
        String userEmail = principal != null ? principal.getName() : "ADMIN";
        boolean current = settingService.getBoolean("maintenance.enabled", false);
        boolean target = (enabled != null) ? enabled : !current;
        
        settingService.updateSetting("maintenance.enabled", String.valueOf(target), userEmail);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("maintenanceEnabled", target);
        response.put("message", "Maintenance mode is now " + (target ? "ENABLED" : "DISABLED") + ".");
        return ResponseEntity.ok(response);
    }
}
