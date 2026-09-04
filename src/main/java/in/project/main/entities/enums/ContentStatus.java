package in.project.main.entities.enums;

public enum ContentStatus {
    DRAFT("Draft", "bg-gray-100 text-gray-600"),
    SCHEDULED("Scheduled", "bg-purple-100 text-purple-600"),
    PUBLISHED("Published", "bg-green-100 text-green-700"),
    UNPUBLISHED("Unpublished", "bg-yellow-100 text-yellow-700"),
    ARCHIVED("Archived", "bg-gray-100 text-gray-500");

    private final String displayName;
    private final String badgeClass;

    ContentStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }

    public boolean isVisibleToPublic() {
        return this == PUBLISHED;
    }
}
