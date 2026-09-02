package in.project.main.entities.enums;

public enum AuditSeverity {
    INFO("Info", "bg-blue-50 text-blue-700 border-blue-200", "bi-info-circle"),
    LOW("Low", "bg-gray-50 text-gray-700 border-gray-200", "bi-dash-circle"),
    MEDIUM("Medium", "bg-amber-50 text-amber-700 border-amber-200", "bi-exclamation-circle"),
    HIGH("High", "bg-orange-50 text-orange-700 border-orange-200", "bi-exclamation-triangle"),
    CRITICAL("Critical", "bg-red-50 text-red-700 border-red-200", "bi-shield-exclamation");

    private final String displayName;
    private final String badgeClass;
    private final String iconClass;

    AuditSeverity(String displayName, String badgeClass, String iconClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.iconClass = iconClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }
    public String getIconClass() { return iconClass; }
}
