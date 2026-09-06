package in.project.main.controllers;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

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

    private boolean isAuthorizedInstructor(Feedback feedback, String email, Instructor instructor) {
        if (feedback == null || email == null) return false;

        // Check feedback instructor association
        if (feedback.getInstructor() != null) {
            if (feedback.getInstructor().getEmail() != null && email.equalsIgnoreCase(feedback.getInstructor().getEmail().trim())) {
                return true;
            }
            if (instructor != null && instructor.getId() != null && instructor.getId().equals(feedback.getInstructor().getId())) {
                return true;
            }
        }

        // Check feedback course association
        if (feedback.getCourse() != null) {
            Course course = feedback.getCourse();
            if (course.getInstructorEmail() != null && email.equalsIgnoreCase(course.getInstructorEmail().trim())) {
                return true;
            }
            if (course.getInstructorRef() != null) {
                if (course.getInstructorRef().getEmail() != null && email.equalsIgnoreCase(course.getInstructorRef().getEmail().trim())) {
                    return true;
                }
                if (instructor != null && instructor.getId() != null && instructor.getId().equals(course.getInstructorRef().getId())) {
                    return true;
                }
            }
        }

        // Fallback: Check if course belongs to instructor's courses
        List<Course> instructorCourses = courseRepository.findByInstructorEmail(email);
        if (instructorCourses != null && feedback.getCourse() != null) {
            for (Course c : instructorCourses) {
                if (c.getId().equals(feedback.getCourse().getId())) {
                    return true;
                }
            }
        }

        return false;
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

        if (userDetails == null) {
            return "redirect:/login";
        }

        String email = userDetails.getUsername();
        Instructor instructor = instructorRepository.findByEmail(email);
        Long instructorId = instructor != null ? instructor.getId() : null;

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), resolveSort(sort));

        Page<Feedback> feedbackPage = feedbackService.instructorSearchAndFilter(
                instructorId, email, keyword, status, rating, category, courseId, pageable);

        List<Course> instructorCourses = courseRepository.findByInstructorEmail(email);

        // Compute Instructor KPI Stats
        Page<Feedback> allInstructorFeedback = feedbackService.instructorSearchAndFilter(
                instructorId, email, null, null, null, null, null, PageRequest.of(0, 1000));
        
        long totalCount = allInstructorFeedback.getTotalElements();
        long respondedCount = 0;
        long pendingCount = 0;
        double sumRating = 0;
        int ratingCount = 0;

        for (Feedback fb : allInstructorFeedback.getContent()) {
            if (fb.getStatus() == FeedbackStatus.RESPONDED || fb.getStatus() == FeedbackStatus.RESOLVED || fb.getStatus() == FeedbackStatus.CLOSED) {
                respondedCount++;
            } else {
                pendingCount++;
            }
            if (fb.getRating() != null) {
                sumRating += fb.getRating();
                ratingCount++;
            }
        }
        double avgRating = ratingCount > 0 ? Math.round((sumRating / ratingCount) * 10.0) / 10.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("respondedCount", respondedCount);
        stats.put("pendingCount", pendingCount);
        stats.put("averageRating", avgRating);

        model.addAttribute("feedbackPage", feedbackPage);
        model.addAttribute("instructorCourses", instructorCourses);
        model.addAttribute("feedbackStatuses", FeedbackStatus.values());
        model.addAttribute("stats", stats);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("rating", rating);
        model.addAttribute("category", category);
        model.addAttribute("courseId", courseId);
        model.addAttribute("sort", sort);

        return "instructor/feedback/list";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getFeedbackApi(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        String email = userDetails.getUsername();
        Instructor instructor = instructorRepository.findByEmail(email);
        Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);

        if (feedbackOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Feedback not found"));
        }

        Feedback fb = feedbackOpt.get();
        if (!isAuthorizedInstructor(fb, email, instructor)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }

        List<FeedbackResponse> responses = feedbackService.getFeedbackResponses(id);

        Map<String, Object> data = new HashMap<>();
        data.put("id", fb.getId());
        data.put("studentName", fb.getDisplayName());
        data.put("isAnonymous", fb.isAnonymous());
        data.put("courseName", fb.getCourse() != null ? fb.getCourse().getName() : "General Feedback");
        data.put("rating", fb.getRating() != null ? fb.getRating() : 0);
        data.put("category", fb.getCategory() != null ? fb.getCategory() : "General");
        data.put("subject", fb.getSubject() != null ? fb.getSubject() : "");
        data.put("message", fb.getMessage() != null ? fb.getMessage() : "");
        data.put("status", fb.getStatus() != null ? fb.getStatus().name() : "NEW");
        data.put("statusDisplayName", fb.getStatus() != null ? fb.getStatus().getDisplayName() : "New");
        data.put("statusBadgeClass", fb.getStatus() != null ? fb.getStatus().getBadgeClass() : "bg-blue-100 text-blue-700");
        data.put("adminResponse", fb.getAdminResponse());
        data.put("createdAt", fb.getCreatedAt() != null ? fb.getCreatedAt().format(DATE_FORMATTER) : "");
        data.put("resolvedAt", fb.getResolvedAt() != null ? fb.getResolvedAt().format(DATE_FORMATTER) : null);

        List<Map<String, Object>> respList = new ArrayList<>();
        if (responses != null) {
            for (FeedbackResponse r : responses) {
                Map<String, Object> rMap = new HashMap<>();
                rMap.put("id", r.getId());
                rMap.put("responderName", r.getResponderName() != null ? r.getResponderName() : "EduTake Team");
                rMap.put("responderRole", r.getResponderRole() != null ? r.getResponderRole() : "INSTRUCTOR");
                rMap.put("responderEmail", r.getResponderEmail());
                rMap.put("message", r.getMessage());
                rMap.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FORMATTER) : "");
                respList.add(rMap);
            }
        }
        data.put("responses", respList);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}")
    public String viewFeedback(
            @PathVariable("id") Long id,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        String email = userDetails.getUsername();
        Instructor instructor = instructorRepository.findByEmail(email);
        Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);

        if (feedbackOpt.isEmpty()) {
            model.addAttribute("errorMsg", "Feedback not found.");
            return "redirect:/instructor/feedback";
        }

        Feedback feedback = feedbackOpt.get();

        if (!isAuthorizedInstructor(feedback, email, instructor)) {
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

        if (userDetails == null) {
            return "redirect:/login";
        }

        String email = userDetails.getUsername();
        Instructor instructor = instructorRepository.findByEmail(email);
        Optional<Feedback> feedbackOpt = feedbackService.getFeedbackById(id);

        if (feedbackOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Feedback not found.");
            return "redirect:/instructor/feedback";
        }

        Feedback feedback = feedbackOpt.get();
        if (!isAuthorizedInstructor(feedback, email, instructor)) {
            redirectAttributes.addFlashAttribute("errorMsg", "You are not authorized to respond to this feedback.");
            return "redirect:/instructor/feedback";
        }

        try {
            String responderName = (instructor != null && instructor.getName() != null) ? instructor.getName() : userDetails.getName();
            feedbackService.respondToFeedback(id, email, responderName, "INSTRUCTOR", message);
            redirectAttributes.addFlashAttribute("successMsg", "Response submitted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        return "redirect:/instructor/feedback/" + id;
    }
}
