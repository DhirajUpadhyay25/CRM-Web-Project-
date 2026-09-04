package in.project.main.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import in.project.main.entities.FeedbackResponse;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.FeedbackService;

@Controller
@RequestMapping("/student/feedback")
public class StudentFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    private Long getStudentId(CustomUserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()) != null
                ? userRepository.findByEmail(userDetails.getUsername()).getId() : null;
    }

    @Autowired
    private in.project.main.repositories.UserRepository userRepository;

    @GetMapping({"", "/", "/list"})
    public String listFeedback(
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Long studentId = getStudentId(userDetails);
        if (studentId == null) {
            return "redirect:/login";
        }

        Page<Feedback> feedbackPage = feedbackService.getStudentFeedback(
                studentId, PageRequest.of(Math.max(0, page), Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("feedbackPage", feedbackPage);
        return "student/feedback/list";
    }

    @GetMapping("/give")
    public String openGiveFeedbackForm(
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "courseId", required = false) Long courseId) {

        Long studentId = getStudentId(userDetails);
        if (studentId == null) {
            return "redirect:/login";
        }

        model.addAttribute("eligibleCourses", feedbackService.getStudentEligibleCourses(studentId));
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("feedbackTypes", in.project.main.entities.enums.FeedbackType.values());
        return "student/feedback/give";
    }

    @PostMapping("/submit")
    public String submitFeedback(
            @RequestParam("courseId") Long courseId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam("message") String message,
            @RequestParam(value = "isAnonymous", required = false) Boolean isAnonymous,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Long studentId = getStudentId(userDetails);
        if (studentId == null) {
            return "redirect:/login";
        }

        try {
            feedbackService.submitFeedback(studentId, courseId, null,
                    rating, category, subject, message, isAnonymous, isPublic);
            redirectAttributes.addFlashAttribute("successMsg", "Feedback submitted successfully! Thank you for your input.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/student/feedback/give?courseId=" + courseId;
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "You are not authorized to submit this feedback.");
            return "redirect:/student/feedback";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to submit feedback. Please try again.");
            return "redirect:/student/feedback/give?courseId=" + courseId;
        }

        return "redirect:/student/feedback";
    }

    @GetMapping("/{id}")
    public String viewFeedback(
            @PathVariable("id") Long id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long studentId = getStudentId(userDetails);
        if (studentId == null) {
            return "redirect:/login";
        }

        Optional<Feedback> feedbackOpt = feedbackService.getStudentFeedbackById(id, studentId);
        if (feedbackOpt.isEmpty()) {
            model.addAttribute("errorMsg", "Feedback not found or access denied.");
            return "redirect:/student/feedback";
        }

        Feedback feedback = feedbackOpt.get();
        List<FeedbackResponse> responses = feedbackService.getFeedbackResponses(id);

        model.addAttribute("feedback", feedback);
        model.addAttribute("responses", responses);
        return "student/feedback/detail";
    }

    @GetMapping("/{id}/edit")
    public String openEditForm(
            @PathVariable("id") Long id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Long studentId = getStudentId(userDetails);
        if (studentId == null) {
            return "redirect:/login";
        }

        Optional<Feedback> feedbackOpt = feedbackService.getStudentFeedbackById(id, studentId);
        if (feedbackOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Feedback not found or access denied.");
            return "redirect:/student/feedback";
        }

        Feedback feedback = feedbackOpt.get();
        if (feedback.getStatus() != null && !feedback.getStatus().isEditable()) {
            model.addAttribute("errorMsg", "Feedback can no longer be edited in its current status.");
            return "redirect:/student/feedback/" + id;
        }

        model.addAttribute("feedback", feedback);
        return "student/feedback/edit";
    }

    @PostMapping("/{id}/edit")
    public String editFeedback(
            @PathVariable("id") Long id,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "message", required = false) String message,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Long studentId = getStudentId(userDetails);
        if (studentId == null) {
            return "redirect:/login";
        }

        try {
            feedbackService.editFeedback(id, studentId, rating, category, subject, message);
            redirectAttributes.addFlashAttribute("successMsg", "Feedback updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/student/feedback/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteFeedback(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Long studentId = getStudentId(userDetails);
        if (studentId == null) {
            return "redirect:/login";
        }

        try {
            feedbackService.deleteFeedback(id, studentId);
            redirectAttributes.addFlashAttribute("successMsg", "Feedback withdrawn successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/student/feedback";
    }
}
