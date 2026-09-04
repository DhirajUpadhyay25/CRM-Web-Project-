package in.project.main.entities.enums;

public enum TestimonialSource {
    STUDENT_SUBMISSION("Student Submission"),
    FEEDBACK_CONVERSION("Converted from Feedback"),
    ADMIN_CREATED("Admin Created");

    private final String displayName;

    TestimonialSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
