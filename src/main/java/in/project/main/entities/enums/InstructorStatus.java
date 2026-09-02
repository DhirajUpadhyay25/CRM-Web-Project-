package in.project.main.entities.enums;

/**
 * Defines the lifecycle status of an Instructor in the platform.
 */
public enum InstructorStatus {
    ACTIVE,
    INACTIVE,
    PENDING,
    SUSPENDED,
    BANNED;

    public String getDisplayName() {
        switch (this) {
            case ACTIVE: return "Active";
            case INACTIVE: return "Inactive";
            case PENDING: return "Pending";
            case SUSPENDED: return "Suspended";
            case BANNED: return "Banned";
            default: return name();
        }
    }

    public String getBadgeClass() {
        switch (this) {
            case ACTIVE:
                return "bg-emerald-50 text-emerald-700 border-emerald-200";
            case INACTIVE:
                return "bg-gray-100 text-gray-700 border-gray-200";
            case PENDING:
                return "bg-amber-50 text-amber-700 border-amber-200";
            case SUSPENDED:
                return "bg-orange-50 text-orange-700 border-orange-200";
            case BANNED:
                return "bg-red-50 text-red-700 border-red-200";
            default:
                return "bg-gray-100 text-gray-700 border-gray-200";
        }
    }

    public static InstructorStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ACTIVE;
        }
        String clean = value.trim().toUpperCase();
        for (InstructorStatus status : values()) {
            if (status.name().equalsIgnoreCase(clean)) {
                return status;
            }
        }
        if (clean.contains("ACT") && !clean.contains("INACT")) return ACTIVE;
        if (clean.contains("INACT")) return INACTIVE;
        if (clean.contains("PEND")) return PENDING;
        if (clean.contains("SUSP")) return SUSPENDED;
        if (clean.contains("BAN")) return BANNED;
        return ACTIVE;
    }
}
