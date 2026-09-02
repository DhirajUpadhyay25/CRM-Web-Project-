package in.project.main.controllers;

import in.project.main.entities.*;
import in.project.main.entities.enums.NotificationType;
import in.project.main.repositories.*;
import in.project.main.services.LearningService;
import in.project.main.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin/assignments")
public class AdminAssignmentController {

    @Autowired
    private AssignmentSubmissionRepository submissionRepo;

    @Autowired
    private AssignmentRepository assignmentRepo;

    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private LearningService learningService;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private in.project.main.services.AuditLogService auditLogService;

    // 1. LIST SUBMISSIONS
    @GetMapping("/submissions")
    public String listSubmissions(
            @RequestParam(name = "status", required = false) String status,
            Model model) {

        List<AssignmentSubmission> submissions;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            submissions = submissionRepo.findByStatus(status.trim().toUpperCase());
        } else {
            submissions = submissionRepo.findAll();
        }

        submissions.sort((o1, o2) -> {
            if (o1.getSubmittedAt() == null || o2.getSubmittedAt() == null) return 0;
            return o2.getSubmittedAt().compareTo(o1.getSubmittedAt());
        });

        List<Map<String, Object>> displayList = new ArrayList<>();
        for (AssignmentSubmission s : submissions) {
            Assignment a = assignmentRepo.findById(s.getAssignmentId()).orElse(null);
            User student = userRepo.findByEmail(s.getUserEmail());
            Course course = a != null ? courseRepo.findById(a.getCourseId()).orElse(null) : null;

            Map<String, Object> map = new HashMap<>();
            map.put("submission", s);
            map.put("assignment", a);
            map.put("studentName", student != null ? student.getName() : s.getUserEmail());
            map.put("courseName", course != null ? course.getName() : "Unknown Course");
            displayList.add(map);
        }

        model.addAttribute("submissions", displayList);
        model.addAttribute("statusFilter", status);
        return "admin/learning/assignments/submissions";
    }

    // 2. VIEW SINGLE SUBMISSION FOR GRADING
    @GetMapping("/submissions/{id}")
    public String viewGradeForm(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        AssignmentSubmission submission = submissionRepo.findById(id).orElse(null);
        if (submission == null) {
            ra.addFlashAttribute("errorMsg", "Submission not found.");
            return "redirect:/admin/assignments/submissions";
        }

        Assignment a = assignmentRepo.findById(submission.getAssignmentId()).orElse(null);
        User student = userRepo.findByEmail(submission.getUserEmail());
        Course course = a != null ? courseRepo.findById(a.getCourseId()).orElse(null) : null;

        model.addAttribute("submission", submission);
        model.addAttribute("assignment", a);
        model.addAttribute("student", student);
        model.addAttribute("course", course);

        return "admin/learning/assignments/grade";
    }

    // 3. POST GRADE SUBMISSION
    @PostMapping("/submissions/{id}/grade")
    public String gradeSubmission(
            @PathVariable("id") Long id,
            @RequestParam("score") Integer score,
            @RequestParam("feedback") String feedback,
            java.security.Principal principal,
            RedirectAttributes ra) {

        AssignmentSubmission submission = submissionRepo.findById(id).orElse(null);
        if (submission == null) {
            ra.addFlashAttribute("errorMsg", "Submission not found.");
            return "redirect:/admin/assignments/submissions";
        }

        Assignment a = assignmentRepo.findById(submission.getAssignmentId()).orElse(null);
        if (a == null) {
            ra.addFlashAttribute("errorMsg", "Assignment definition not found.");
            return "redirect:/admin/assignments/submissions";
        }

        if (score < 0 || score > a.getMaxScore()) {
            ra.addFlashAttribute("errorMsg", "Score must be between 0 and " + a.getMaxScore() + ".");
            return "redirect:/admin/assignments/submissions/" + id;
        }

        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setStatus("GRADED");
        submissionRepo.save(submission);

        // Audit log in LearningService
        learningService.logActivity(submission.getUserEmail(), "ASSIGNMENT_GRADED",
                "Graded assignment: " + a.getTitle() + " (Score: " + score + "/" + a.getMaxScore() + ")");

        if (auditLogService != null) {
            String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
            in.project.main.events.PlatformAuditEvent audit = in.project.main.events.PlatformAuditEvent.of(
                actorEmail,
                in.project.main.entities.enums.AuditEventType.SETTINGS_CHANGED,
                "ASSIGNMENT_GRADED",
                "Graded student submission for assignment '" + a.getTitle() + "' (Student: " + submission.getUserEmail() + ", Score: " + score + "/" + a.getMaxScore() + ")."
            )
            .withActor(null, actorEmail, "Admin", "ADMIN")
            .withEntity("ASSIGNMENT_SUBMISSION", String.valueOf(id), a.getTitle())
            .withStatus(in.project.main.entities.enums.AuditStatus.SUCCESS)
            .withSeverity(in.project.main.entities.enums.AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        // Issue notification
        notificationService.createNotification(
                submission.getUserEmail(),
                NotificationType.ASSIGNMENT_GRADED,
                "Assignment Graded",
                "Your assignment '" + a.getTitle() + "' has been graded. Score: " + score + "/" + a.getMaxScore(),
                "/student/assignments/" + a.getId()
        );

        ra.addFlashAttribute("successMsg", "Submission graded successfully and student notified!");
        return "redirect:/admin/assignments/submissions";
    }
}
