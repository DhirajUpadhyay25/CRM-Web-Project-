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
import in.project.main.entities.Media;
import in.project.main.repositories.MediaRepository;

@Controller
@RequestMapping("/admin/media")
public class AdminMediaController {

    @Autowired
    private MediaRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/content/media/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String fileName,
                      @RequestParam String fileUrl,
                      @RequestParam String fileType,
                      @RequestParam String size,
                      RedirectAttributes ra) {
        try {
            Media media = new Media();
            media.setFileName(fileName);
            media.setFileUrl(fileUrl);
            media.setFileType(fileType);
            media.setSize(size);
            repository.save(media);
            ra.addFlashAttribute("success", "Media created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create media: " + e.getMessage());
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Media deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete media: " + e.getMessage());
        }
        return "redirect:/admin/media";
    }
}
