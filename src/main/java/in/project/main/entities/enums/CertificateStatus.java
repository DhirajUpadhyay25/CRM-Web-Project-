package in.project.main.entities.enums;

/**
 * Lifecycle statuses for certificates within the LMS credential ecosystem.
 */
public enum CertificateStatus {
    ELIGIBLE("Eligible", "Course completed, awaiting student request", "bg-sky-50 text-sky-700 border-sky-200", "bi-stars"),
    REQUESTED("Requested", "Student submitted certificate claim", "bg-amber-50 text-amber-700 border-amber-200", "bi-clock-history"),
    UNDER_REVIEW("Under Review", "Administrative review in progress", "bg-indigo-50 text-indigo-700 border-indigo-200", "bi-search"),
    APPROVED("Approved", "Approved, pending issuance", "bg-emerald-50 text-emerald-700 border-emerald-200", "bi-check2-circle"),
    ISSUED("Issued", "Active, officially generated and verifiable", "bg-emerald-100 text-emerald-800 border-emerald-300", "bi-patch-check-fill"),
    REJECTED("Rejected", "Request rejected by administrator", "bg-rose-50 text-rose-700 border-rose-200", "bi-x-circle-fill"),
    REVOKED("Revoked", "Administratively revoked or superseded", "bg-red-100 text-red-800 border-red-300", "bi-slash-circle-fill"),
    EXPIRED("Expired", "Certificate validity period has expired", "bg-gray-100 text-gray-700 border-gray-300", "bi-calendar-x");

    private final String displayName;
    private final String description;
    private final String badgeClasses;
    private final String icon;

    CertificateStatus(String displayName, String description, String badgeClasses, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.badgeClasses = badgeClasses;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getBadgeClasses() {
        return badgeClasses;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isValid() {
        return this == ISSUED;
    }

    public boolean isTerminal() {
        return this == REVOKED || this == EXPIRED;
    }

    public boolean canBeApproved() {
        return this == REQUESTED || this == UNDER_REVIEW;
    }

    public boolean canBeRevoked() {
        return this == ISSUED || this == APPROVED;
    }
}
