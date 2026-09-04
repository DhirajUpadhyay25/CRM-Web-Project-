package in.project.main.entities.enums;

public enum TestimonialStatus {
    PENDING("Pending Review", "bg-yellow-100 text-yellow-700"),
    APPROVED("Approved", "bg-blue-100 text-blue-700"),
    REJECTED("Rejected", "bg-red-100 text-red-700"),
    PUBLISHED("Published", "bg-green-100 text-green-700"),
    ARCHIVED("Archived", "bg-gray-100 text-gray-500");

    private final String displayName;
    private final String badgeClass;

    TestimonialStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }
}
