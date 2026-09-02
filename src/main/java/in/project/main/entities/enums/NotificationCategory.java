package in.project.main.entities.enums;

/**
 * Functional categories for platform notifications.
 */
public enum NotificationCategory {
    ACCOUNT("Account & Security"),
    STUDENT("Student Management"),
    INSTRUCTOR("Instructor & Teaching"),
    COURSE("Courses & Lessons"),
    ENROLLMENT("Enrollments"),
    PAYMENT("Orders & Payments"),
    QUIZ("Quizzes & Assessments"),
    ASSIGNMENT("Assignments"),
    ANNOUNCEMENT("Announcements"),
    SYSTEM("System Alerts");

    private final String displayName;

    NotificationCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
