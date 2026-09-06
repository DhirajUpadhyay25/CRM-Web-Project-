package in.project.main.controllers;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Faq;
import in.project.main.entities.FaqCategory;
import in.project.main.entities.User;
import in.project.main.entities.enums.ContentVisibility;
import in.project.main.repositories.UserRepository;
import in.project.main.services.FaqService;

@Controller
@RequestMapping("/admin/faqs")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('faqs.view')")
public class AdminFaqController {

    private static final Logger logger = LoggerFactory.getLogger(AdminFaqController.class);

    @Autowired
    private FaqService faqService;

    @Autowired
    private UserRepository userRepository;

    private static final List<String> CONTEXT_TAGS = Arrays.asList(
            "GENERAL", "ENROLLMENT", "CLASS", "CERTIFICATE", "PAYMENT", "TECHNICAL", "ACCOUNT", "INSTRUCTOR"
    );

    // ==========================================
    // List View with KPIs, Search, and Filters
    // ==========================================

    @GetMapping
    public String list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String contextTag,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) ContentVisibility visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
        Page<Faq> faqPage = faqService.getFaqs(search, categoryId, contextTag, isActive, visibility, pageable);

        model.addAttribute("faqs", faqPage);
        model.addAttribute("kpis", faqService.getMetrics());
        model.addAttribute("categories", faqService.getAllCategories());
        model.addAttribute("contextTags", CONTEXT_TAGS);
        model.addAttribute("visibilities", ContentVisibility.values());

        // Retain active filters in UI
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("contextTag", contextTag);
        model.addAttribute("isActive", isActive);
        model.addAttribute("visibility", visibility);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);

        return "admin/content/faqs/list";
    }

    // ==========================================
    // Create FAQ
    // ==========================================

    @GetMapping("/create")
    public String createForm(Model model) {
        Faq faq = new Faq();
        faq.setIsActive(true);
        faq.setSortOrder(0);
        faq.setVisibility(ContentVisibility.PUBLIC);

        model.addAttribute("faq", faq);
        model.addAttribute("categories", faqService.getAllCategories());
        model.addAttribute("contextTags", CONTEXT_TAGS);
        model.addAttribute("visibilities", ContentVisibility.values());
        model.addAttribute("isEdit", false);
        return "admin/content/faqs/form";
    }

    @PostMapping("/create")
    public String saveFaq(
            @ModelAttribute Faq faq,
            @RequestParam(required = false) Long categoryId,
            Principal principal,
            RedirectAttributes ra) {

        try {
            Long actorId = null;
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName());
                if (user != null) actorId = user.getId();
            }

            faqService.saveFaq(faq, categoryId, actorId, actorEmail);
            ra.addFlashAttribute("success", "FAQ '" + faq.getQuestion() + "' created successfully.");
        } catch (Exception e) {
            logger.error("Failed to create FAQ: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Error creating FAQ: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }

    // ==========================================
    // Edit FAQ
    // ==========================================

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return faqService.getFaqById(id).map(faq -> {
            model.addAttribute("faq", faq);
            model.addAttribute("categories", faqService.getAllCategories());
            model.addAttribute("contextTags", CONTEXT_TAGS);
            model.addAttribute("visibilities", ContentVisibility.values());
            model.addAttribute("isEdit", true);
            return "admin/content/faqs/form";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "FAQ not found.");
            return "redirect:/admin/faqs";
        });
    }

    @PostMapping("/{id}/edit")
    public String updateFaq(
            @PathVariable Long id,
            @ModelAttribute Faq faq,
            @RequestParam(required = false) Long categoryId,
            Principal principal,
            RedirectAttributes ra) {

        try {
            faq.setId(id);
            Long actorId = null;
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName());
                if (user != null) actorId = user.getId();
            }

            faqService.saveFaq(faq, categoryId, actorId, actorEmail);
            ra.addFlashAttribute("success", "FAQ updated successfully.");
        } catch (Exception e) {
            logger.error("Failed to update FAQ ID {}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("error", "Error updating FAQ: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }

    // ==========================================
    // View FAQ Details Page
    // ==========================================

    @GetMapping("/{id}")
    public String viewDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return faqService.getFaqById(id).map(faq -> {
            model.addAttribute("faq", faq);
            return "admin/content/faqs/detail";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "FAQ not found.");
            return "redirect:/admin/faqs";
        });
    }

    // ==========================================
    // Categories Dedicated Page
    // ==========================================

    @GetMapping("/categories")
    public String categoriesPage(Model model) {
        model.addAttribute("categories", faqService.getAllCategories());
        model.addAttribute("contextTags", CONTEXT_TAGS);
        return "admin/content/faqs/categories";
    }

    // ==========================================
    // Quick View & Modal JSON API
    // ==========================================

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getFaqApi(@PathVariable Long id) {
        return faqService.getFaqById(id).map(faq -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", faq.getId());
            data.put("question", faq.getQuestion());
            data.put("answer", faq.getAnswer());
            data.put("categoryId", faq.getFaqCategory() != null ? faq.getFaqCategory().getId() : null);
            data.put("categoryName", faq.getCategoryName());
            data.put("categoryIcon", faq.getFaqCategory() != null ? faq.getFaqCategory().getIconClassSafe() : "bi-question-circle");
            data.put("contextTag", faq.getContextTag());
            data.put("sortOrder", faq.getSortOrderSafe());
            data.put("isActive", faq.isIsActive());
            data.put("visibility", faq.getVisibility() != null ? faq.getVisibility().name() : "PUBLIC");
            data.put("viewCount", faq.getViewCount() != null ? faq.getViewCount() : 0L);
            data.put("helpfulCount", faq.getHelpfulCount() != null ? faq.getHelpfulCount() : 0L);
            data.put("notHelpfulCount", faq.getNotHelpfulCount() != null ? faq.getNotHelpfulCount() : 0L);
            data.put("satisfactionRate", faq.getHelpfulnessRate());
            data.put("createdByEmail", faq.getCreatedByEmail() != null ? faq.getCreatedByEmail() : "Admin");
            data.put("createdAt", faq.getCreatedAt() != null ? faq.getCreatedAt().toString() : "");
            data.put("updatedAt", faq.getUpdatedAt() != null ? faq.getUpdatedAt().toString() : "");
            return ResponseEntity.ok(data);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // Status Toggle & Delete
    // ==========================================

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            boolean newState = faqService.toggleActive(id, actorEmail);
            ra.addFlashAttribute("success", "FAQ status changed to " + (newState ? "Active" : "Inactive") + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to toggle FAQ status: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }

    @PostMapping("/{id}/delete")
    public String deleteFaq(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String actorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            faqService.deleteFaq(id, actorEmail);
            ra.addFlashAttribute("success", "FAQ deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete FAQ: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }

    // ==========================================
    // Category Management
    // ==========================================

    @PostMapping("/categories/save")
    public String saveCategory(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String iconClass,
            @RequestParam(defaultValue = "0") Integer sortOrder,
            @RequestParam(defaultValue = "GENERAL") String contextTag,
            @RequestParam(defaultValue = "true") Boolean isActive,
            RedirectAttributes ra) {

        try {
            FaqCategory category = (id != null)
                    ? faqService.getCategoryById(id).orElse(new FaqCategory())
                    : new FaqCategory();

            category.setName(name);
            category.setSlug(slug);
            category.setDescription(description);
            category.setIconClass(iconClass);
            category.setSortOrder(sortOrder);
            category.setContextTag(contextTag);
            category.setActive(isActive != null && isActive);

            faqService.saveCategory(category);
            ra.addFlashAttribute("success", "Category '" + name + "' saved successfully.");
        } catch (Exception e) {
            logger.error("Failed to save FAQ category: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Error saving category: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        try {
            boolean deleted = faqService.deleteCategory(id);
            if (deleted) {
                ra.addFlashAttribute("success", "FAQ category deleted successfully.");
            } else {
                ra.addFlashAttribute("error", "Cannot delete category: it has active FAQs associated with it.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete category: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }

    // ==========================================
    // Seed Sample FAQs
    // ==========================================

    @PostMapping("/seed-defaults")
    public String seedDefaults(RedirectAttributes ra) {
        try {
            int count = faqService.seedDefaults(true);
            ra.addFlashAttribute("success", "Successfully seeded " + count + " default FAQs with categories.");
        } catch (Exception e) {
            logger.error("Failed to seed FAQs: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Error seeding FAQs: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }
}
