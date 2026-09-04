package in.project.main.entities.enums;

public enum FeedbackType {
    COURSE("Course Feedback", "bi-book"),
    INSTRUCTOR("Instructor Feedback", "bi-person-workspace"),
    LIVE_CLASS("Live Class / Session", "bi-camera-video"),
    PLATFORM("Platform Experience", "bi-laptop"),
    LEARNING_MATERIAL("Learning Material", "bi-file-earmark-play"),
    ASSIGNMENT("Assignment", "bi-journal-check"),
    QUIZ("Quiz / Assessment", "bi-patch-question"),
    CERTIFICATE("Certificate Process", "bi-award"),
    TECHNICAL_ISSUE("Technical Issue", "bi-bug"),
    SUPPORT("Support Experience", "bi-headset"),
    GENERAL("General Suggestion", "bi-chat-dots"),
    OTHER("Other", "bi-three-dots");

    private final String displayName;
    private final String iconClass;

    FeedbackType(String displayName, String iconClass) {
        this.displayName = displayName;
        this.iconClass = iconClass;
    }

    public String getDisplayName() { return displayName; }
    public String getIconClass() { return iconClass; }
}
