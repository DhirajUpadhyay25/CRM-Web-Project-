package in.project.main.entities.enums;

public enum ContentVisibility {
    PUBLIC("Public", "Everyone can view"),
    STUDENTS_ONLY("Students Only", "Only logged-in students can view"),
    INSTRUCTORS_ONLY("Instructors Only", "Only instructors can view"),
    ADMINS_ONLY("Admins Only", "Only administrators can view");

    private final String displayName;
    private final String description;

    ContentVisibility(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
