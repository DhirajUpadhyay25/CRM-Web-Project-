package in.project.main.entities.enums;

public enum FeedbackPriority {
    LOW("Low", "bg-gray-100 text-gray-600"),
    MEDIUM("Medium", "bg-blue-100 text-blue-600"),
    HIGH("High", "bg-orange-100 text-orange-600"),
    CRITICAL("Critical", "bg-red-100 text-red-700");

    private final String displayName;
    private final String badgeClass;

    FeedbackPriority(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }
}
