package in.project.main.dto;

import jakarta.validation.constraints.NotNull;

public class InstructorCourseAssignDTO {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
}
