package in.project.main.dto;

import java.util.ArrayList;
import java.util.List;

public class EnrollmentAnalyticsDTO {

    private EnrollmentStatsDTO stats;

    // Status breakdown for Donut/Pie Chart
    private List<StatusCountItem> statusBreakdown = new ArrayList<>();

    // 30-day enrollment timeline trend
    private List<DailyTrendItem> trendLast30Days = new ArrayList<>();

    // Top 5 Enrolled Courses
    private List<CourseEnrollmentCountItem> topCourses = new ArrayList<>();

    // Enrollment Type Breakdown (Free vs Paid vs Admin Assigned)
    private List<TypeCountItem> typeBreakdown = new ArrayList<>();

    public static class StatusCountItem {
        private String status;
        private String label;
        private long count;
        private String color;

        public StatusCountItem() {}

        public StatusCountItem(String status, String label, long count, String color) {
            this.status = status;
            this.label = label;
            this.count = count;
            this.color = color;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class DailyTrendItem {
        private String date;
        private long count;

        public DailyTrendItem() {}

        public DailyTrendItem(String date, long count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class CourseEnrollmentCountItem {
        private Long courseId;
        private String courseName;
        private String categoryName;
        private long enrollmentCount;
        private long activeCount;
        private long completedCount;

        public CourseEnrollmentCountItem() {}

        public CourseEnrollmentCountItem(Long courseId, String courseName, String categoryName, long enrollmentCount, long activeCount, long completedCount) {
            this.courseId = courseId;
            this.courseName = courseName;
            this.categoryName = categoryName;
            this.enrollmentCount = enrollmentCount;
            this.activeCount = activeCount;
            this.completedCount = completedCount;
        }

        public Long getCourseId() { return courseId; }
        public void setCourseId(Long courseId) { this.courseId = courseId; }
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public long getEnrollmentCount() { return enrollmentCount; }
        public void setEnrollmentCount(long enrollmentCount) { this.enrollmentCount = enrollmentCount; }
        public long getActiveCount() { return activeCount; }
        public void setActiveCount(long activeCount) { this.activeCount = activeCount; }
        public long getCompletedCount() { return completedCount; }
        public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }
    }

    public static class TypeCountItem {
        private String type;
        private String label;
        private long count;

        public TypeCountItem() {}

        public TypeCountItem(String type, String label, long count) {
            this.type = type;
            this.label = label;
            this.count = count;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public EnrollmentAnalyticsDTO() {}

    public EnrollmentStatsDTO getStats() { return stats; }
    public void setStats(EnrollmentStatsDTO stats) { this.stats = stats; }

    public List<StatusCountItem> getStatusBreakdown() { return statusBreakdown; }
    public void setStatusBreakdown(List<StatusCountItem> statusBreakdown) { this.statusBreakdown = statusBreakdown; }

    public List<DailyTrendItem> getTrendLast30Days() { return trendLast30Days; }
    public void setTrendLast30Days(List<DailyTrendItem> trendLast30Days) { this.trendLast30Days = trendLast30Days; }

    public List<CourseEnrollmentCountItem> getTopCourses() { return topCourses; }
    public void setTopCourses(List<CourseEnrollmentCountItem> topCourses) { this.topCourses = topCourses; }

    public List<TypeCountItem> getTypeBreakdown() { return typeBreakdown; }
    public void setTypeBreakdown(List<TypeCountItem> typeBreakdown) { this.typeBreakdown = typeBreakdown; }
}
