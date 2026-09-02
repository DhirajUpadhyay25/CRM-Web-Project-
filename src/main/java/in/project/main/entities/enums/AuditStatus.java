package in.project.main.entities.enums;

public enum AuditStatus {
    SUCCESS("Success", "bg-green-50 text-green-700 border-green-200", "bi-check-circle-fill text-green-500"),
    FAILED("Failed", "bg-red-50 text-red-700 border-red-200", "bi-x-circle-fill text-red-500"),
    DENIED("Denied", "bg-amber-50 text-amber-700 border-amber-200", "bi-slash-circle-fill text-amber-500");

    private final String displayName;
    private final String badgeClass;
    private final String iconClass;

    AuditStatus(String displayName, String badgeClass, String iconClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.iconClass = iconClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }
    public String getIconClass() { return iconClass; }
}
