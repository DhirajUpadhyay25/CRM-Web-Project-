package in.project.main.dto;

import java.time.LocalDateTime;

public class StudentEnrolledCourseDTO {
    private Long enrollmentId;
    private Long courseId;
    private String courseName;
    private String courseSlug;
    private String shortDescription;
    private String imageUrl;
    private String categoryName;
    private String level;
    private String duration;
    private String instructorName;
    private LocalDateTime enrolledAt;
    private LocalDateTime lastAccessedAt;
    private String enrollmentStatus; // ACTIVE, COMPLETED, etc.

    private int progressPercentage; // 0 to 100
    private int completedLessonsCount;
    private int totalLessonsCount;
    private String lastAccessedLessonTitle;
    private Long lastAccessedLessonId;
    private Long targetLessonId; // Lesson ID to open on click

    private String actionType; // "START", "CONTINUE", "REVIEW"
    private boolean completed;
    private boolean started;
    private boolean certificateEligible;

    public StudentEnrolledCourseDTO() {}

    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseSlug() { return courseSlug; }
    public void setCourseSlug(String courseSlug) { this.courseSlug = courseSlug; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public String getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    public int getCompletedLessonsCount() { return completedLessonsCount; }
    public void setCompletedLessonsCount(int completedLessonsCount) { this.completedLessonsCount = completedLessonsCount; }

    public int getTotalLessonsCount() { return totalLessonsCount; }
    public void setTotalLessonsCount(int totalLessonsCount) { this.totalLessonsCount = totalLessonsCount; }

    public String getLastAccessedLessonTitle() { return lastAccessedLessonTitle; }
    public void setLastAccessedLessonTitle(String lastAccessedLessonTitle) { this.lastAccessedLessonTitle = lastAccessedLessonTitle; }

    public Long getLastAccessedLessonId() { return lastAccessedLessonId; }
    public void setLastAccessedLessonId(Long lastAccessedLessonId) { this.lastAccessedLessonId = lastAccessedLessonId; }

    public Long getTargetLessonId() { return targetLessonId; }
    public void setTargetLessonId(Long targetLessonId) { this.targetLessonId = targetLessonId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }

    public boolean isCertificateEligible() { return certificateEligible; }
    public void setCertificateEligible(boolean certificateEligible) { this.certificateEligible = certificateEligible; }
}
