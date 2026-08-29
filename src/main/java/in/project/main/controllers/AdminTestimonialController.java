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
import in.project.main.entities.Testimonial;
import in.project.main.repositories.TestimonialRepository;

@Controller
@RequestMapping("/admin/testimonials")
public class AdminTestimonialController {

    @Autowired
    private TestimonialRepository repository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", repository.findAll());
        return "admin/content/testimonials/list";
    }

    @PostMapping("/add")
    public String add(@RequestParam String studentName,
                      @RequestParam String courseName,
                      @RequestParam Integer rating,
                      @RequestParam String review,
                      @RequestParam Boolean isApproved,
                      RedirectAttributes ra) {
        try {
            Testimonial testimonial = new Testimonial();
            testimonial.setStudentName(studentName);
            testimonial.setCourseName(courseName);
            testimonial.setRating(rating);
            testimonial.setReview(review);
            testimonial.setIsApproved(isApproved);
            repository.save(testimonial);
            ra.addFlashAttribute("success", "Testimonial created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create testimonial: " + e.getMessage());
        }
        return "redirect:/admin/testimonials";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String studentName,
                         @RequestParam String courseName,
                         @RequestParam Integer rating,
                         @RequestParam String review,
                         @RequestParam Boolean isApproved,
                         RedirectAttributes ra) {
        try {
            Testimonial testimonial = repository.findById(id).orElseThrow(() -> new RuntimeException("Testimonial not found"));
            testimonial.setStudentName(studentName);
            testimonial.setCourseName(courseName);
            testimonial.setRating(rating);
            testimonial.setReview(review);
            testimonial.setIsApproved(isApproved);
            repository.save(testimonial);
            ra.addFlashAttribute("success", "Testimonial updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update testimonial: " + e.getMessage());
        }
        return "redirect:/admin/testimonials";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            repository.deleteById(id);
            ra.addFlashAttribute("success", "Testimonial deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete testimonial: " + e.getMessage());
        }
        return "redirect:/admin/testimonials";
    }
}
