package in.project.main.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminProfileController {

    @GetMapping("/admin/profile")
    public String adminProfile() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/adminProfile")
    public String adminProfileAlt() {
        return "redirect:/admin/dashboard";
    }
}
