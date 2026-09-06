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

import in.project.main.entities.Blog;
import in.project.main.entities.BlogCategory;
import in.project.main.entities.enums.ContentStatus;
import in.project.main.entities.enums.ContentVisibility;
import in.project.main.repositories.BlogCategoryRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.services.BlogService;

@Controller
@RequestMapping("/admin/blogs")
public class AdminBlogController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy, hh:mm a");

    @Autowired
    private BlogService blogService;

    @Autowired
    private BlogCategoryRepository blogCategoryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ContentVisibility visibility,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort,
            Principal principal,
            Model model) {

        // Auto seed default articles if empty
        String actor = (principal != null) ? principal.getName() : "admin@edutake.com";
        blogService.seedDefaultBlogsIfEmpty(actor);

        // Sorting
        Sort sortObj = Sort.by(Sort.Direction.DESC, "updatedAt");
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",");
            Sort.Direction dir = "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sortObj = Sort.by(dir, parts[0]);
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);
        Page<Blog> blogs = blogService.getBlogsPaged(keyword, status, categoryId, visibility, isFeatured, pageable);

        model.addAttribute("blogs", blogs);
        model.addAttribute("items", blogs.getContent());
        model.addAttribute("analytics", blogService.getAnalytics());
        model.addAttribute("categories", blogService.getAllCategories());
        model.addAttribute("contentStatuses", ContentStatus.values());
        model.addAttribute("contentVisibilities", ContentVisibility.values());
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("status", status);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("visibility", visibility);
        model.addAttribute("isFeatured", isFeatured);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", blogs.getNumber());
        model.addAttribute("totalPages", blogs.getTotalPages());
        model.addAttribute("totalElements", blogs.getTotalElements());

        return "admin/content/blogs/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        Blog blog = new Blog();
        blog.setStatus(ContentStatus.PUBLISHED);
        blog.setVisibility(ContentVisibility.PUBLIC);

        model.addAttribute("blog", blog);
        model.addAttribute("isNew", true);
        model.addAttribute("categories", blogService.getAllCategories());
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("contentStatuses", ContentStatus.values());
        model.addAttribute("contentVisibilities", ContentVisibility.values());
        return "admin/content/blogs/form";
    }

    @PostMapping("/create")
    public String create(
            @ModelAttribute("blog") Blog blog,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean saveAndContinue,
            Principal principal,
            RedirectAttributes ra) {

        try {
            if (blog.getTitle() == null || blog.getTitle().trim().isEmpty()) {
                ra.addFlashAttribute("errorMsg", "Article title is mandatory.");
                return "redirect:/admin/blogs/create";
            }

            if (categoryId != null && categoryId > 0) {
                BlogCategory cat = blogCategoryRepository.findById(categoryId).orElse(null);
                blog.setBlogCategory(cat);
            }

            String authorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            Blog saved = blogService.createBlog(blog, authorEmail);
            ra.addFlashAttribute("successMsg", "Article '" + saved.getTitle() + "' was successfully created.");

            if (saveAndContinue) {
                return "redirect:/admin/blogs/" + saved.getId() + "/edit";
            }
            return "redirect:/admin/blogs";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to create article: " + e.getMessage());
            return "redirect:/admin/blogs/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Blog> blogOpt = blogService.findById(id);
        if (blogOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Article not found with ID: " + id);
            return "redirect:/admin/blogs";
        }

        model.addAttribute("blog", blogOpt.get());
        model.addAttribute("isNew", false);
        model.addAttribute("categories", blogService.getAllCategories());
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("contentStatuses", ContentStatus.values());
        model.addAttribute("contentVisibilities", ContentVisibility.values());
        return "admin/content/blogs/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @ModelAttribute("blog") Blog blog,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean saveAndContinue,
            Principal principal,
            RedirectAttributes ra) {

        try {
            if (blog.getTitle() == null || blog.getTitle().trim().isEmpty()) {
                ra.addFlashAttribute("errorMsg", "Article title cannot be empty.");
                return "redirect:/admin/blogs/" + id + "/edit";
            }

            String editorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            Blog updated = blogService.updateBlog(id, blog, categoryId, editorEmail);
            ra.addFlashAttribute("successMsg", "Article '" + updated.getTitle() + "' updated successfully.");

            if (saveAndContinue) {
                return "redirect:/admin/blogs/" + id + "/edit";
            }
            return "redirect:/admin/blogs";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update article: " + e.getMessage());
            return "redirect:/admin/blogs/" + id + "/edit";
        }
    }

    @GetMapping("/{id}")
    public String viewDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Blog> blogOpt = blogService.findById(id);
        if (blogOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Article not found with ID: " + id);
            return "redirect:/admin/blogs";
        }

        model.addAttribute("blog", blogOpt.get());
        return "admin/content/blogs/detail";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getBlogJson(@PathVariable Long id) {
        Optional<Blog> blogOpt = blogService.findById(id);
        if (blogOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Blog b = blogOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("id", b.getId());
        data.put("title", b.getTitle());
        data.put("slug", b.getSlug());
        data.put("excerpt", b.getExcerpt());
        data.put("content", b.getContent());
        data.put("featuredImage", b.getFeaturedImage());
        data.put("categoryName", b.getCategoryName());
        data.put("tags", b.getTags());
        data.put("author", b.getAuthor() != null ? b.getAuthor() : "Admin");
        data.put("authorEmail", b.getAuthorEmail() != null ? b.getAuthorEmail() : "admin@edutake.com");
        data.put("status", b.getStatus() != null ? b.getStatus().name() : "DRAFT");
        data.put("statusDisplayName", b.getStatus() != null ? b.getStatus().getDisplayName() : "Draft");
        data.put("statusBadgeClass", b.getStatusBadgeClass());
        data.put("visibility", b.getVisibility() != null ? b.getVisibility().getDisplayName() : "Public");
        data.put("isFeatured", b.isFeatured());
        data.put("viewCount", b.getViewCount() != null ? b.getViewCount() : 0L);
        data.put("seoTitle", b.getSeoTitle());
        data.put("seoDescription", b.getSeoDescription());
        data.put("publishedAtFormatted", b.getPublishedAt() != null ? b.getPublishedAt().format(DATE_FMT) : "Not published yet");
        data.put("updatedAtFormatted", b.getUpdatedAt() != null ? b.getUpdatedAt().format(DATE_FMT) : "N/A");
        data.put("publicUrl", "/blog/" + b.getSlug());

        return ResponseEntity.ok(data);
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String editorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            Blog b = blogService.toggleStatus(id, editorEmail);
            ra.addFlashAttribute("successMsg", "Article '" + b.getTitle() + "' status changed to " + b.getStatus().getDisplayName() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update article status: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @PostMapping("/{id}/toggle-featured")
    public String toggleFeatured(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String editorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            Blog b = blogService.toggleFeatured(id, editorEmail);
            ra.addFlashAttribute("successMsg", "Article '" + b.getTitle() + "' featured state set to " + b.isFeatured() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to toggle featured status: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            String editorEmail = (principal != null) ? principal.getName() : "admin@edutake.com";
            blogService.deleteBlog(id, editorEmail);
            ra.addFlashAttribute("successMsg", "Article deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to delete article: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @PostMapping("/seed-defaults")
    public String seedDefaults(Principal principal, RedirectAttributes ra) {
        try {
            String actor = (principal != null) ? principal.getName() : "admin@edutake.com";
            int count = blogService.seedDefaultBlogsIfEmpty(actor);
            if (count > 0) {
                ra.addFlashAttribute("successMsg", "Successfully seeded " + count + " default articles and categories.");
            } else {
                ra.addFlashAttribute("errorMsg", "Articles already exist. No new articles were added.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to seed default articles: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }
}
