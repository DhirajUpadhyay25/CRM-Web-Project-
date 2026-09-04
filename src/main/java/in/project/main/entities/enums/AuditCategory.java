package in.project.main.entities.enums;

public enum AuditCategory {
    AUTHENTICATION("Authentication & Session"),
    AUTHORIZATION("Authorization & Access"),
    USER("User Management"),
    STUDENT("Student Operations"),
    INSTRUCTOR("Instructor Operations"),
    COURSE("Course Management"),
    CURRICULUM("Curriculum & Content"),
    ENROLLMENT("Enrollments"),
    PAYMENT("Payments & Orders"),
    QUIZ("Quizzes & Assessments"),
    NOTIFICATION("Notifications"),
    ADMIN("Administration & Settings"),
    SYSTEM("System & Integration"),
    SECURITY("Security Alerts"),
    FEEDBACK("Feedback Management");

    private final String displayName;

    AuditCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
