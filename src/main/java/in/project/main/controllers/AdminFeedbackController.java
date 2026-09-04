package in.project.main.controllers;

import java.time.LocalDateTime;
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

import in.project.main.entities.Feedback;
import in.project.main.entities.FeedbackNote;
import in.project.main.entities.FeedbackResponse;
import in.project.main.entities.FeedbackStatusHistory;
import in.project.main.entities.enums.FeedbackStatus;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.FeedbackService;
import in.project.main.services.CourseService;
import in.project.main.services.InstructorService;

@Controller
@RequestMapping("/admin/feedback")
public class AdminFeedbackController {

    @Autowired private FeedbackService feedbackService;
    @Autowired(required = false) private CourseService courseService;
    @Autowired(required = false) private InstructorService instructorService;

    private Sort resolveSort(String sort) {
        if (sort == null || sort.trim().isEmpty() || "newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("rating_high".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "rating");
        } else if ("rating_low".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "rating");
        } else if ("updated".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    @GetMapping({"", "/", "/list"})
    public String listFeedback(
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) FeedbackStatus status,
            @RequestParam(name = "rating", required = false) Integer rating,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "courseId", required = false) Long courseId,
            @RequestParam(name = "instructorId", required = false) Long instructorId,
            @RequestParam(name = "minRating", required = false) Integer minRating,
            @RequestParam(name = "sort", defaultValue = "newest") String sort) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), resolveSort(sort));
        Page<Feedback> feedbackPage = feedbackService.adminSearchAndFilter(
                keyword, status, rating, category, courseId, instructorId,
                minRating, null, null, pageable);

        Map<String, Object> analytics = feedbackService.getAnalytics();

        model.addAttribute("feedbackPage", feedbackPage);
        model.addAttribute("analytics", analytics);
        model.addAttribute("feedbackStatuses", FeedbackStatus.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("rating", rating);
        model.addAttribute("category", category);
        model.addAttribute("courseId", courseId);
        model.addAttribute("instructorId", instructorId);
        model.addAttribute("minRating", minRating);
        model.addAttribute("sort", sort);

        try {
            if (courseService != null) {
                model.addAttribute("courses", courseService.getAllCourseDetails());
            }
        } catch (Exception ignored) {}

        try {
            if (instructorService != null) {
                model.addAttribute("instructors", instructorService.getAllInstructors());
            }
        } catch (Exception ignored) {}

        return "admin/feedback/list";
    }

    @GetMapping("/{id}")
    public String viewFeedback(
            @PathVariable("id") Long id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);
        if (feedbackOpt.isEmpty()) {
            model.addAttribute("errorMsg", "Feedback not found.");
            return "redirect:/admin/feedback";
        }

        Feedback feedback = feedbackOpt.get();
        List<FeedbackResponse> responses = feedbackService.getFeedbackResponses(id);
        List<FeedbackNote> notes = feedbackService.getFeedbackNotes(id);
        List<FeedbackStatusHistory> history = feedbackService.getStatusHistory(id);

        model.addAttribute("feedback", feedback);
        model.addAttribute("responses", responses);
        model.addAttribute("notes", notes);
        model.addAttribute("history", history);
        model.addAttribute("feedbackStatuses", FeedbackStatus.values());

        return "admin/feedback/detail";
    }

    @PostMapping("/{id}/respond")
    public String respondToFeedback(
            @PathVariable("id") Long id,
            @RequestParam("message") String message,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            feedbackService.respondToFeedback(id, userDetails.getUsername(),
                    userDetails.getName(), userDetails.getRole().name(), message);
            redirectAttributes.addFlashAttribute("successMsg", "Response submitted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/admin/feedback/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") FeedbackStatus newStatus,
            @RequestParam(value = "reason", required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            feedbackService.updateFeedbackStatus(id, newStatus, userDetails.getUsername(), reason);
            redirectAttributes.addFlashAttribute("successMsg", "Status updated to " + newStatus.getDisplayName() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/admin/feedback/" + id;
    }

    @PostMapping("/{id}/assign")
    public String assignFeedback(
            @PathVariable("id") Long id,
            @RequestParam("adminEmail") String adminEmail,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            feedbackService.assignFeedback(id, null, adminEmail);
            redirectAttributes.addFlashAttribute("successMsg", "Feedback assigned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/admin/feedback/" + id;
    }

    @PostMapping("/{id}/note")
    public String addNote(
            @PathVariable("id") Long id,
            @RequestParam("note") String note,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            feedbackService.addInternalNote(id, userDetails.getUsername(), userDetails.getName(), note);
            redirectAttributes.addFlashAttribute("successMsg", "Note added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/admin/feedback/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteFeedback(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            feedbackService.updateFeedbackStatus(id, FeedbackStatus.ARCHIVED, userDetails.getUsername(), "Archived by admin");
            redirectAttributes.addFlashAttribute("successMsg", "Feedback archived.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/admin/feedback";
    }
}
