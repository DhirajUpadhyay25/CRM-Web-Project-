package in.project.main.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkEnrollmentResultDTO {

    private int totalRequested;
    private int successCount;
    private int skippedAlreadyEnrolledCount;
    private int failedCount;
    private Long courseId;
    private String courseName;
    private List<ItemResult> results = new ArrayList<>();

    public static class ItemResult {
        private String studentIdentifier;
        private String studentName;
        private boolean success;
        private String status; // ENROLLED, ALREADY_ENROLLED, FAILED, INVALID_STUDENT
        private String message;
        private Long enrollmentId;

        public ItemResult() {}

        public ItemResult(String studentIdentifier, String studentName, boolean success, String status, String message, Long enrollmentId) {
            this.studentIdentifier = studentIdentifier;
            this.studentName = studentName;
            this.success = success;
            this.status = status;
            this.message = message;
            this.enrollmentId = enrollmentId;
        }

        public String getStudentIdentifier() { return studentIdentifier; }
        public void setStudentIdentifier(String studentIdentifier) { this.studentIdentifier = studentIdentifier; }

        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Long getEnrollmentId() { return enrollmentId; }
        public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }
    }

    public BulkEnrollmentResultDTO() {}

    public int getTotalRequested() { return totalRequested; }
    public void setTotalRequested(int totalRequested) { this.totalRequested = totalRequested; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getSkippedAlreadyEnrolledCount() { return skippedAlreadyEnrolledCount; }
    public void setSkippedAlreadyEnrolledCount(int skippedAlreadyEnrolledCount) { this.skippedAlreadyEnrolledCount = skippedAlreadyEnrolledCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public List<ItemResult> getResults() { return results; }
    public void setResults(List<ItemResult> results) { this.results = results; }
}
