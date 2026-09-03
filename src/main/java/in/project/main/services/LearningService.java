package in.project.main.services;

import in.project.main.entities.*;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.NotificationType;
import in.project.main.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LearningService {

    @Autowired private LessonProgressRepository progressRepo;
    @Autowired private LessonRepository lessonRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private CertificateRepository certificateRepo;
    @Autowired private QuizRepository quizRepo;
    @Autowired private QuizQuestionRepository questionRepo;
    @Autowired private QuizAttemptRepository attemptRepo;
    @Autowired private AssignmentRepository assignmentRepo;
    @Autowired private AssignmentSubmissionRepository submissionRepo;
    @Autowired private StudentActivityRepository activityRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;

    public boolean checkCourseAccess(String email, Long courseId) {
        User user = userRepo.findByEmail(email);
        if (user == null || user.isBanStatus()) {
            return false;
        }
        Optional<Course> courseOpt = courseRepo.findById(courseId);
        if (courseOpt.isEmpty()) {
            return false;
        }
        Optional<Enrollment> enrollmentOpt = enrollmentRepo.findByUserEmailAndCourseId(email, courseId);
        if (enrollmentOpt.isEmpty()) {
            return false;
        }
        Enrollment enrollment = enrollmentOpt.get();
        return enrollment.canAccess();
    }

    @Transactional
    public void recordLessonAccess(String email, Long courseId, Long lessonId) {
        LessonProgress progress = progressRepo.findByUserEmailAndLessonId(email, lessonId)
                .orElseGet(() -> {
                    LessonProgress newProg = new LessonProgress();
                    newProg.setUserEmail(email);
                    newProg.setCourseId(courseId);
                    newProg.setLessonId(lessonId);
                    newProg.setCompleted(false);
                    return newProg;
                });
        progress.setLastAccessedAt(LocalDateTime.now());
        progressRepo.save(progress);

        // Update enrollment last accessed timestamp
        enrollmentRepo.findByUserEmailAndCourseId(email, courseId).ifPresent(e -> {
            e.setLastAccessedAt(LocalDateTime.now());
            enrollmentRepo.save(e);
        });
    }

    @Transactional
    public void completeLesson(String email, Long courseId, Long lessonId) {
        LessonProgress progress = progressRepo.findByUserEmailAndLessonId(email, lessonId)
                .orElseGet(() -> {
                    LessonProgress newProg = new LessonProgress();
                    newProg.setUserEmail(email);
                    newProg.setCourseId(courseId);
                    newProg.setLessonId(lessonId);
                    return newProg;
                });
        
        boolean wasCompleted = progress.isCompleted();
        progress.setCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastAccessedAt(LocalDateTime.now());
        progressRepo.save(progress);

        if (!wasCompleted) {
            // Log Activity
            Lesson lesson = lessonRepo.findById(lessonId).orElse(null);
            String title = lesson != null ? lesson.getTitle() : "Lesson " + lessonId;
            logActivity(email, "LESSON_COMPLETE", "Completed lesson: " + title);

            // Re-evaluate Course Completion
            checkAndUpdateCourseCompletion(email, courseId);
        }
    }

    public int getCourseProgressPercent(String email, Long courseId) {
        List<Lesson> lessons = lessonRepo.findByCourseId(String.valueOf(courseId));
        if (lessons.isEmpty()) return 0;

        long completed = progressRepo.countByUserEmailAndCourseIdAndCompleted(email, courseId, true);
        return (int) ((completed * 100) / lessons.size());
    }

    public Map<String, Object> getCourseProgressDetails(String email, Long courseId) {
        List<Lesson> lessons = lessonRepo.findByCourseId(String.valueOf(courseId));
        long completedLessons = progressRepo.countByUserEmailAndCourseIdAndCompleted(email, courseId, true);
        
        Set<String> totalModules = new HashSet<>();
        Set<String> completedModules = new HashSet<>();

        // Group lessons by module (sectionName)
        Map<String, List<Lesson>> moduleLessons = new HashMap<>();
        for (Lesson l : lessons) {
            String sec = l.getSectionName() != null ? l.getSectionName() : "General";
            totalModules.add(sec);
            moduleLessons.computeIfAbsent(sec, k -> new ArrayList<>()).add(l);
        }

        // Check completion of each module
        for (Map.Entry<String, List<Lesson>> entry : moduleLessons.entrySet()) {
            boolean moduleComplete = true;
            for (Lesson l : entry.getValue()) {
                Optional<LessonProgress> p = progressRepo.findByUserEmailAndLessonId(email, l.getId());
                if (p.isEmpty() || !p.get().isCompleted()) {
                    moduleComplete = false;
                    break;
                }
            }
            if (moduleComplete) {
                completedModules.add(entry.getKey());
            }
        }

        int percent = lessons.isEmpty() ? 0 : (int) ((completedLessons * 100) / lessons.size());

        Map<String, Object> details = new HashMap<>();
        details.put("percent", percent);
        details.put("completedLessons", completedLessons);
        details.put("totalLessons", lessons.size());
        details.put("completedModules", completedModules.size());
        details.put("totalModules", totalModules.size());
        return details;
    }

    public boolean isLessonLocked(String email, Long courseId, Long lessonId) {
        List<Lesson> lessons = lessonRepo.findByCourseIdOrderByOrderIndexAsc(String.valueOf(courseId));
        if (lessons.isEmpty()) return false;

        // First lesson is always unlocked
        if (lessons.get(0).getId().equals(lessonId)) {
            return false;
        }

        // Find current lesson index
        int idx = -1;
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getId().equals(lessonId)) {
                idx = i;
                break;
            }
        }

        if (idx <= 0) return false;

        // Locked if the previous lesson is not completed
        Lesson previousLesson = lessons.get(idx - 1);
        Optional<LessonProgress> prevProg = progressRepo.findByUserEmailAndLessonId(email, previousLesson.getId());
        return prevProg.isEmpty() || !prevProg.get().isCompleted();
    }

    @Transactional
    public void logActivity(String email, String type, String description) {
        StudentActivity act = new StudentActivity();
        act.setUserEmail(email);
        act.setActivityType(type);
        act.setDescription(description);
        activityRepo.save(act);
    }

    @Transactional
    public void checkAndUpdateCourseCompletion(String email, Long courseId) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepo.findByUserEmailAndCourseId(email, courseId);
        if (enrollmentOpt.isEmpty()) return;

        Enrollment enrollment = enrollmentOpt.get();
        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) return;

        int percent = getCourseProgressPercent(email, courseId);
        
        // Let's check quiz passing as well.
        // If the course has quizzes, we require passing attempts for all course quizzes to consider it complete
        List<Quiz> quizzes = quizRepo.findByCourseId(courseId);
        boolean quizzesPassed = true;
        for (Quiz q : quizzes) {
            long passedAttempts = attemptRepo.countByUserEmailAndQuizIdAndPassed(email, q.getId(), true);
            if (passedAttempts == 0) {
                quizzesPassed = false;
                break;
            }
        }

        if (percent >= 100 && quizzesPassed) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
            enrollmentRepo.save(enrollment);

            logActivity(email, "COURSE_COMPLETE", "Successfully completed the course: " + enrollment.getCourse().getName());

            // Notify Student of Certificate Eligibility
            Notification notif = new Notification();
            notif.setRecipientEmail(email);
            notif.setType(NotificationType.CERTIFICATE_ELIGIBLE);
            notif.setTitle("Certificate Eligible!");
            notif.setMessage("Congratulations! You completed " + enrollment.getCourse().getName() + " and are now eligible to claim your official certificate.");
            notif.setTargetUrl("/student/certificates");
            notificationRepo.save(notif);
        }
    }

    @Transactional
    public QuizAttempt submitQuiz(String email, Long quizId, Map<Long, Integer> answers) {
        Quiz quiz = quizRepo.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz not found"));
        List<QuizQuestion> questions = questionRepo.findByQuizId(quizId);

        if (questions.isEmpty()) {
            throw new RuntimeException("Quiz has no questions");
        }

        int correctCount = 0;
        for (QuizQuestion q : questions) {
            Integer studentAns = answers.get(q.getId());
            if (studentAns != null && studentAns.equals(q.getCorrectOption())) {
                correctCount++;
            }
        }

        int scorePercent = (correctCount * 100) / questions.size();
        boolean passed = scorePercent >= quiz.getPassingScore();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserEmail(email);
        attempt.setQuizId(quizId);
        attempt.setScore(scorePercent);
        attempt.setPassed(passed);
        attemptRepo.save(attempt);

        if (passed) {
            logActivity(email, "QUIZ_PASS", "Passed quiz: " + quiz.getTitle() + " with " + scorePercent + "%");
            // Check course completion since a quiz pass could trigger it
            checkAndUpdateCourseCompletion(email, quiz.getCourseId());
        } else {
            logActivity(email, "QUIZ_FAIL", "Failed quiz: " + quiz.getTitle() + " with " + scorePercent + "%");
        }

        return attempt;
    }

    @Transactional
    public AssignmentSubmission submitAssignment(String email, Long assignmentId, String submissionText, String fileUrl) {
        Assignment assignment = assignmentRepo.findById(assignmentId).orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        AssignmentSubmission submission = submissionRepo.findByUserEmailAndAssignmentId(email, assignmentId)
                .orElseGet(() -> {
                    AssignmentSubmission sub = new AssignmentSubmission();
                    sub.setUserEmail(email);
                    sub.setAssignmentId(assignmentId);
                    return sub;
                });

        submission.setSubmissionText(submissionText);
        if (fileUrl != null) {
            submission.setFileUrl(fileUrl);
        }
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(LocalDateTime.now());
        submissionRepo.save(submission);

        logActivity(email, "ASSIGNMENT_SUBMIT", "Submitted assignment: " + assignment.getTitle());

        return submission;
    }
}
