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
import in.project.main.repositories.UserRepository;

@Controller
@RequestMapping("/student/feedback")
public class StudentFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private UserRepository userRepository;

    private Long getStudentId(CustomUserDetails userDetails) {
        if (userDetails == null) return null;
        var user = userRepository.findByEmail(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    @GetMapping({"", "/", "/list"})
    public String listFeedback(
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Long studentId = getStudentId(userDetails);
        String studentEmail = userDetails != null ? userDetails.getUsername() : null;
        if (studentId == null && studentEmail == null) {
            return "redirect:/login";
        }

        Page<Feedback> feedbackPage = feedbackService.getStudentFeedback(
                studentId, studentEmail, PageRequest.of(Math.max(0, page), Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("feedbackPage", feedbackPage);
        model.addAttribute("stats", feedbackService.getStudentFeedbackStats(studentId, studentEmail));
        return "student/feedback/list";
    }

    @GetMapping("/api/{id}")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getFeedbackApi(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long studentId = getStudentId(userDetails);
        String studentEmail = userDetails != null ? userDetails.getUsername() : null;
        if (studentId == null && studentEmail == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Unauthorized"));
        }

        Optional<Feedback> feedbackOpt = feedbackService.getStudentFeedbackById(id, studentId, studentEmail);
        if (feedbackOpt.isEmpty()) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "Feedback not found"));
        }

        Feedback fb = feedbackOpt.get();
        List<FeedbackResponse> responses = feedbackService.getFeedbackResponses(id);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", fb.getId());
        data.put("courseName", fb.getCourse() != null ? fb.getCourse().getName() : "General");
        data.put("instructorName", fb.getInstructor() != null ? fb.getInstructor().getName() : (fb.getCourse() != null && fb.getCourse().getInstructorRef() != null ? fb.getCourse().getInstructorRef().getName() : "N/A"));
        data.put("rating", fb.getRating() != null ? fb.getRating() : 0);
        data.put("category", fb.getCategory() != null ? fb.getCategory() : "General");
        data.put("subject", fb.getSubject() != null ? fb.getSubject() : "");
        data.put("message", fb.getMessage() != null ? fb.getMessage() : "");
        data.put("status", fb.getStatus() != null ? fb.getStatus().name() : "NEW");
        data.put("statusDisplayName", fb.getStatus() != null ? fb.getStatus().getDisplayName() : "New");
        data.put("statusBadgeClass", fb.getStatus() != null ? fb.getStatus().getBadgeClass() : "bg-blue-100 text-blue-700");
        data.put("isEditable", fb.getStatus() != null && fb.getStatus().isEditable());
        data.put("isAnonymous", fb.isAnonymous());
        data.put("isPublic", fb.isPublic());
        data.put("adminResponse", fb.getAdminResponse());
        data.put("createdAt", fb.getCreatedAt() != null ? fb.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "");
        data.put("resolvedAt", fb.getResolvedAt() != null ? fb.getResolvedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : null);

        List<java.util.Map<String, Object>> respList = new java.util.ArrayList<>();
        if (responses != null) {
            for (FeedbackResponse r : responses) {
                java.util.Map<String, Object> rMap = new java.util.HashMap<>();
                rMap.put("id", r.getId());
                rMap.put("responderName", r.getResponderName() != null ? r.getResponderName() : "EduTake Team");
                rMap.put("responderRole", r.getResponderRole() != null ? r.getResponderRole() : "ADMIN");
                rMap.put("responderEmail", r.getResponderEmail());
                rMap.put("message", r.getMessage());
                rMap.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "");
                respList.add(rMap);
            }
        }
        data.put("responses", respList);

        return org.springframework.http.ResponseEntity.ok(data);
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
            @RequestParam(value = "courseId", required = false) String courseIdStr,
            @RequestParam(value = "rating", required = false) String ratingStr,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "isAnonymous", required = false) Boolean isAnonymous,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Long studentId = getStudentId(userDetails);
        if (studentId == null) {
            return "redirect:/login";
        }

        try {
            if (courseIdStr == null || courseIdStr.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMsg", "Please select a course.");
                return "redirect:/student/feedback/give";
            }
            Long courseId;
            try {
                courseId = Long.parseLong(courseIdStr.trim());
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Invalid course selection.");
                return "redirect:/student/feedback/give";
            }

            if (ratingStr == null || ratingStr.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMsg", "Please select a rating.");
                return "redirect:/student/feedback/give?courseId=" + courseId;
            }
            Integer rating;
            try {
                rating = Integer.parseInt(ratingStr.trim());
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("errorMsg", "Invalid rating value.");
                return "redirect:/student/feedback/give?courseId=" + courseId;
            }

            if (message == null || message.trim().length() < 10) {
                redirectAttributes.addFlashAttribute("errorMsg", "Feedback message must be at least 10 characters.");
                return "redirect:/student/feedback/give?courseId=" + courseId;
            }

            feedbackService.submitFeedback(studentId, courseId, null,
                    rating, category, subject, message.trim(), isAnonymous, isPublic);
            redirectAttributes.addFlashAttribute("successMsg", "Feedback submitted successfully! Thank you for your input.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/student/feedback/give";
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "You are not authorized to submit this feedback.");
            return "redirect:/student/feedback";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Unable to submit feedback. Please try again.");
            return "redirect:/student/feedback/give";
        }

        return "redirect:/student/feedback";
    }

    @GetMapping("/{id}")
    public String viewFeedback(
            @PathVariable("id") Long id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long studentId = getStudentId(userDetails);
        String studentEmail = userDetails != null ? userDetails.getUsername() : null;
        if (studentId == null && studentEmail == null) {
            return "redirect:/login";
        }

        Optional<Feedback> feedbackOpt = feedbackService.getStudentFeedbackById(id, studentId, studentEmail);
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
        String studentEmail = userDetails != null ? userDetails.getUsername() : null;
        if (studentId == null && studentEmail == null) {
            return "redirect:/login";
        }

        Optional<Feedback> feedbackOpt = feedbackService.getStudentFeedbackById(id, studentId, studentEmail);
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
            @RequestParam(value = "rating", required = false) String ratingStr,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "message", required = false) String message,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Long studentId = getStudentId(userDetails);
        String studentEmail = userDetails != null ? userDetails.getUsername() : null;
        if (studentId == null && studentEmail == null) {
            return "redirect:/login";
        }

        try {
            Integer rating = null;
            if (ratingStr != null && !ratingStr.trim().isEmpty()) {
                rating = Integer.parseInt(ratingStr.trim());
            }
            // Ensure studentId is available or load from feedback
            Optional<Feedback> fbOpt = feedbackService.getStudentFeedbackById(id, studentId, studentEmail);
            if (fbOpt.isEmpty()) {
                throw new SecurityException("You are not authorized to edit this feedback.");
            }
            Long effectiveStudentId = fbOpt.get().getStudent() != null ? fbOpt.get().getStudent().getId() : studentId;
            feedbackService.editFeedback(id, effectiveStudentId, rating, category, subject, message);
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
        String studentEmail = userDetails != null ? userDetails.getUsername() : null;
        if (studentId == null && studentEmail == null) {
            return "redirect:/login";
        }

        try {
            Optional<Feedback> fbOpt = feedbackService.getStudentFeedbackById(id, studentId, studentEmail);
            if (fbOpt.isEmpty()) {
                throw new SecurityException("You are not authorized to withdraw this feedback.");
            }
            Long effectiveStudentId = fbOpt.get().getStudent() != null ? fbOpt.get().getStudent().getId() : studentId;
            feedbackService.deleteFeedback(id, effectiveStudentId);
            redirectAttributes.addFlashAttribute("successMsg", "Feedback withdrawn successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/student/feedback";
    }
}
