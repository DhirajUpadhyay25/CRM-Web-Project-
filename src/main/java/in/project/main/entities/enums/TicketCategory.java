package in.project.main.entities.enums;

public enum TicketCategory {
    ACCOUNT_LOGIN("Account & Login", "bi-person-badge"),
    COURSE_ACCESS("Course Access", "bi-collection-play"),
    LESSON_VIDEO("Lesson / Video", "bi-play-circle"),
    ASSIGNMENT("Assignment", "bi-journal-check"),
    QUIZ_ASSESSMENT("Quiz / Assessment", "bi-patch-question"),
    PROGRESS_TRACKING("Progress Tracking", "bi-graph-up-arrow"),
    CERTIFICATE("Certificate", "bi-award"),
    PAYMENT_SUBSCRIPTION("Payment & Subscription", "bi-credit-card"),
    TECHNICAL_ISSUE("Technical Issue", "bi-bug"),
    INSTRUCTOR_CONTENT("Instructor / Course Content", "bi-person-workspace"),
    FEEDBACK("Feedback & Suggestion", "bi-chat-heart"),
    OTHER("Other Inquiry", "bi-three-dots");

    private final String displayName;
    private final String iconClass;

    TicketCategory(String displayName, String iconClass) {
        this.displayName = displayName;
        this.iconClass = iconClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconClass() {
        return iconClass;
    }
}
