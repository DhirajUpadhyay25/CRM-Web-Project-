package in.project.main.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LessonDiscussionDTO {
    private Long id;
    private Long courseId;
    private Long lessonId;
    private String authorEmail;
    private String authorName;
    private String authorRole;
    private String questionTitle;
    private String content;
    private Long parentId;
    private LocalDateTime createdAt;
    private String timeAgo;
    private List<LessonDiscussionDTO> replies = new ArrayList<>();

    public LessonDiscussionDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public String getQuestionTitle() { return questionTitle; }
    public void setQuestionTitle(String questionTitle) { this.questionTitle = questionTitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }

    public List<LessonDiscussionDTO> getReplies() { return replies; }
    public void setReplies(List<LessonDiscussionDTO> replies) { this.replies = replies; }
}
