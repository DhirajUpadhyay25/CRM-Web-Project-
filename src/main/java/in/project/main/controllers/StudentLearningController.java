package in.project.main.controllers;

import in.project.main.entities.*;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.*;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.CourseService;
import in.project.main.services.LearningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/student")
public class StudentLearningController {

    @Autowired private LearningService learningService;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private CourseService courseService;
    @Autowired private CourseRepository courseRepo;
    @Autowired private LessonRepository lessonRepo;
    @Autowired private LessonProgressRepository progressRepo;
    @Autowired private QuizRepository quizRepo;
    @Autowired private QuizQuestionRepository questionRepo;
    @Autowired private QuizAttemptRepository attemptRepo;
    @Autowired private AssignmentRepository assignmentRepo;
    @Autowired private AssignmentSubmissionRepository submissionRepo;
    @Autowired private StudentActivityRepository activityRepo;
    @Autowired private UserRepository userRepo;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/upload/assignments/";

    private void addCommonStudentAttributes(CustomUserDetails userDetails, Model model) {
        User user = userRepo.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("studentName", user.getName());
        model.addAttribute("studentEmail", user.getEmail());
        model.addAttribute("studentImage", user.getImageName());
    }

    // ----------------------------------------------------
    // COURSE OVERVIEW
    // ----------------------------------------------------
    @GetMapping("/courses/{courseId}/overview")
    public String courseOverview(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes ra) {
        
        String email = userDetails.getUsername();
        addCommonStudentAttributes(userDetails, model);

        // Security Check: Is enrolled?
        Optional<Enrollment> enrollmentOpt = enrollmentRepo.findByUserEmailAndCourseId(email, courseId);
        if (enrollmentOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "You must be enrolled to access this course.");
            return "redirect:/student/courses";
        }

        Course course = enrollmentOpt.get().getCourse();
        List<Lesson> lessons = lessonRepo.findByCourseIdOrderByOrderIndexAsc(String.valueOf(courseId));
        Map<String, Object> progress = learningService.getCourseProgressDetails(email, courseId);

        model.addAttribute("enrollment", enrollmentOpt.get());
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("progress", progress);

        // Count total modules (unique sectionNames)
        Set<String> modules = new LinkedHashSet<>();
        for (Lesson l : lessons) {
            modules.add(l.getSectionName() != null ? l.getSectionName() : "General");
        }
        model.addAttribute("totalModules", modules.size());

        // Find the last accessed lesson ID for Continue Learning button target
        LessonProgress lastProg = progressRepo.findFirstByUserEmailAndCourseIdOrderByLastAccessedAtDesc(email, courseId);
        Long targetLessonId = null;
        if (lastProg != null) {
            targetLessonId = lastProg.getLessonId();
        } else if (!lessons.isEmpty()) {
            targetLessonId = lessons.get(0).getId();
        }
        model.addAttribute("targetLessonId", targetLessonId);

