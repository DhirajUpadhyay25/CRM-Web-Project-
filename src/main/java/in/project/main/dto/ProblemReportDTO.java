package in.project.main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProblemReportDTO {

    private String name;
    private String email;

    @NotBlank(message = "Issue type is required")
    private String issueType; // BROKEN_PAGE, VIDEO_ISSUE, QUIZ_ISSUE, ASSIGNMENT_ISSUE, UI_BUG, INCORRECT_CONTENT, OTHER

    @NotBlank(message = "Please describe the problem")
    @Size(min = 5, max = 2000, message = "Description must be at least 5 characters")
    private String description;

    private String pageUrl;
    private String browserInfo;
    private String deviceInfo;
    private Long courseId;
    private Long lessonId;

    public ProblemReportDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

    public String getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
}
