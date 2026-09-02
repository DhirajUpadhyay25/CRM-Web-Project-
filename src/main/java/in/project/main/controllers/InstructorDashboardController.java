package in.project.main.controllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.dto.CourseDTO;
import in.project.main.entities.*;
import in.project.main.entities.enums.*;
import in.project.main.repositories.*;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.*;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/instructor")
public class InstructorDashboardController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private LearningService learningService;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    // Helper: Enforce ownership check
    private Course checkCourseOwnership(Long courseId, String email) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (!email.equalsIgnoreCase(course.getInstructorEmail())) {
            throw new SecurityException("Access Denied: You do not own this course");
        }
        return course;
    }

    private void populateDropdowns(Model model) {
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("levels", CourseLevel.values());
    }

    // ========================================================
    // 1. DASHBOARD
    // ========================================================
    @GetMapping({"/dashboard", ""})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        List<Course> courses = courseRepository.findByInstructorEmail(email);
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        long totalCourses = courses.size();
        long totalStudents = 0;
        long activeLearners = 0;
        long courseCompletions = 0;

        if (!courseIds.isEmpty()) {
            List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                    .filter(e -> courseIds.contains(e.getCourse().getId()))
                    .collect(Collectors.toList());
            totalStudents = enrollments.size();
            courseCompletions = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();
            activeLearners = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE).count();
        }

        // Count pending review submissions
        long pendingReviews = 0;
        if (!courseIds.isEmpty()) {
            List<Assignment> assignments = assignmentRepository.findByCourseIdIn(courseIds);
            List<Long> assignmentIds = assignments.stream().map(Assignment::getId).collect(Collectors.toList());
            if (!assignmentIds.isEmpty()) {
                pendingReviews = assignmentSubmissionRepository.findByStatus("SUBMITTED").stream()
                        .filter(s -> assignmentIds.contains(s.getAssignmentId()))
                        .count();
            }
        }

        model.addAttribute("totalCourses", totalCourses);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("activeLearners", activeLearners);
        model.addAttribute("courseCompletions", courseCompletions);
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("instructorName", userDetails.getName());

        return "instructor/dashboard";
    }

    // ========================================================
    // 2. MY COURSES
    // ========================================================
    @GetMapping("/courses")
    public String listCourses(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Course> courses = courseRepository.findByInstructorEmail(userDetails.getUsername());
        
        // Add additional stats per course
        List<Map<String, Object>> courseList = new ArrayList<>();
        for (Course c : courses) {
            Map<String, Object> map = new HashMap<>();
            map.put("course", c);
            long enrollCount = enrollmentRepository.countByCourseId(c.getId());
            long lessonCount = lessonRepository.findByCourseId(String.valueOf(c.getId())).size();
            map.put("studentCount", enrollCount);
            map.put("lessonCount", lessonCount);
            courseList.add(map);
        }
        
        model.addAttribute("courses", courseList);
        return "instructor/courses/list";
    }

    // ========================================================
    // 3. CREATE COURSE
    // ========================================================
    @GetMapping("/courses/new")
    public String openCreateCoursePage(Model model) {
        model.addAttribute("courseDTO", new CourseDTO());
        populateDropdowns(model);
        return "instructor/courses/add";
    }

    @PostMapping("/courses/new")
    public String createCourse(
            @Valid @ModelAttribute("courseDTO") CourseDTO courseDTO,
            BindingResult bindingResult,
            @RequestParam(value = "courseImg", required = false) MultipartFile courseImg,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateDropdowns(model);
            return "instructor/courses/add";
        }

        try {
            courseDTO.setStatus(CourseStatus.DRAFT);
            courseDTO.setInstructor(userDetails.getName());
            courseDTO.setInstructorEmail(userDetails.getUsername());

            Course created = courseService.createCourse(courseDTO, courseImg);
            redirectAttributes.addFlashAttribute("successMsg", "Course '" + created.getName() + "' created successfully!");
            return "redirect:/instructor/courses";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            populateDropdowns(model);
            return "instructor/courses/add";
        }
    }

    // ========================================================
    // 4. EDIT COURSE
    // ========================================================
    @GetMapping("/courses/{id}/edit")
    public String openEditCoursePage(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Course course = checkCourseOwnership(id, userDetails.getUsername());

        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setName(course.getName());
        dto.setShortDescription(course.getShortDescription());
        dto.setDescription(course.getDescription());
        dto.setInstructor(course.getInstructor());
        dto.setInstructorEmail(course.getInstructorEmail());
        dto.setLevel(course.getLevel());
        dto.setLanguage(course.getLanguage());
        dto.setDuration(course.getDuration());
        dto.setOriginalPrice(course.getOriginalPrice());
        dto.setDiscountedPrice(course.getDiscountedPrice());
        dto.setStatus(course.getStatus());
        dto.setFeatured(course.isFeatured());
        dto.setSeoTitle(course.getSeoTitle());
        dto.setSeoDescription(course.getSeoDescription());
        if (course.getCategory() != null) {
            dto.setCategoryId(course.getCategory().getId());
        }
        dto.setImageUrl(course.getImageUrl());

        model.addAttribute("courseDTO", dto);
        model.addAttribute("course", course);
        populateDropdowns(model);

        return "instructor/courses/edit";
    }

    @PostMapping("/courses/{id}/edit")
    public String editCourse(
            @PathVariable Long id,
            @Valid @ModelAttribute("courseDTO") CourseDTO courseDTO,
            BindingResult bindingResult,
            @RequestParam(value = "courseImg", required = false) MultipartFile courseImg,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        checkCourseOwnership(id, userDetails.getUsername());

        if (bindingResult.hasErrors()) {
            populateDropdowns(model);
            return "instructor/courses/edit";
        }

        try {
            courseDTO.setInstructor(userDetails.getName());
            courseDTO.setInstructorEmail(userDetails.getUsername());
            Course old = courseRepository.findById(id).orElseThrow();
            courseDTO.setStatus(old.getStatus());

            courseService.updateCourse(id, courseDTO, courseImg);
            redirectAttributes.addFlashAttribute("successMsg", "Course updated successfully!");
            return "redirect:/instructor/courses";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Failed to update course: " + e.getMessage());
            populateDropdowns(model);
            return "instructor/courses/edit";
        }
    }

    // ========================================================
    // 5. COURSE PREVIEW & PUBLISH
    // ========================================================
    @GetMapping("/courses/{id}/preview")
    public String previewCourse(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Course course = checkCourseOwnership(id, userDetails.getUsername());
        return "redirect:/courses/" + course.getSlug() + "?preview=true";
    }

    @PostMapping("/courses/{id}/publish")
    public String publishCourse(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes ra) {
        try {
            Course course = checkCourseOwnership(id, userDetails.getUsername());
            
            // Course Validation
            if (course.getName() == null || course.getName().trim().isEmpty()) {
                throw new IllegalStateException("Title is required.");
            }
            if (course.getShortDescription() == null || course.getShortDescription().trim().isEmpty()) {
                throw new IllegalStateException("Short description is required.");
            }
            if (course.getCategory() == null) {
                throw new IllegalStateException("Category is required.");
            }
            if (course.getOriginalPrice() == null) {
                throw new IllegalStateException("Original price is required.");
            }
            
            // Check curriculum existence
            List<Lesson> lessons = lessonRepository.findByCourseId(String.valueOf(course.getId()));
            if (lessons.isEmpty()) {
                throw new IllegalStateException("At least one lesson must be added to the course curriculum before publishing.");
            }

            course.setStatus(CourseStatus.PUBLISHED);
            courseRepository.save(course);

            if (auditLogService != null) {
                in.project.main.events.PlatformAuditEvent audit = in.project.main.events.PlatformAuditEvent.of(
                    userDetails.getUsername(),
                    in.project.main.entities.enums.AuditEventType.COURSE_PUBLISHED,
                    "COURSE_PUBLISHED",
                    "Instructor published course '" + course.getName() + "' (ID: " + id + ")."
                )
                .withActor(null, userDetails.getUsername(), userDetails.getName(), "INSTRUCTOR")
                .withEntity("COURSE", String.valueOf(id), course.getName())
                .withStatus(in.project.main.entities.enums.AuditStatus.SUCCESS)
                .withSeverity(in.project.main.entities.enums.AuditSeverity.INFO);

                auditLogService.record(audit);
            }
            
            ra.addFlashAttribute("successMsg", "Course '" + course.getName() + "' successfully published!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to publish: " + e.getMessage());
        }
        return "redirect:/instructor/courses";
    }

    // ========================================================
    // 6. COURSE CURRICULUM BUILDER
    // ========================================================
    @GetMapping("/courses/{id}/builder")
    public String openCourseBuilderPage(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Course course = checkCourseOwnership(id, userDetails.getUsername());
        
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(String.valueOf(id));
        Map<String, List<Lesson>> curriculum = new LinkedHashMap<>();
        for (Lesson lesson : lessons) {
            String sec = lesson.getSectionName() != null ? lesson.getSectionName() : "General";
            curriculum.computeIfAbsent(sec, k -> new ArrayList<>()).add(lesson);
        }

        List<Assignment> assignments = assignmentRepository.findByCourseId(id);
        List<Quiz> quizzes = quizRepository.findByCourseId(id);

        model.addAttribute("course", course);
        model.addAttribute("curriculum", curriculum);
        model.addAttribute("assignments", assignments);
        model.addAttribute("quizzes", quizzes);

        return "instructor/courses/builder";
    }

    @PostMapping("/courses/{id}/lessons/add")
    public String addLesson(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String sectionName,
            @RequestParam(defaultValue = "1") Integer orderIndex,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        Course course = checkCourseOwnership(id, userDetails.getUsername());
        
        Lesson lesson = new Lesson();
        lesson.setCourseId(String.valueOf(id));
        lesson.setTitle(title);
        lesson.setSectionName(sectionName);
        lesson.setOrderIndex(orderIndex);
        Lesson saved = lessonRepository.save(lesson);

        if (auditLogService != null) {
            in.project.main.events.PlatformAuditEvent audit = in.project.main.events.PlatformAuditEvent.of(
                userDetails.getUsername(),
                in.project.main.entities.enums.AuditEventType.LESSON_CREATED,
                "LESSON_CREATED",
                "Instructor added lesson '" + title + "' to course '" + course.getName() + "' (Section: " + sectionName + ")."
            )
            .withActor(null, userDetails.getUsername(), userDetails.getName(), "INSTRUCTOR")
            .withEntity("LESSON", String.valueOf(saved.getId()), title)
            .withStatus(in.project.main.entities.enums.AuditStatus.SUCCESS)
            .withSeverity(in.project.main.entities.enums.AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        ra.addFlashAttribute("successMsg", "Lesson added successfully!");
        return "redirect:/instructor/courses/" + id + "/builder";
    }

    @PostMapping("/courses/{id}/assignments/add")
    public String addAssignment(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String dueDate,
            @RequestParam int maxScore,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        Course course = checkCourseOwnership(id, userDetails.getUsername());

        Assignment assignment = new Assignment();
        assignment.setCourseId(id);
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setDueDate(LocalDateTime.parse(dueDate));
        assignment.setMaxScore(maxScore);
        Assignment saved = assignmentRepository.save(assignment);

        if (auditLogService != null) {
            in.project.main.events.PlatformAuditEvent audit = in.project.main.events.PlatformAuditEvent.of(
                userDetails.getUsername(),
                in.project.main.entities.enums.AuditEventType.SETTINGS_CHANGED,
                "ASSIGNMENT_CREATED",
                "Instructor created assignment '" + title + "' for course '" + course.getName() + "'."
            )
            .withActor(null, userDetails.getUsername(), userDetails.getName(), "INSTRUCTOR")
            .withEntity("ASSIGNMENT", String.valueOf(saved.getId()), title)
            .withStatus(in.project.main.entities.enums.AuditStatus.SUCCESS)
            .withSeverity(in.project.main.entities.enums.AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        ra.addFlashAttribute("successMsg", "Assignment created successfully!");
        return "redirect:/instructor/courses/" + id + "/builder";
    }

    @PostMapping("/courses/{id}/quizzes/add")
    public String addQuiz(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam int passingScore,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        Course course = checkCourseOwnership(id, userDetails.getUsername());

        Quiz quiz = new Quiz();
        quiz.setCourseId(id);
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setPassingScore(passingScore);
        Quiz saved = quizRepository.save(quiz);

        if (auditLogService != null) {
            in.project.main.events.PlatformAuditEvent audit = in.project.main.events.PlatformAuditEvent.of(
                userDetails.getUsername(),
                in.project.main.entities.enums.AuditEventType.QUIZ_CREATED,
                "QUIZ_CREATED",
                "Instructor created quiz '" + title + "' for course '" + course.getName() + "'."
            )
            .withActor(null, userDetails.getUsername(), userDetails.getName(), "INSTRUCTOR")
            .withEntity("QUIZ", String.valueOf(saved.getId()), title)
            .withStatus(in.project.main.entities.enums.AuditStatus.SUCCESS)
            .withSeverity(in.project.main.entities.enums.AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        ra.addFlashAttribute("successMsg", "Quiz created successfully!");
        return "redirect:/instructor/courses/" + id + "/builder";
    }

    // ========================================================
    // 7. QUIZ QUESTION MANAGEMENT
    // ========================================================
    @GetMapping("/quizzes/{quizId}/questions")
    public String viewQuizQuestions(@PathVariable Long quizId, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        checkCourseOwnership(quiz.getCourseId(), userDetails.getUsername());

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizId(quizId);
        
        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        return "instructor/courses/quiz-questions";
    }

    @PostMapping("/quizzes/{quizId}/questions/add")
    public String addQuizQuestion(
            @PathVariable Long quizId,
            @RequestParam String questionText,
            @RequestParam String optionA,
            @RequestParam String optionB,
            @RequestParam String optionC,
            @RequestParam String optionD,
            @RequestParam int correctOption,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        checkCourseOwnership(quiz.getCourseId(), userDetails.getUsername());

        QuizQuestion qq = new QuizQuestion();
        qq.setQuizId(quizId);
        qq.setQuestionText(questionText);
        qq.setOptionA(optionA);
        qq.setOptionB(optionB);
        qq.setOptionC(optionC);
        qq.setOptionD(optionD);
        qq.setCorrectOption(correctOption);
        quizQuestionRepository.save(qq);

        ra.addFlashAttribute("successMsg", "Question added successfully!");
        return "redirect:/instructor/quizzes/" + quizId + "/questions";
    }

    @PostMapping("/quizzes/{quizId}/questions/{questionId}/delete")
    public String deleteQuizQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        checkCourseOwnership(quiz.getCourseId(), userDetails.getUsername());

        quizQuestionRepository.deleteById(questionId);
        ra.addFlashAttribute("successMsg", "Question deleted successfully!");
        return "redirect:/instructor/quizzes/" + quizId + "/questions";
    }

    // ========================================================
    // 8. STUDENT PROGRESS TRACKING
    // ========================================================
    @GetMapping("/students")
    public String myStudents(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Course> courses = courseRepository.findByInstructorEmail(userDetails.getUsername());
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        List<Map<String, Object>> studentList = new ArrayList<>();
        if (!courseIds.isEmpty()) {
            List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                    .filter(e -> courseIds.contains(e.getCourse().getId()))
                    .collect(Collectors.toList());

            for (Enrollment e : enrollments) {
                Map<String, Object> map = new HashMap<>();
                map.put("enrollment", e);
                map.put("courseName", e.getCourse().getName());
                map.put("studentName", e.getUser().getName());
                map.put("studentEmail", e.getUser().getEmail());
                int progress = learningService.getCourseProgressPercent(e.getUser().getEmail(), e.getCourse().getId());
                map.put("progress", progress);
                studentList.add(map);
            }
        }

        model.addAttribute("students", studentList);
        return "instructor/students/list";
    }

    // ========================================================
    // 9. SUBMISSIONS REVIEW & GRADING
    // ========================================================
    @GetMapping("/submissions")
    public String listSubmissions(
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        List<Course> courses = courseRepository.findByInstructorEmail(userDetails.getUsername());
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        List<Map<String, Object>> displayList = new ArrayList<>();
        if (!courseIds.isEmpty()) {
            List<Assignment> assignments = assignmentRepository.findByCourseIdIn(courseIds);
            List<Long> assignmentIds = assignments.stream().map(Assignment::getId).collect(Collectors.toList());

            if (!assignmentIds.isEmpty()) {
                List<AssignmentSubmission> submissions;
                if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
                    submissions = assignmentSubmissionRepository.findByStatus(status);
                } else {
                    submissions = assignmentSubmissionRepository.findAll();
                }

                // Filter submissions belonging to instructor's assignments
                submissions = submissions.stream()
                        .filter(s -> assignmentIds.contains(s.getAssignmentId()))
                        .collect(Collectors.toList());

                // Sort by submittedAt desc
                submissions.sort((o1, o2) -> {
                    if (o1.getSubmittedAt() == null || o2.getSubmittedAt() == null) return 0;
                    return o2.getSubmittedAt().compareTo(o1.getSubmittedAt());
                });

                for (AssignmentSubmission s : submissions) {
                    Assignment a = assignmentRepository.findById(s.getAssignmentId()).orElse(null);
                    User student = userRepository.findByEmail(s.getUserEmail());
                    Course course = a != null ? courseRepository.findById(a.getCourseId()).orElse(null) : null;

                    Map<String, Object> map = new HashMap<>();
                    map.put("submission", s);
                    map.put("assignment", a);
                    map.put("studentName", student != null ? student.getName() : s.getUserEmail());
                    map.put("courseName", course != null ? course.getName() : "Unknown Course");
                    displayList.add(map);
                }
            }
        }

        model.addAttribute("submissions", displayList);
        model.addAttribute("statusFilter", status);
        return "instructor/assignments/submissions";
    }

    @GetMapping("/submissions/{id}")
    public String viewGradeForm(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model, RedirectAttributes ra) {
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(id).orElse(null);
        if (submission == null) {
            ra.addFlashAttribute("errorMsg", "Submission not found.");
            return "redirect:/instructor/submissions";
        }

        Assignment a = assignmentRepository.findById(submission.getAssignmentId()).orElse(null);
        if (a == null) {
            ra.addFlashAttribute("errorMsg", "Assignment definition not found.");
            return "redirect:/instructor/submissions";
        }

        // Ownership validation
        checkCourseOwnership(a.getCourseId(), userDetails.getUsername());

        User student = userRepository.findByEmail(submission.getUserEmail());
        Course course = courseRepository.findById(a.getCourseId()).orElse(null);

        model.addAttribute("submission", submission);
        model.addAttribute("assignment", a);
        model.addAttribute("student", student);
        model.addAttribute("course", course);

        return "instructor/assignments/grade";
    }

    @PostMapping("/submissions/{id}/grade")
    public String gradeSubmission(
            @PathVariable Long id,
            @RequestParam Integer score,
            @RequestParam String feedback,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        AssignmentSubmission submission = assignmentSubmissionRepository.findById(id).orElse(null);
        if (submission == null) {
            ra.addFlashAttribute("errorMsg", "Submission not found.");
            return "redirect:/instructor/submissions";
        }

        Assignment a = assignmentRepository.findById(submission.getAssignmentId()).orElse(null);
        if (a == null) {
            ra.addFlashAttribute("errorMsg", "Assignment definition not found.");
            return "redirect:/instructor/submissions";
        }

        // Ownership validation
        checkCourseOwnership(a.getCourseId(), userDetails.getUsername());

        if (score < 0 || score > a.getMaxScore()) {
            ra.addFlashAttribute("errorMsg", "Score must be between 0 and " + a.getMaxScore() + ".");
            return "redirect:/instructor/submissions/" + id;
        }

        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setStatus("GRADED");
        assignmentSubmissionRepository.save(submission);

        // Audit log in LearningService
        learningService.logActivity(submission.getUserEmail(), "ASSIGNMENT_GRADED",
                "Graded assignment: " + a.getTitle() + " (Score: " + score + "/" + a.getMaxScore() + ")");

        // Issue notification
        notificationService.createNotification(
                submission.getUserEmail(),
                NotificationType.ASSIGNMENT_GRADED,
                "Assignment Graded",
                "Your assignment '" + a.getTitle() + "' has been graded. Score: " + score + "/" + a.getMaxScore(),
                "/student/assignments/" + a.getId()
        );

        ra.addFlashAttribute("successMsg", "Submission graded successfully and student notified!");
        return "redirect:/instructor/submissions";
    }

    // ========================================================
    // 10. PROFILE MANAGEMENT
    // ========================================================
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Instructor instructor = instructorRepository.findByEmail(userDetails.getUsername());
        if (instructor == null) {
            instructor = new Instructor();
            instructor.setEmail(userDetails.getUsername());
            instructor.setName(userDetails.getName());
            instructorRepository.save(instructor);
        }
        model.addAttribute("instructor", instructor);
        return "instructor/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam String name,
            @RequestParam String bio,
            @RequestParam String specialization,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        Instructor instructor = instructorRepository.findByEmail(userDetails.getUsername());
        if (instructor == null) {
            instructor = new Instructor();
            instructor.setEmail(userDetails.getUsername());
        }
        instructor.setName(name);
        instructor.setBio(bio);
        instructor.setSpecialization(specialization);
        instructorRepository.save(instructor);

        // Update corresponding employee record
        Employee employee = employeeRepository.findByEmail(userDetails.getUsername());
        if (employee != null) {
            employee.setName(name);
            employeeRepository.save(employee);
        }

        ra.addFlashAttribute("successMsg", "Profile updated successfully!");
        return "redirect:/instructor/profile";
    }
}
