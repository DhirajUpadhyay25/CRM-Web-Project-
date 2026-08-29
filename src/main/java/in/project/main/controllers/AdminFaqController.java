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
import in.project.main.entities.Faq;
import in.project.main.repositories.FaqRepository;

@Controller
@RequestMapping("/admin/faqs")
public class AdminFaqController {

    @Autowired
    private FaqRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/content/faqs/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String question,
                      @RequestParam String answer,
                      @RequestParam String category,
                      @RequestParam Boolean isActive,
                      RedirectAttributes ra) {
        try {
            Faq faq = new Faq();
            faq.setQuestion(question);
            faq.setAnswer(answer);
            faq.setCategory(category);
            faq.setIsActive(isActive);
            repository.save(faq);
            ra.addFlashAttribute("success", "FAQ created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create FAQ: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String question,
                         @RequestParam String answer,
                         @RequestParam String category,
                         @RequestParam Boolean isActive,
                         RedirectAttributes ra) {
        try {
            Faq faq = repository.findById(id).orElseThrow(() -> new RuntimeException("FAQ not found"));
            faq.setQuestion(question);
            faq.setAnswer(answer);
            faq.setCategory(category);
            faq.setIsActive(isActive);
            repository.save(faq);
            ra.addFlashAttribute("success", "FAQ updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update FAQ: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "FAQ deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete FAQ: " + e.getMessage());
        }
        return "redirect:/admin/faqs";
    }
}
