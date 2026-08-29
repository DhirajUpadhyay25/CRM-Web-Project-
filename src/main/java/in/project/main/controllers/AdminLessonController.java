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
import in.project.main.entities.Lesson;
import in.project.main.repositories.LessonRepository;

@Controller
@RequestMapping("/admin/lessons")
public class AdminLessonController {

    @Autowired
    private LessonRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/learning/lessons/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String title,
                      @RequestParam String courseId,
                      @RequestParam String sectionName,
                      @RequestParam Integer orderIndex,
                      RedirectAttributes ra) {
        try {
            Lesson lesson = new Lesson();
            lesson.setTitle(title);
            lesson.setCourseId(courseId);
            lesson.setSectionName(sectionName);
            lesson.setOrderIndex(orderIndex);
            repository.save(lesson);
            ra.addFlashAttribute("success", "Lesson created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create lesson: " + e.getMessage());
        }
        return "redirect:/admin/lessons";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam String courseId,
                         @RequestParam String sectionName,
                         @RequestParam Integer orderIndex,
                         RedirectAttributes ra) {
        try {
            Lesson lesson = repository.findById(id).orElseThrow(() -> new RuntimeException("Lesson not found"));
            lesson.setTitle(title);
            lesson.setCourseId(courseId);
            lesson.setSectionName(sectionName);
            lesson.setOrderIndex(orderIndex);
            repository.save(lesson);
            ra.addFlashAttribute("success", "Lesson updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update lesson: " + e.getMessage());
        }
        return "redirect:/admin/lessons";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Lesson deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete lesson: " + e.getMessage());
        }
        return "redirect:/admin/lessons";
    }
}
