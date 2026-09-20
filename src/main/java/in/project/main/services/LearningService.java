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
    @Autowired private StudentLessonNoteRepository noteRepo;
    @Autowired private LessonBookmarkRepository bookmarkRepo;
    @Autowired private LessonDiscussionRepository discussionRepo;

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

        // Free preview is always unlocked
        Lesson target = lessonRepo.findById(lessonId).orElse(null);
        if (target != null && Boolean.TRUE.equals(target.getIsFreePreview())) {
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

    // ----------------------------------------------------
    // ENRICHED MY COURSES DASHBOARD
    // ----------------------------------------------------
    public List<in.project.main.dto.StudentEnrolledCourseDTO> getStudentEnrolledCoursesOverview(String email, String search, String filter) {
        List<Enrollment> enrollments = enrollmentRepo.findByUserEmailOrderByEnrolledAtDesc(email);
        List<in.project.main.dto.StudentEnrolledCourseDTO> dtos = new ArrayList<>();

        String searchLower = (search != null) ? search.trim().toLowerCase() : "";

        for (Enrollment e : enrollments) {
            Course c = e.getCourse();
            if (c == null) continue;

            // Search filter
            if (!searchLower.isEmpty()) {
                boolean matchTitle = c.getName() != null && c.getName().toLowerCase().contains(searchLower);
                boolean matchInstructor = c.getInstructor() != null && c.getInstructor().toLowerCase().contains(searchLower);
                boolean matchCategory = c.getCategory() != null && c.getCategory().getName() != null && c.getCategory().getName().toLowerCase().contains(searchLower);
                if (!matchTitle && !matchInstructor && !matchCategory) {
                    continue;
                }
            }

            in.project.main.dto.StudentEnrolledCourseDTO dto = new in.project.main.dto.StudentEnrolledCourseDTO();
            dto.setEnrollmentId(e.getId());
            dto.setCourseId(c.getId());
            dto.setCourseName(c.getName());
            dto.setCourseSlug(c.getSlug());
            dto.setShortDescription(c.getShortDescription() != null ? c.getShortDescription() : c.getDescription());
            dto.setImageUrl(c.getImageUrl());
            dto.setCategoryName(c.getCategory() != null ? c.getCategory().getName() : "General");
            dto.setLevel(c.getLevel() != null ? c.getLevel().name() : "ALL_LEVELS");
            dto.setDuration(c.getDuration());
            dto.setInstructorName(c.getInstructor() != null ? c.getInstructor() : "EduTake Mentor");
            dto.setEnrolledAt(e.getEnrolledAt());
            dto.setLastAccessedAt(e.getLastAccessedAt());
            dto.setEnrollmentStatus(e.getStatus() != null ? e.getStatus().name() : "ACTIVE");

            List<Lesson> lessons = lessonRepo.findByCourseIdOrderByOrderIndexAsc(String.valueOf(c.getId()));
            dto.setTotalLessonsCount(lessons.size());

            long completedCount = progressRepo.countByUserEmailAndCourseIdAndCompleted(email, c.getId(), true);
            dto.setCompletedLessonsCount((int) completedCount);

            int percent = 0;
            if (e.getStatus() == EnrollmentStatus.COMPLETED) {
                percent = 100;
            } else if (!lessons.isEmpty()) {
                percent = (int) ((completedCount * 100) / lessons.size());
            }
            dto.setProgressPercentage(percent);

            boolean isCompleted = (e.getStatus() == EnrollmentStatus.COMPLETED || percent >= 100);
            boolean isStarted = (completedCount > 0 || e.getLastAccessedAt() != null);
            dto.setCompleted(isCompleted);
            dto.setStarted(isStarted);
            dto.setCertificateEligible(isCompleted);

            // Status Filter logic: ALL, IN_PROGRESS, NOT_STARTED, COMPLETED
            if (filter != null && !filter.isEmpty() && !"ALL".equalsIgnoreCase(filter)) {
                if ("COMPLETED".equalsIgnoreCase(filter) && !isCompleted) continue;
                if ("NOT_STARTED".equalsIgnoreCase(filter) && (isStarted || isCompleted)) continue;
                if ("IN_PROGRESS".equalsIgnoreCase(filter) && (!isStarted || isCompleted)) continue;
            }

            // Record last accessed lesson title if present
            LessonProgress lastProg = progressRepo.findFirstByUserEmailAndCourseIdOrderByLastAccessedAtDesc(email, c.getId());
            if (lastProg != null) {
                dto.setLastAccessedLessonId(lastProg.getLessonId());
                lessonRepo.findById(lastProg.getLessonId()).ifPresent(l -> dto.setLastAccessedLessonTitle(l.getTitle()));
            }

            // Target lesson logic (Phase 5):
            // 1. If course is not completed, target the first incomplete lesson!
            // 2. If all lessons are completed, target first lesson for review.
            Long targetLessonId = null;
            for (Lesson l : lessons) {
                Optional<LessonProgress> lp = progressRepo.findByUserEmailAndLessonId(email, l.getId());
                if (lp.isEmpty() || !lp.get().isCompleted()) {
                    targetLessonId = l.getId();
                    break;
                }
            }
            if (targetLessonId == null && !lessons.isEmpty()) {
                targetLessonId = (lastProg != null) ? lastProg.getLessonId() : lessons.get(0).getId();
            }
            dto.setTargetLessonId(targetLessonId);

            if (isCompleted) {
                dto.setActionType("REVIEW");
            } else if (isStarted) {
                dto.setActionType("CONTINUE");
            } else {
                dto.setActionType("START");
            }

            dtos.add(dto);
        }

        return dtos;
    }

    public Map<String, Long> getStudentCourseMetrics(String email) {
        List<Enrollment> enrollments = enrollmentRepo.findByUserEmailOrderByEnrolledAtDesc(email);
        long totalCount = enrollments.size();
        long completedCount = 0;
        long inProgressCount = 0;
        long notStartedCount = 0;

        for (Enrollment e : enrollments) {
            Course c = e.getCourse();
            if (c == null) continue;
            if (e.getStatus() == EnrollmentStatus.COMPLETED) {
                completedCount++;
            } else {
                long completedLessons = progressRepo.countByUserEmailAndCourseIdAndCompleted(email, c.getId(), true);
                if (completedLessons > 0 || e.getLastAccessedAt() != null) {
                    inProgressCount++;
                } else {
                    notStartedCount++;
                }
            }
        }

        Map<String, Long> metrics = new HashMap<>();
        metrics.put("totalCount", totalCount);
        metrics.put("completedCount", completedCount);
        metrics.put("inProgressCount", inProgressCount);
        metrics.put("notStartedCount", notStartedCount);
        return metrics;
    }

    // ----------------------------------------------------
    // VIDEO PLAYBACK TRACKING
    // ----------------------------------------------------
    @Transactional
    public void updateVideoProgress(String email, Long courseId, Long lessonId, double seconds, int percent) {
        LessonProgress progress = progressRepo.findByUserEmailAndLessonId(email, lessonId)
                .orElseGet(() -> {
                    LessonProgress newProg = new LessonProgress();
                    newProg.setUserEmail(email);
                    newProg.setCourseId(courseId);
                    newProg.setLessonId(lessonId);
                    newProg.setStartedAt(LocalDateTime.now());
                    return newProg;
                });

        progress.setPlaybackPosition(seconds);
        progress.setWatchPercentage(Math.max(progress.getWatchPercentage() != null ? progress.getWatchPercentage() : 0, percent));
        progress.setLastAccessedAt(LocalDateTime.now());
        if (progress.getStartedAt() == null) {
            progress.setStartedAt(LocalDateTime.now());
        }

        // Automatic completion rule: if video watch percentage is >= 85%, mark complete!
        if (percent >= 85 && !progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            progress.setStatus("COMPLETED");
            progressRepo.save(progress);

            Lesson lesson = lessonRepo.findById(lessonId).orElse(null);
            String title = lesson != null ? lesson.getTitle() : "Lesson " + lessonId;
            logActivity(email, "LESSON_COMPLETE", "Completed lesson: " + title);

            checkAndUpdateCourseCompletion(email, courseId);
        } else {
            if (!progress.isCompleted()) {
                progress.setStatus("IN_PROGRESS");
            }
            progressRepo.save(progress);
        }

        enrollmentRepo.findByUserEmailAndCourseId(email, courseId).ifPresent(e -> {
            e.setLastAccessedAt(LocalDateTime.now());
            enrollmentRepo.save(e);
        });
    }

    // ----------------------------------------------------
    // STUDENT LESSON NOTES
    // ----------------------------------------------------
    @Transactional
    public StudentLessonNote saveStudentNote(String email, Long courseId, Long lessonId, String content, Double timestampSeconds) {
        StudentLessonNote note = new StudentLessonNote();
        note.setUserEmail(email);
        note.setCourseId(courseId);
        note.setLessonId(lessonId);
        note.setContent(content);
        note.setTimestampSeconds(timestampSeconds != null ? timestampSeconds : 0.0);
        return noteRepo.save(note);
    }

    public List<StudentLessonNote> getStudentNotes(String email, Long lessonId) {
        return noteRepo.findByUserEmailAndLessonIdOrderByCreatedAtDesc(email, lessonId);
    }

    @Transactional
    public boolean deleteStudentNote(String email, Long noteId) {
        Optional<StudentLessonNote> noteOpt = noteRepo.findById(noteId);
        if (noteOpt.isPresent() && noteOpt.get().getUserEmail().equalsIgnoreCase(email)) {
            noteRepo.delete(noteOpt.get());
            return true;
        }
        return false;
    }

    // ----------------------------------------------------
    // LESSON BOOKMARKS
    // ----------------------------------------------------
    @Transactional
    public boolean toggleLessonBookmark(String email, Long courseId, Long lessonId, String lessonTitle) {
        Optional<LessonBookmark> b = bookmarkRepo.findByUserEmailAndLessonId(email, lessonId);
        if (b.isPresent()) {
            bookmarkRepo.delete(b.get());
            return false; // Removed
        } else {
            LessonBookmark newB = new LessonBookmark();
            newB.setUserEmail(email);
            newB.setCourseId(courseId);
            newB.setLessonId(lessonId);
            newB.setLessonTitle(lessonTitle);
            bookmarkRepo.save(newB);
            return true; // Added
        }
    }

    public boolean isLessonBookmarked(String email, Long lessonId) {
        return bookmarkRepo.existsByUserEmailAndLessonId(email, lessonId);
    }

    public List<LessonBookmark> getStudentBookmarks(String email) {
        return bookmarkRepo.findByUserEmailOrderByCreatedAtDesc(email);
    }

    // ----------------------------------------------------
    // LESSON Q&A / DISCUSSIONS
    // ----------------------------------------------------
    @Transactional
    public LessonDiscussion postLessonQuestion(String email, String authorName, String role, Long courseId, Long lessonId, String title, String content, Long parentId) {
        LessonDiscussion d = new LessonDiscussion();
        d.setCourseId(courseId);
        d.setLessonId(lessonId);
        d.setAuthorEmail(email);
        d.setAuthorName(authorName != null ? authorName : email);
        d.setAuthorRole(role != null ? role : "STUDENT");
        d.setQuestionTitle(title);
        d.setContent(content);
        d.setParentId(parentId);
        return discussionRepo.save(d);
    }

    public List<in.project.main.dto.LessonDiscussionDTO> getLessonDiscussions(Long lessonId) {
        List<LessonDiscussion> rootQuestions = discussionRepo.findByLessonIdAndParentIdIsNullOrderByCreatedAtDesc(lessonId);
        List<in.project.main.dto.LessonDiscussionDTO> result = new ArrayList<>();

        for (LessonDiscussion q : rootQuestions) {
            in.project.main.dto.LessonDiscussionDTO dto = toDiscussionDTO(q);
            List<LessonDiscussion> replies = discussionRepo.findByParentIdOrderByCreatedAtAsc(q.getId());
            for (LessonDiscussion r : replies) {
                dto.getReplies().add(toDiscussionDTO(r));
            }
            result.add(dto);
        }
        return result;
    }

    private in.project.main.dto.LessonDiscussionDTO toDiscussionDTO(LessonDiscussion d) {
        in.project.main.dto.LessonDiscussionDTO dto = new in.project.main.dto.LessonDiscussionDTO();
        dto.setId(d.getId());
        dto.setCourseId(d.getCourseId());
        dto.setLessonId(d.getLessonId());
        dto.setAuthorEmail(d.getAuthorEmail());
        dto.setAuthorName(d.getAuthorName());
        dto.setAuthorRole(d.getAuthorRole());
        dto.setQuestionTitle(d.getQuestionTitle());
        dto.setContent(d.getContent());
        dto.setParentId(d.getParentId());
        dto.setCreatedAt(d.getCreatedAt());
        if (d.getCreatedAt() != null) {
            long mins = java.time.Duration.between(d.getCreatedAt(), LocalDateTime.now()).toMinutes();
            if (mins < 1) dto.setTimeAgo("Just now");
            else if (mins < 60) dto.setTimeAgo(mins + "m ago");
            else if (mins < 1440) dto.setTimeAgo((mins / 60) + "h ago");
            else dto.setTimeAgo((mins / 1440) + "d ago");
        }
        return dto;
    }
}
