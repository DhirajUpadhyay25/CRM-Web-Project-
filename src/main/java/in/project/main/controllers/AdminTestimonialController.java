package in.project.main.controllers;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Course;
import in.project.main.entities.Testimonial;
import in.project.main.entities.User;
import in.project.main.entities.enums.TestimonialSource;
import in.project.main.entities.enums.TestimonialStatus;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.TestimonialService;

@Controller
@RequestMapping("/admin/testimonials")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('testimonials.view') or hasAuthority('content.view')")
public class AdminTestimonialController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTestimonialController.class);

    @Autowired
    private TestimonialService testimonialService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    // ==========================================
    // List View with KPIs, Search & Filters
    // ==========================================

    @GetMapping
    public String list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) TestimonialStatus status,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) TestimonialSource source,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
        Page<Testimonial> testimonialPage = testimonialService.getTestimonials(search, courseId, status, rating, source, featured, pageable);

        model.addAttribute("testimonials", testimonialPage);
        model.addAttribute("kpis", testimonialService.getMetrics());
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("statuses", TestimonialStatus.values());
        model.addAttribute("sources", TestimonialSource.values());

        // Retain filters in UI
        model.addAttribute("search", search);
        model.addAttribute("courseId", courseId);
        model.addAttribute("status", status);
        model.addAttribute("rating", rating);
        model.addAttribute("source", source);
        model.addAttribute("featured", featured);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        return "admin/content/testimonials/list";
    }

    // ==========================================
    // Create Testimonial
    // ==========================================

    @GetMapping("/create")
    public String createForm(Model model) {
        Testimonial testimonial = new Testimonial();
        testimonial.setStatus(TestimonialStatus.PUBLISHED);
        testimonial.setSource(TestimonialSource.ADMIN_CREATED);
        testimonial.setRating(5);
        testimonial.setDisplayOrder(0);
        testimonial.setIsFeatured(false);
        testimonial.setConsentName(true);
        testimonial.setConsentPhoto(true);
        testimonial.setConsentPublish(true);

        model.addAttribute("testimonial", testimonial);
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("statuses", TestimonialStatus.values());
        model.addAttribute("sources", TestimonialSource.values());
        model.addAttribute("isEdit", false);
        return "admin/content/testimonials/form";
    }

    @PostMapping("/create")
    public String createTestimonial(
            @ModelAttribute Testimonial testimonial,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long studentId,
            Principal principal,
            RedirectAttributes ra) {

        try {
            Long actorId = null;
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName());
                if (user != null) actorId = user.getId();
            }

            Testimonial saved = testimonialService.saveTestimonial(testimonial, courseId, studentId, actorId, actorEmail);
            ra.addFlashAttribute("success", "Testimonial for '" + saved.getDisplayStudentName() + "' created successfully.");
            return "redirect:/admin/testimonials/" + saved.getId();
        } catch (Exception e) {
            logger.error("Failed to create testimonial: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Error creating testimonial: " + e.getMessage());
            return "redirect:/admin/testimonials/create";
        }
    }

    // ==========================================
    // Edit Testimonial
    // ==========================================

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return testimonialService.getTestimonialById(id)
                .map(testimonial -> {
                    model.addAttribute("testimonial", testimonial);
                    model.addAttribute("courses", courseRepository.findAll());
                    model.addAttribute("statuses", TestimonialStatus.values());
                    model.addAttribute("sources", TestimonialSource.values());
                    model.addAttribute("isEdit", true);
                    return "admin/content/testimonials/form";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Testimonial not found with ID " + id);
                    return "redirect:/admin/testimonials";
                });
    }

    @PostMapping("/{id}/edit")
    public String updateTestimonial(
            @PathVariable Long id,
            @ModelAttribute Testimonial testimonial,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long studentId,
            Principal principal,
            RedirectAttributes ra) {

        try {
            testimonial.setId(id);
            Long actorId = null;
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName());
                if (user != null) actorId = user.getId();
            }

            Testimonial updated = testimonialService.saveTestimonial(testimonial, courseId, studentId, actorId, actorEmail);
            ra.addFlashAttribute("success", "Testimonial for '" + updated.getDisplayStudentName() + "' updated successfully.");
            return "redirect:/admin/testimonials/" + updated.getId();
        } catch (Exception e) {
            logger.error("Failed to update testimonial #{}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("error", "Error updating testimonial: " + e.getMessage());
            return "redirect:/admin/testimonials/edit/" + id;
        }
    }

    // ==========================================
    // View Details
    // ==========================================

    @GetMapping("/{id}")
    public String viewDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return testimonialService.getTestimonialById(id)
                .map(testimonial -> {
                    model.addAttribute("testimonial", testimonial);
                    model.addAttribute("statuses", TestimonialStatus.values());
                    return "admin/content/testimonials/detail";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Testimonial not found with ID " + id);
                    return "redirect:/admin/testimonials";
                });
    }

    // ==========================================
    // Status & Feature Modifiers
    // ==========================================

    @PostMapping("/{id}/publish")
    public String publishTestimonial(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            testimonialService.updateStatus(id, TestimonialStatus.PUBLISHED, "Published by Administrator", actorEmail);
            ra.addFlashAttribute("success", "Testimonial #" + id + " published successfully to public storefront.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to publish testimonial: " + e.getMessage());
        }
        return "redirect:/admin/testimonials/" + id;
    }

    @PostMapping("/{id}/toggle-featured")
    public String toggleFeatured(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            boolean isFeatured = testimonialService.toggleFeatured(id, actorEmail);
            ra.addFlashAttribute("success", "Testimonial #" + id + " featured spotlight is now " + (isFeatured ? "ACTIVE" : "INACTIVE") + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to toggle spotlight: " + e.getMessage());
        }
        return "redirect:/admin/testimonials";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam TestimonialStatus status,
            @RequestParam(required = false) String moderationReason,
            Principal principal,
            RedirectAttributes ra) {
        try {
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            testimonialService.updateStatus(id, status, moderationReason, actorEmail);
            ra.addFlashAttribute("success", "Testimonial #" + id + " status changed to " + status + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update status: " + e.getMessage());
        }
        return "redirect:/admin/testimonials/" + id;
    }

    // ==========================================
    // Delete Testimonial
    // ==========================================

    @PostMapping("/{id}/delete")
    public String deleteTestimonial(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            testimonialService.softDelete(id, actorEmail);
            ra.addFlashAttribute("success", "Testimonial #" + id + " deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete testimonial: " + e.getMessage());
        }
        return "redirect:/admin/testimonials";
    }

    // ==========================================
    // Default Seeder
    // ==========================================

    @PostMapping("/seed-defaults")
    public String seedDefaults(RedirectAttributes ra) {
        try {
            int count = testimonialService.seedDefaultTestimonials();
            if (count > 0) {
                ra.addFlashAttribute("success", "Successfully seeded " + count + " inspiring student success stories!");
            } else {
                ra.addFlashAttribute("success", "Standard testimonials already exist.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to seed default testimonials: " + e.getMessage());
        }
        return "redirect:/admin/testimonials";
    }
}