        return "student/course-overview";
    }

    // ----------------------------------------------------
    // COURSE PLAYER / LESSON VIEWER
    // ----------------------------------------------------
    @GetMapping("/courses/{courseId}/player")
    public String coursePlayer(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long lessonId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes ra) {

        String email = userDetails.getUsername();
        addCommonStudentAttributes(userDetails, model);

        // Security Check: Enrollment bypass
        Optional<Enrollment> enrollmentOpt = enrollmentRepo.findByUserEmailAndCourseId(email, courseId);
        if (enrollmentOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "You are not enrolled in this course.");
            return "redirect:/student/courses";
        }

        List<Lesson> lessons = lessonRepo.findByCourseIdOrderByOrderIndexAsc(String.valueOf(courseId));
        if (lessons.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "This course does not have any lessons yet.");
            return "redirect:/student/courses/" + courseId + "/overview";
        }

        // Continue Learning Engine redirection if no lessonId provided
        if (lessonId == null) {
            LessonProgress lastProg = progressRepo.findFirstByUserEmailAndCourseIdOrderByLastAccessedAtDesc(email, courseId);
            Long nextLessonId = (lastProg != null) ? lastProg.getLessonId() : lessons.get(0).getId();
            return "redirect:/student/courses/" + courseId + "/player?lessonId=" + nextLessonId;
        }

        // Verify lesson belongs to this course
        Lesson currentLesson = lessonRepo.findById(lessonId)
                .filter(l -> l.getCourseId().equals(String.valueOf(courseId)))
                .orElse(null);
        if (currentLesson == null) {
            ra.addFlashAttribute("errorMsg", "Invalid lesson reference.");
            return "redirect:/student/courses/" + courseId + "/overview";
        }

        // Security Check: Lesson locking bypass (sequential check)
        if (learningService.isLessonLocked(email, courseId, lessonId)) {
            ra.addFlashAttribute("errorMsg", "This lesson is locked. Please complete previous lessons first.");
            // Find the last completed lesson or fall back to first lesson
            return "redirect:/student/courses/" + courseId + "/player";
        }

        // Record lesson access
        learningService.recordLessonAccess(email, courseId, lessonId);

        // Get completed status for all lessons
        List<LessonProgress> completedProg = progressRepo.findByUserEmailAndCourseId(email, courseId);
        Set<Long> completedLessonIds = new HashSet<>();
        for (LessonProgress lp : completedProg) {
            if (lp.isCompleted()) {
                completedLessonIds.add(lp.getLessonId());
            }
        }

        // Group syllabus structure for player sidebar
        Map<String, List<Map<String, Object>>> syllabus = new LinkedHashMap<>();
        for (Lesson l : lessons) {
            String section = l.getSectionName() != null ? l.getSectionName() : "General";
            Map<String, Object> lMap = new HashMap<>();
            lMap.put("id", l.getId());
            lMap.put("title", l.getTitle());
            lMap.put("orderIndex", l.getOrderIndex());
            lMap.put("completed", completedLessonIds.contains(l.getId()));
            lMap.put("locked", learningService.isLessonLocked(email, courseId, l.getId()));
            lMap.put("active", l.getId().equals(lessonId));
            
            syllabus.computeIfAbsent(section, k -> new ArrayList<>()).add(lMap);
        }

        // Navigation controls (Prev/Next)
        Lesson prevLesson = null;
        Lesson nextLesson = null;
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getId().equals(lessonId)) {
                if (i > 0) prevLesson = lessons.get(i - 1);
                if (i < lessons.size() - 1) nextLesson = lessons.get(i + 1);
                break;
            }
        }

        // Course completion checklist (Passed quizzes & assignments check)
        List<Quiz> quizzes = quizRepo.findByCourseId(courseId);
        List<Assignment> assignments = assignmentRepo.findByCourseId(courseId);
        
        List<Map<String, Object>> quizChecks = new ArrayList<>();
        for (Quiz q : quizzes) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("title", q.getTitle());
            List<QuizAttempt> attempts = attemptRepo.findByUserEmailAndQuizIdOrderByAttemptedAtDesc(email, q.getId());
            boolean passed = attempts.stream().anyMatch(QuizAttempt::isPassed);
            map.put("passed", passed);
            map.put("score", attempts.isEmpty() ? null : attempts.get(0).getScore());
            quizChecks.add(map);
        }

        Map<String, Object> progressStats = learningService.getCourseProgressDetails(email, courseId);

        model.addAttribute("enrollment", enrollmentOpt.get());
        model.addAttribute("course", enrollmentOpt.get().getCourse());
        model.addAttribute("lesson", currentLesson);
        model.addAttribute("syllabus", syllabus);
        model.addAttribute("prevLesson", prevLesson);
        model.addAttribute("nextLesson", nextLesson);
        model.addAttribute("quizChecks", quizChecks);
        model.addAttribute("assignments", assignments);
        model.addAttribute("progressStats", progressStats);

        return "student/course-player";
    }

    // ----------------------------------------------------
    // LESSON COMPLETE ACTION
    // ----------------------------------------------------
    @PostMapping("/courses/{courseId}/lessons/{lessonId}/complete")
    public String completeLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        String email = userDetails.getUsername();

        // Security Check: Enrolled?
        Optional<Enrollment> enrollmentOpt = enrollmentRepo.findByUserEmailAndCourseId(email, courseId);
        if (enrollmentOpt.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Not enrolled.");
            return "redirect:/student/courses";
        }

        // Complete lesson
        learningService.completeLesson(email, courseId, lessonId);

        // Find next lesson to redirect to
        List<Lesson> lessons = lessonRepo.findByCourseIdOrderByOrderIndexAsc(String.valueOf(courseId));
        Long nextLessonId = null;
        for (int i = 0; i < lessons.size() - 1; i++) {
            if (lessons.get(i).getId().equals(lessonId)) {
                nextLessonId = lessons.get(i + 1).getId();
                break;
            }
        }

        if (nextLessonId != null) {
            ra.addFlashAttribute("successMsg", "Lesson completed! Loading next lesson...");
            return "redirect:/student/courses/" + courseId + "/player?lessonId=" + nextLessonId;
        } else {
            ra.addFlashAttribute("successMsg", "Congratulations! You have completed all lessons in this course!");
            return "redirect:/student/courses/" + courseId + "/player?lessonId=" + lessonId;
        }
    }

    // ----------------------------------------------------
    // QUIZZES
    // ----------------------------------------------------
    @GetMapping("/courses/{courseId}/quizzes/{quizId}")
    public String quizView(
            @PathVariable Long courseId,
            @PathVariable Long quizId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes ra) {

        String email = userDetails.getUsername();
        addCommonStudentAttributes(userDetails, model);

        // Enrolled check
        if (!enrollmentRepo.findByUserEmailAndCourseId(email, courseId).isPresent()) {
            ra.addFlashAttribute("errorMsg", "Access Denied.");
            return "redirect:/student/courses";
        }

        Quiz quiz = quizRepo.findById(quizId)
                .filter(q -> q.getCourseId().equals(courseId))
                .orElse(null);
        if (quiz == null) {
            ra.addFlashAttribute("errorMsg", "Quiz not found.");
            return "redirect:/student/courses/" + courseId + "/player";
        }

        List<QuizQuestion> questions = questionRepo.findByQuizId(quizId);
        List<QuizAttempt> attempts = attemptRepo.findByUserEmailAndQuizIdOrderByAttemptedAtDesc(email, quizId);

        model.addAttribute("quiz", quiz);
        model.addAttribute("courseId", courseId);
        model.addAttribute("questions", questions);
        model.addAttribute("attempts", attempts);

        return "student/quiz-view";
    }

    @PostMapping("/courses/{courseId}/quizzes/{quizId}/submit")
    public String submitQuiz(
            @PathVariable Long courseId,
            @PathVariable Long quizId,
            @RequestParam Map<String, String> allParams,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes ra) {

        String email = userDetails.getUsername();
        addCommonStudentAttributes(userDetails, model);

        // Enrolled check
        if (!enrollmentRepo.findByUserEmailAndCourseId(email, courseId).isPresent()) {
            ra.addFlashAttribute("errorMsg", "Access Denied.");
            return "redirect:/student/courses";
        }

        Quiz quiz = quizRepo.findById(quizId).orElse(null);
        if (quiz == null) {
            ra.addFlashAttribute("errorMsg", "Quiz not found.");
            return "redirect:/student/courses/" + courseId + "/player";
        }

        List<QuizQuestion> questions = questionRepo.findByQuizId(quizId);
        Map<Long, Integer> answers = new HashMap<>();

        for (QuizQuestion q : questions) {
            String ansKey = "question_" + q.getId();
            if (allParams.containsKey(ansKey)) {
                try {
                    answers.put(q.getId(), Integer.parseInt(allParams.get(ansKey)));
                } catch (NumberFormatException ignored) {}
            }
        }

        QuizAttempt attempt = learningService.submitQuiz(email, quizId, answers);

        model.addAttribute("quiz", quiz);
        model.addAttribute("courseId", courseId);
        model.addAttribute("attempt", attempt);
        model.addAttribute("questions", questions);
        model.addAttribute("studentAnswers", answers);

        return "student/quiz-result";
    }

    // ----------------------------------------------------
    // ASSIGNMENTS
    // ----------------------------------------------------
    @GetMapping("/assignments")
    public String listAssignments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        String email = userDetails.getUsername();
        addCommonStudentAttributes(userDetails, model);

        // Find enrolled course IDs
        List<Enrollment> enrollments = enrollmentRepo.findByUserEmailOrderByEnrolledAtDesc(email);
        List<Long> courseIds = enrollments.stream().map(e -> e.getCourse().getId()).toList();

        List<Map<String, Object>> assignmentList = new ArrayList<>();
        if (!courseIds.isEmpty()) {
            List<Assignment> assignments = assignmentRepo.findByCourseIdIn(courseIds);
            for (Assignment a : assignments) {
                Course c = courseRepo.findById(a.getCourseId()).orElse(null);
                Optional<AssignmentSubmission> sub = submissionRepo.findByUserEmailAndAssignmentId(email, a.getId());

                Map<String, Object> map = new HashMap<>();
                map.put("assignment", a);
                map.put("courseName", c != null ? c.getName() : "Course");
                map.put("status", sub.isPresent() ? sub.get().getStatus() : (LocalDateTime.now().isAfter(a.getDueDate()) ? "OVERDUE" : "NOT_STARTED"));
                map.put("submission", sub.orElse(null));
                assignmentList.add(map);
            }
        }

        model.addAttribute("assignments", assignmentList);
        return "student/assignments";
    }

    @GetMapping("/assignments/{assignmentId}")
    public String viewAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes ra) {

        String email = userDetails.getUsername();
        addCommonStudentAttributes(userDetails, model);

        Assignment assignment = assignmentRepo.findById(assignmentId).orElse(null);
        if (assignment == null) {
            ra.addFlashAttribute("errorMsg", "Assignment not found.");
            return "redirect:/student/assignments";
        }

        // Security check: Enrolled in course?
        if (!enrollmentRepo.findByUserEmailAndCourseId(email, assignment.getCourseId()).isPresent()) {
            ra.addFlashAttribute("errorMsg", "Access Denied.");
            return "redirect:/student/assignments";
        }

        Course c = courseRepo.findById(assignment.getCourseId()).orElse(null);
        Optional<AssignmentSubmission> sub = submissionRepo.findByUserEmailAndAssignmentId(email, assignmentId);

        model.addAttribute("assignment", assignment);
        model.addAttribute("course", c);
        model.addAttribute("submission", sub.orElse(null));
        model.addAttribute("status", sub.isPresent() ? sub.get().getStatus() : (LocalDateTime.now().isAfter(assignment.getDueDate()) ? "OVERDUE" : "NOT_STARTED"));

        return "student/assignment-detail";
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    public String submitAssignmentForm(
            @PathVariable Long assignmentId,
            @RequestParam("submissionText") String submissionText,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        String email = userDetails.getUsername();

        Assignment assignment = assignmentRepo.findById(assignmentId).orElse(null);
        if (assignment == null) {
            ra.addFlashAttribute("errorMsg", "Assignment not found.");
            return "redirect:/student/assignments";
        }

        // Security Check: Enrolled?
        if (!enrollmentRepo.findByUserEmailAndCourseId(email, assignment.getCourseId()).isPresent()) {
            ra.addFlashAttribute("errorMsg", "Access Denied.");
            return "redirect:/student/assignments";
        }

        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            // File size validation (5MB max)
            if (file.getSize() > 5 * 1024 * 1024) {
                ra.addFlashAttribute("errorMsg", "File size exceeds the 5MB limit.");
                return "redirect:/student/assignments/" + assignmentId;
            }

            // File type validation
            String origName = file.getOriginalFilename();
            String ext = "";
            if (origName != null && origName.lastIndexOf('.') > 0) {
                ext = origName.substring(origName.lastIndexOf('.')).toLowerCase();
            }
            List<String> allowedExts = Arrays.asList(".pdf", ".zip", ".png", ".jpg", ".jpeg", ".doc", ".docx");
            if (!allowedExts.contains(ext)) {
                ra.addFlashAttribute("errorMsg", "File type not supported. Please upload PDF, ZIP, DOC, or Image files.");
                return "redirect:/student/assignments/" + assignmentId;
            }

            // Save File securely
            try {
                String fileName = System.currentTimeMillis() + "_" + studentSafeFilename(origName);
                File uploadPath = new File(UPLOAD_DIR);
                if (!uploadPath.exists()) {
                    uploadPath.mkdirs();
                }
                File dest = new File(UPLOAD_DIR + fileName);
                file.transferTo(dest);
                fileUrl = "/upload/assignments/" + fileName;
            } catch (IOException e) {
                ra.addFlashAttribute("errorMsg", "Failed to upload file. Please try again.");
                return "redirect:/student/assignments/" + assignmentId;
            }
        }

        learningService.submitAssignment(email, assignmentId, submissionText, fileUrl);
        ra.addFlashAttribute("successMsg", "Assignment submitted successfully!");
        return "redirect:/student/assignments/" + assignmentId;
    }

    private String studentSafeFilename(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    // ----------------------------------------------------
    // LEARNING CALENDAR
    // ----------------------------------------------------
    @GetMapping("/calendar")
    public String calendar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        String email = userDetails.getUsername();
        addCommonStudentAttributes(userDetails, model);

        List<Enrollment> enrollments = enrollmentRepo.findByUserEmailOrderByEnrolledAtDesc(email);
        List<Long> courseIds = enrollments.stream().map(e -> e.getCourse().getId()).toList();

        List<Map<String, Object>> events = new ArrayList<>();
        if (!courseIds.isEmpty()) {
            List<Assignment> assignments = assignmentRepo.findByCourseIdIn(courseIds);
            for (Assignment a : assignments) {
                Course c = courseRepo.findById(a.getCourseId()).orElse(null);
                Map<String, Object> evt = new HashMap<>();
                evt.put("title", "Deadline: " + a.getTitle());
                evt.put("date", a.getDueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                evt.put("type", "ASSIGNMENT");
                evt.put("courseName", c != null ? c.getName() : "Course");
                evt.put("url", "/student/assignments/" + a.getId());
                events.add(evt);
            }
            
            // Fetch Quizzes as calendar items too
            for (Long cId : courseIds) {
                Course c = courseRepo.findById(cId).orElse(null);
                List<Quiz> quizzes = quizRepo.findByCourseId(cId);
                for (Quiz q : quizzes) {
                    Map<String, Object> evt = new HashMap<>();
                    evt.put("title", "Quiz: " + q.getTitle());
                    evt.put("date", LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 10:00");
                    evt.put("type", "QUIZ");
                    evt.put("courseName", c != null ? c.getName() : "Course");
                    evt.put("url", "/student/courses/" + cId + "/quizzes/" + q.getId());
                    events.add(evt);
                }
            }
        }

        model.addAttribute("events", events);
        return "student/calendar";
    }

    // ----------------------------------------------------
    // ACTIVITY TIMELINE
    // ----------------------------------------------------
    @GetMapping("/activity")
    public String activityTimeline(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        String email = userDetails.getUsername();
        addCommonStudentAttributes(userDetails, model);

        List<StudentActivity> activities = activityRepo.findByUserEmailOrderByCreatedAtDesc(email);
        
        List<Map<String, Object>> formattedActs = new ArrayList<>();
        for (StudentActivity sa : activities) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", sa.getActivityType());
            map.put("description", sa.getDescription());
            map.put("createdAt", sa.getCreatedAt());
            map.put("timeStr", sa.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            formattedActs.add(map);
        }

        model.addAttribute("activities", formattedActs);
        return "student/activity";
    }
}
