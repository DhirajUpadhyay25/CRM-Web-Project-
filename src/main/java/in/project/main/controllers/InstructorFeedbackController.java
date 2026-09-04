package in.project.main.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Course;
import in.project.main.entities.Feedback;
import in.project.main.entities.FeedbackResponse;
import in.project.main.entities.Instructor;
import in.project.main.entities.enums.FeedbackStatus;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.InstructorRepository;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.FeedbackService;

@Controller
@RequestMapping("/instructor/feedback")
public class InstructorFeedbackController {

    @Autowired private FeedbackService feedbackService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private InstructorRepository instructorRepository;

    private Sort resolveSort(String sort) {
        if (sort == null || sort.trim().isEmpty() || "newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("rating_high".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "rating");
        } else if ("rating_low".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "rating");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    @GetMapping({"", "/", "/list"})
    public String listFeedback(
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) FeedbackStatus status,
            @RequestParam(name = "rating", required = false) Integer rating,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "courseId", required = false) Long courseId,
            @RequestParam(name = "sort", defaultValue = "newest") String sort) {

        String email = userDetails.getUsername();
        Instructor instructor = instructorRepository.findByEmail(email);

        Long instructorId = instructor != null ? instructor.getId() : null;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), resolveSort(sort));

        Page<Feedback> feedbackPage = feedbackService.instructorSearchAndFilter(
                instructorId, email, keyword, status, rating, category, courseId, pageable);

        List<Course> instructorCourses = courseRepository.findByInstructorEmail(email);

        model.addAttribute("feedbackPage", feedbackPage);
        model.addAttribute("instructorCourses", instructorCourses);
        model.addAttribute("feedbackStatuses", FeedbackStatus.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("rating", rating);
        model.addAttribute("category", category);
        model.addAttribute("courseId", courseId);
        model.addAttribute("sort", sort);

        return "instructor/feedback/list";
    }

    @GetMapping("/{id}")
    public String viewFeedback(
            @PathVariable("id") Long id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String email = userDetails.getUsername();
        Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);

        if (feedbackOpt.isEmpty()) {
            model.addAttribute("errorMsg", "Feedback not found.");
            return "redirect:/instructor/feedback";
        }

        Feedback feedback = feedbackOpt.get();

        // Verify instructor owns the course
        boolean authorized = false;
        if (feedback.getInstructor() != null && email.equalsIgnoreCase(feedback.getInstructor().getEmail())) {
            authorized = true;
        } else if (feedback.getCourse() != null && email.equalsIgnoreCase(feedback.getCourse().getInstructorEmail())) {
            authorized = true;
        }

        if (!authorized) {
            model.addAttribute("errorMsg", "You are not authorized to view this feedback.");
            return "redirect:/instructor/feedback";
        }

        List<FeedbackResponse> responses = feedbackService.getFeedbackResponses(id);

        model.addAttribute("feedback", feedback);
        model.addAttribute("responses", responses);

        return "instructor/feedback/detail";
    }

    @PostMapping("/{id}/respond")
    public String respondToFeedback(
            @PathVariable("id") Long id,
            @RequestParam("message") String message,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();

        try {
            feedbackService.respondToFeedback(id, email, userDetails.getName(),
                    userDetails.getRole().name(), message);
            redirectAttributes.addFlashAttribute("successMsg", "Response submitted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/instructor/feedback/" + id;
    }
}
