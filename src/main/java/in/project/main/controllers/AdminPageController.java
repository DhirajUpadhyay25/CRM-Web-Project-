package in.project.main.controllers;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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

import in.project.main.entities.enums.ContentStatus;
import in.project.main.entities.enums.ContentVisibility;
import in.project.main.services.PageService;

@Controller
@RequestMapping("/admin/pages")
public class AdminPageController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");

    @Autowired
    private PageService pageService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) ContentVisibility visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort,
            Principal principal,
            Model model) {

        // Auto seed default pages if empty
        String actor = (principal != null) ? principal.getName() : "admin@edutake.com";
        pageService.seedDefaultPagesIfEmpty(actor);

        // Sorting
        Sort sortObj = Sort.by(Sort.Direction.DESC, "updatedAt");
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",");
            Sort.Direction dir = "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sortObj = Sort.by(dir, parts[0]);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);
        Page<in.project.main.entities.Page> pages = pageService.getPagesPaged(keyword, status, visibility, pageable);

        model.addAttribute("pages", pages);
        model.addAttribute("items", pages.getContent());
        model.addAttribute("analytics", pageService.getAnalytics());
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("status", status);
        model.addAttribute("visibility", visibility);
        model.addAttribute("sort", sort);
        model.addAttribute("contentStatuses", ContentStatus.values());
        model.addAttribute("contentVisibilities", ContentVisibility.values());
        model.addAttribute("currentPage", pages.getNumber());
        model.addAttribute("totalPages", pages.getTotalPages());
        model.addAttribute("totalElements", pages.getTotalElements());

        return "admin/content/pages/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        in.project.main.entities.Page page = new in.project.main.entities.Page();
        page.setStatus(ContentStatus.PUBLISHED);
        page.setVisibility(ContentVisibility.PUBLIC);

        model.addAttribute("page", page);
        model.addAttribute("isNew", true);
        model.addAttribute("contentStatuses", ContentStatus.values());
        model.addAttribute("contentVisibilities", ContentVisibility.values());
        return "admin/content/pages/form";
    }

    @PostMapping("/create")
    public String create(
            @ModelAttribute("page") in.project.main.entities.Page page,
            @RequestParam(required = false, defaultValue = "false") boolean saveAndContinue,
            Principal principal,
            RedirectAttributes ra) {

        try {
            if (page.getTitle() == null || page.getTitle().trim().isEmpty()) {
                ra.addFlashAttribute("errorMsg", "Page title is mandatory.");
                return "redirect:/admin/pages/create";
            }

            String authorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            in.project.main.entities.Page saved = pageService.createPage(page, authorEmail);
            ra.addFlashAttribute("successMsg", "Page '" + saved.getTitle() + "' was successfully created.");

            if (saveAndContinue) {
                return "redirect:/admin/pages/" + saved.getId() + "/edit";
            }
            return "redirect:/admin/pages";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to create page: " + e.getMessage());
            return "redirect:/admin/pages/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<in.project.main.entities.Page> pageOpt = pageService.findById(id);
        if (pageOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Page not found with ID: " + id);
            return "redirect:/admin/pages";
        }

        model.addAttribute("page", pageOpt.get());
        model.addAttribute("isNew", false);
        model.addAttribute("contentStatuses", ContentStatus.values());
        model.addAttribute("contentVisibilities", ContentVisibility.values());
        return "admin/content/pages/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("page") in.project.main.entities.Page page,
            @RequestParam(required = false, defaultValue = "false") boolean saveAndContinue,
            Principal principal,
            RedirectAttributes ra) {

        try {
            if (page.getTitle() == null || page.getTitle().trim().isEmpty()) {
                ra.addFlashAttribute("errorMsg", "Page title cannot be empty.");
                return "redirect:/admin/pages/" + id + "/edit";
            }

            String editorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            in.project.main.entities.Page updated = pageService.updatePage(id, page, editorEmail);
            ra.addFlashAttribute("successMsg", "Page '" + updated.getTitle() + "' updated successfully.");

            if (saveAndContinue) {
                return "redirect:/admin/pages/" + id + "/edit";
            }
            return "redirect:/admin/pages";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update page: " + e.getMessage());
            return "redirect:/admin/pages/" + id + "/edit";
        }
    }

    @GetMapping("/{id}")
    public String viewDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<in.project.main.entities.Page> pageOpt = pageService.findById(id);
        if (pageOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Page not found with ID: " + id);
            return "redirect:/admin/pages";
        }

        model.addAttribute("page", pageOpt.get());
        return "admin/content/pages/detail";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getPageJson(@PathVariable Long id) {
        Optional<in.project.main.entities.Page> pageOpt = pageService.findById(id);
        if (pageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        in.project.main.entities.Page p = pageOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("id", p.getId());
        data.put("title", p.getTitle());
        data.put("slug", p.getSlug());
        data.put("summary", p.getSummary());
        data.put("content", p.getContent());
        data.put("featuredImage", p.getFeaturedImage());
        data.put("status", p.getStatus() != null ? p.getStatus().name() : "DRAFT");
        data.put("statusDisplayName", p.getStatus() != null ? p.getStatus().getDisplayName() : "Draft");
        data.put("statusBadgeClass", p.getStatusBadgeClass());
        data.put("visibility", p.getVisibility() != null ? p.getVisibility().getDisplayName() : "Public");
        data.put("seoTitle", p.getEffectiveSeoTitle());
        data.put("seoDescription", p.getEffectiveSeoDescription());
        data.put("seoKeywords", p.getSeoKeywords());
        data.put("canonicalUrl", p.getCanonicalUrl());
        data.put("authorEmail", p.getAuthorEmail() != null ? p.getAuthorEmail() : "Admin");
        data.put("publishedAtFormatted", p.getPublishedAt() != null ? p.getPublishedAt().format(DATE_FMT) : "Not published yet");
        data.put("updatedAtFormatted", p.getUpdatedAt() != null ? p.getUpdatedAt().format(DATE_FMT) : "N/A");
        data.put("publicUrl", "/page/" + p.getSlug());

        return ResponseEntity.ok(data);
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String editorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            in.project.main.entities.Page p = pageService.toggleStatus(id, editorEmail);
            ra.addFlashAttribute("successMsg", "Page '" + p.getTitle() + "' status changed to " + p.getStatus().getDisplayName() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update page status: " + e.getMessage());
        }
        return "redirect:/admin/pages";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String editorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            pageService.deletePage(id, editorEmail);
            ra.addFlashAttribute("successMsg", "Page deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to delete page: " + e.getMessage());
        }
        return "redirect:/admin/pages";
    }

    @PostMapping("/seed-defaults")
    public String seedDefaults(Principal principal, RedirectAttributes ra) {
        try {
            String actor = (principal != null) ? principal.getName() : "admin@edutake.com";
            int count = pageService.seedDefaultPagesIfEmpty(actor);
            if (count > 0) {
                ra.addFlashAttribute("successMsg", "Successfully seeded " + count + " default policy and info pages.");
            } else {
                ra.addFlashAttribute("errorMsg", "Standard pages already exist. No new pages were added.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to seed default pages: " + e.getMessage());
        }
        return "redirect:/admin/pages";
    }
}
