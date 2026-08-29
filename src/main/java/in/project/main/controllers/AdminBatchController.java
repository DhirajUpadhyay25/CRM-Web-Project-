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
import in.project.main.entities.Batch;
import in.project.main.repositories.BatchRepository;

@Controller
@RequestMapping("/admin/batches")
public class AdminBatchController {

    @Autowired
    private BatchRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/learning/batches/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String name,
                      @RequestParam String courseId,
                      @RequestParam String startDate,
                      @RequestParam String status,
                      RedirectAttributes ra) {
        try {
            Batch batch = new Batch();
            batch.setName(name);
            batch.setCourseId(courseId);
            batch.setStartDate(startDate);
            batch.setStatus(status);
            repository.save(batch);
            ra.addFlashAttribute("success", "Batch created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create batch: " + e.getMessage());
        }
        return "redirect:/admin/batches";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam String courseId,
                         @RequestParam String startDate,
                         @RequestParam String status,
                         RedirectAttributes ra) {
        try {
            Batch batch = repository.findById(id).orElseThrow(() -> new RuntimeException("Batch not found"));
            batch.setName(name);
            batch.setCourseId(courseId);
            batch.setStartDate(startDate);
            batch.setStatus(status);
            repository.save(batch);
            ra.addFlashAttribute("success", "Batch updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update batch: " + e.getMessage());
        }
        return "redirect:/admin/batches";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Batch deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete batch: " + e.getMessage());
        }
        return "redirect:/admin/batches";
    }
}
