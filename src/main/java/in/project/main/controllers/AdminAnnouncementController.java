package in.project.main.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Announcement;
import in.project.main.entities.Course;
import in.project.main.repositories.CourseRepository;
import in.project.main.services.AnnouncementService;

@Controller
@RequestMapping("/admin/announcements")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping
    public String listAnnouncements(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "targetAudience", required = false) String targetAudience,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("pinned").descending().and(Sort.by("id").descending()));
        Page<Announcement> announcementPage = announcementService.getAnnouncementsPage(
                search, targetAudience, priority, isActive, pageable);

        Map<String, Object> metrics = announcementService.getAnnouncementMetrics();
        List<Course> courses = courseRepository.findAll();

        model.addAttribute("announcementPage", announcementPage);
        model.addAttribute("announcements", announcementPage.getContent());
        model.addAttribute("metrics", metrics);
        model.addAttribute("courses", courses);
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("selectedAudience", targetAudience != null ? targetAudience : "ALL_AUDIENCES");
        model.addAttribute("selectedPriority", priority != null ? priority : "ALL_PRIORITIES");
        model.addAttribute("selectedStatus", isActive != null ? String.valueOf(isActive) : "ALL");

        return "admin/communication/announcements/list";
    }

    @PostMapping({"/add", "/create"})
    public String addAnnouncement(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "ALL") String targetAudience,
            @RequestParam(defaultValue = "GENERAL") String category,
            @RequestParam(defaultValue = "NORMAL") String priority,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String publishDate,
            @RequestParam(required = false) String expiresAt,
            @RequestParam(defaultValue = "false") Boolean pinned,
            @RequestParam(defaultValue = "true") Boolean isActive,
            @RequestParam(defaultValue = "false") Boolean broadcastNotification,
            Principal principal,
            RedirectAttributes ra) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Announcement a = new Announcement();
            a.setTitle(title);
            a.setContent(content);
            a.setTargetAudience(targetAudience);
            a.setCategory(category);
            a.setPriority(priority);
            a.setCourseId(courseId);
            a.setCourseName(courseName);
            a.setPublishDate(publishDate);
            a.setExpiresAt(expiresAt);
            a.setPinned(pinned);
            a.setIsActive(isActive);

            announcementService.createAnnouncement(a, broadcastNotification, actorEmail);
            ra.addFlashAttribute("successMsg", "Announcement '" + title + "' published successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to create announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/update")
    public String updateAnnouncement(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "ALL") String targetAudience,
            @RequestParam(defaultValue = "GENERAL") String category,
            @RequestParam(defaultValue = "NORMAL") String priority,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String publishDate,
            @RequestParam(required = false) String expiresAt,
            @RequestParam(defaultValue = "false") Boolean pinned,
            @RequestParam(defaultValue = "true") Boolean isActive,
            Principal principal,
            RedirectAttributes ra) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Announcement a = new Announcement();
            a.setTitle(title);
            a.setContent(content);
            a.setTargetAudience(targetAudience);
            a.setCategory(category);
            a.setPriority(priority);
            a.setCourseId(courseId);
            a.setCourseName(courseName);
            a.setPublishDate(publishDate);
            a.setExpiresAt(expiresAt);
            a.setPinned(pinned);
            a.setIsActive(isActive);

            announcementService.updateAnnouncement(id, a, actorEmail);
            ra.addFlashAttribute("successMsg", "Announcement updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Announcement updated = announcementService.toggleActiveStatus(id, actorEmail);
            ra.addFlashAttribute("successMsg", "Announcement is now " + (Boolean.TRUE.equals(updated.getIsActive()) ? "Active" : "Inactive") + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to toggle status: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/toggle-pinned")
    public String togglePinned(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Announcement updated = announcementService.togglePinnedStatus(id, actorEmail);
            ra.addFlashAttribute("successMsg", "Announcement " + (Boolean.TRUE.equals(updated.getPinned()) ? "pinned to top." : "unpinned."));
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to toggle pin: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/delete")
    public String deleteAnnouncement(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes ra) {
        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            announcementService.deleteAnnouncement(id, actorEmail);
            ra.addFlashAttribute("successMsg", "Announcement deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to delete announcement: " + e.getMessage());
        }
        return "redirect:/admin/announcements";
    }
}
