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
import in.project.main.entities.Page;
import in.project.main.repositories.PageRepository;

@Controller
@RequestMapping("/admin/pages")
public class AdminPageController {

    @Autowired
    private PageRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/content/pages/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String title,
                      @RequestParam String slug,
                      @RequestParam String status,
                      RedirectAttributes ra) {
        try {
            Page page = new Page();
            page.setTitle(title);
            page.setSlug(slug);
            try {
                page.setStatus(in.project.main.entities.enums.ContentStatus.valueOf(status.toUpperCase()));
            } catch (Exception e) {
                page.setStatus(in.project.main.entities.enums.ContentStatus.DRAFT);
            }
            repository.save(page);
            ra.addFlashAttribute("success", "Page created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create page: " + e.getMessage());
        }
        return "redirect:/admin/pages";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam String slug,
                         @RequestParam String status,
                         RedirectAttributes ra) {
        try {
            Page page = repository.findById(id).orElseThrow(() -> new RuntimeException("Page not found"));
            page.setTitle(title);
            page.setSlug(slug);
            try {
                page.setStatus(in.project.main.entities.enums.ContentStatus.valueOf(status.toUpperCase()));
            } catch (Exception e) {
                page.setStatus(in.project.main.entities.enums.ContentStatus.DRAFT);
            }
            repository.save(page);
            ra.addFlashAttribute("success", "Page updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update page: " + e.getMessage());
        }
        return "redirect:/admin/pages";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Page deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete page: " + e.getMessage());
        }
        return "redirect:/admin/pages";
    }
}
