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
import in.project.main.entities.Announcement;
import in.project.main.repositories.AnnouncementRepository;

@Controller
@RequestMapping("/admin/announcements")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/communication/announcements/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String title,
                      @RequestParam String content,
                      @RequestParam String targetAudience,
                      @RequestParam String publishDate,
                      @RequestParam Boolean isActive,
                      RedirectAttributes ra) {
        try {
            Announcement announcement = new Announcement();
            announcement.setTitle(title);
            announcement.setContent(content);
            announcement.setTargetAudience(targetAudience);
            announcement.setPublishDate(publishDate);
            announcement.setIsActive(isActive);
            repository.save(announcement);
            ra.addFlashAttribute("success", "Announcement created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam String targetAudience,
                         @RequestParam String publishDate,
                         @RequestParam Boolean isActive,
                         RedirectAttributes ra) {
        try {
            Announcement announcement = repository.findById(id).orElseThrow(() -> new RuntimeException("Announcement not found"));
            announcement.setTitle(title);
            announcement.setContent(content);
            announcement.setTargetAudience(targetAudience);
            announcement.setPublishDate(publishDate);
            announcement.setIsActive(isActive);
            repository.save(announcement);
            ra.addFlashAttribute("success", "Announcement updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Announcement deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }
}
