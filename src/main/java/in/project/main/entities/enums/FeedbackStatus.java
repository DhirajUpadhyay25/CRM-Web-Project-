package in.project.main.entities.enums;

public enum FeedbackStatus {
    NEW("New", "bg-blue-100 text-blue-700"),
    UNDER_REVIEW("Under Review", "bg-yellow-100 text-yellow-700"),
    IN_PROGRESS("In Progress", "bg-indigo-100 text-indigo-700"),
    RESPONDED("Responded", "bg-purple-100 text-purple-700"),
    RESOLVED("Resolved", "bg-green-100 text-green-700"),
    CLOSED("Closed", "bg-gray-100 text-gray-700"),
    REJECTED("Rejected", "bg-red-100 text-red-700"),
    ARCHIVED("Archived", "bg-gray-100 text-gray-500");

    private final String displayName;
    private final String badgeClass;

    FeedbackStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }

    public boolean isEditable() {
        return this == NEW || this == UNDER_REVIEW;
    }

    public boolean isTerminal() {
        return this == RESOLVED || this == CLOSED || this == REJECTED || this == ARCHIVED;
    }
}
