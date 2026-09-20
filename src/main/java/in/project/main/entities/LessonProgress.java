package in.project.main.entities;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
    @Index(name = "idx_lesson_progress_user", columnList = "userEmail"),
    @Index(name = "idx_lesson_progress_course", columnList = "userEmail, courseId"),
    @Index(name = "idx_lesson_progress_lesson", columnList = "userEmail, lessonId")
})
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private boolean completed = false;

    @Column
    private LocalDateTime lastAccessedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private Double playbackPosition = 0.0; // Current playback second in video

    @Column
    private Integer watchPercentage = 0; // 0 to 100%

    @Column
    private Integer timeSpentSeconds = 0;

    @Column
    private String status = "NOT_STARTED"; // NOT_STARTED, IN_PROGRESS, COMPLETED

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public boolean isCompleted() { return completed; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(boolean completed) { 
        this.completed = completed;
        if (completed) {
            this.status = "COMPLETED";
            this.watchPercentage = 100;
        }
    }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public Double getPlaybackPosition() { return playbackPosition != null ? playbackPosition : 0.0; }
    public void setPlaybackPosition(Double playbackPosition) { this.playbackPosition = playbackPosition; }

    public Integer getWatchPercentage() { return watchPercentage != null ? watchPercentage : 0; }
    public void setWatchPercentage(Integer watchPercentage) { this.watchPercentage = watchPercentage; }

    public Integer getTimeSpentSeconds() { return timeSpentSeconds != null ? timeSpentSeconds : 0; }
    public void setTimeSpentSeconds(Integer timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }

    public String getStatus() { 
        if (completed) return "COMPLETED";
        return status != null ? status : "NOT_STARTED"; 
    }
    public void setStatus(String status) { this.status = status; }
}
