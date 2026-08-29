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
import in.project.main.entities.Blog;
import in.project.main.repositories.BlogRepository;

@Controller
@RequestMapping("/admin/blogs")
public class AdminBlogController {

    @Autowired
    private BlogRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/content/blogs/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String title,
                      @RequestParam String author,
                      @RequestParam String status,
                      @RequestParam String publishDate,
                      RedirectAttributes ra) {
        try {
            Blog blog = new Blog();
            blog.setTitle(title);
            blog.setAuthor(author);
            blog.setStatus(status);
            blog.setPublishDate(publishDate);
            repository.save(blog);
            ra.addFlashAttribute("success", "Blog created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create blog: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam String author,
                         @RequestParam String status,
                         @RequestParam String publishDate,
                         RedirectAttributes ra) {
        try {
            Blog blog = repository.findById(id).orElseThrow(() -> new RuntimeException("Blog not found"));
            blog.setTitle(title);
            blog.setAuthor(author);
            blog.setStatus(status);
            blog.setPublishDate(publishDate);
            repository.save(blog);
            ra.addFlashAttribute("success", "Blog updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update blog: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Blog deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete blog: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }
}
