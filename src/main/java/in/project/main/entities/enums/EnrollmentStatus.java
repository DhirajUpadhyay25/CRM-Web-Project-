package in.project.main.entities.enums;

/**
 * Enrollment lifecycle statuses.
 */
public enum EnrollmentStatus {
    PENDING("Pending", "bg-amber-50 text-amber-700 border-amber-200", "bg-amber-500", false),
    ACTIVE("Active", "bg-emerald-50 text-emerald-700 border-emerald-200", "bg-emerald-500", true),
    COMPLETED("Completed", "bg-blue-50 text-blue-700 border-blue-200", "bg-blue-500", true),
    SUSPENDED("Suspended", "bg-orange-50 text-orange-700 border-orange-200", "bg-orange-500", false),
    CANCELLED("Cancelled", "bg-rose-50 text-rose-700 border-rose-200", "bg-rose-500", false),
    REVOKED("Revoked", "bg-red-50 text-red-800 border-red-300", "bg-red-600", false),
    EXPIRED("Expired", "bg-gray-100 text-gray-700 border-gray-200", "bg-gray-400", false);

    private final String displayName;
    private final String badgeClass;
    private final String dotClass;
    private final boolean accessAllowed;

    EnrollmentStatus(String displayName, String badgeClass, String dotClass, boolean accessAllowed) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.dotClass = dotClass;
        this.accessAllowed = accessAllowed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getDotClass() {
        return dotClass;
    }

    public boolean isAccessAllowed() {
        return accessAllowed;
    }
}

