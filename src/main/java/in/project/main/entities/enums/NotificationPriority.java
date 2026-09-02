package in.project.main.entities.enums;

/**
 * Priority levels for notifications.
 */
public enum NotificationPriority {
    LOW("Low", "bg-gray-100 text-gray-700 border-gray-200"),
    NORMAL("Normal", "bg-blue-50 text-blue-700 border-blue-200"),
    HIGH("High", "bg-amber-50 text-amber-700 border-amber-200"),
    CRITICAL("Critical", "bg-red-50 text-red-700 border-red-200");

    private final String displayName;
    private final String badgeClass;

    NotificationPriority(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
