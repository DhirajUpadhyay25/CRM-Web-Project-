package in.project.main.entities.enums;

/**
 * Defines the identity/credentials verification status of an Instructor.
 */
public enum VerificationStatus {
    VERIFIED,
    PENDING,
    UNVERIFIED;

    public String getDisplayName() {
        switch (this) {
            case VERIFIED: return "Verified";
            case PENDING: return "Pending Verification";
            case UNVERIFIED: return "Unverified";
            default: return name();
        }
    }

    public String getBadgeClass() {
        switch (this) {
            case VERIFIED:
                return "bg-blue-50 text-blue-700 border-blue-200";
            case PENDING:
                return "bg-amber-50 text-amber-700 border-amber-200";
            case UNVERIFIED:
                return "bg-gray-100 text-gray-600 border-gray-200";
            default:
                return "bg-gray-100 text-gray-600 border-gray-200";
        }
    }

    public static VerificationStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return VERIFIED;
        }
        String clean = value.trim().toUpperCase();
        for (VerificationStatus status : values()) {
            if (status.name().equalsIgnoreCase(clean)) {
                return status;
            }
        }
        if (clean.contains("VERIF") && !clean.contains("UNVERIF")) return VERIFIED;
        if (clean.contains("PEND")) return PENDING;
        if (clean.contains("UNVERIF")) return UNVERIFIED;
        return VERIFIED;
    }
}
