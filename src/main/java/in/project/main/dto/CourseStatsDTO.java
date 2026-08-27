package in.project.main.dto;

public class CourseStatsDTO {

    private long totalCourses;
    private long publishedCourses;
    private long draftCourses;
    private long archivedCourses;
    private long featuredCourses;
    private long freeCourses;
    private long paidCourses;

    public CourseStatsDTO() {}

    public CourseStatsDTO(long totalCourses, long publishedCourses, long draftCourses, 
                          long archivedCourses, long featuredCourses, long freeCourses, long paidCourses) {
        this.totalCourses = totalCourses;
        this.publishedCourses = publishedCourses;
        this.draftCourses = draftCourses;
        this.archivedCourses = archivedCourses;
        this.featuredCourses = featuredCourses;
        this.freeCourses = freeCourses;
        this.paidCourses = paidCourses;
    }

    public long getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(long totalCourses) {
        this.totalCourses = totalCourses;
    }

    public long getPublishedCourses() {
        return publishedCourses;
    }

    public void setPublishedCourses(long publishedCourses) {
        this.publishedCourses = publishedCourses;
    }

    public long getDraftCourses() {
        return draftCourses;
    }

    public void setDraftCourses(long draftCourses) {
        this.draftCourses = draftCourses;
    }

    public long getArchivedCourses() {
        return archivedCourses;
    }

    public void setArchivedCourses(long archivedCourses) {
        this.archivedCourses = archivedCourses;
    }

    public long getFeaturedCourses() {
        return featuredCourses;
    }

    public void setFeaturedCourses(long featuredCourses) {
        this.featuredCourses = featuredCourses;
    }

    public long getFreeCourses() {
        return freeCourses;
    }

    public void setFreeCourses(long freeCourses) {
        this.freeCourses = freeCourses;
    }

    public long getPaidCourses() {
        return paidCourses;
    }

    public void setPaidCourses(long paidCourses) {
        this.paidCourses = paidCourses;
    }
}
