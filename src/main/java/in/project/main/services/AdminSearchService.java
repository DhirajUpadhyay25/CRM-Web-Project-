package in.project.main.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import in.project.main.entities.Course;
import in.project.main.entities.Lead;
import in.project.main.entities.Orders;
import in.project.main.entities.User;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.LeadRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;

/**
 * Global admin search service.
 * Searches across multiple entity types using database queries (not in-memory).
 * Results are categorized and limited to prevent memory issues.
 */
@Service
public class AdminSearchService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private LeadRepository leadRepository;

    private static final int RESULTS_PER_CATEGORY = 5;

    /**
     * Search across courses, students, orders, and leads.
     * Returns a map of category name → list of result maps.
     * Each result map contains: id, title, subtitle, url, type.
     */
    public Map<String, List<Map<String, String>>> search(String query) {
        Map<String, List<Map<String, String>>> results = new LinkedHashMap<>();

        if (query == null || query.trim().length() < 2) {
            return results;
        }

        String keyword = query.trim();
        PageRequest limit = PageRequest.of(0, RESULTS_PER_CATEGORY);

        // Search Courses
        List<Map<String, String>> courseResults = new ArrayList<>();
        courseRepository.adminSearchAndFilterCourses(keyword, null, null, null, null, limit)
                .getContent().forEach(course -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("id", String.valueOf(course.getId()));
                    item.put("title", course.getName());
                    item.put("subtitle", course.getStatus() != null ? course.getStatus().name() : "");
                    item.put("url", "/admin/courses/" + course.getId());
                    item.put("type", "COURSE");
                    courseResults.add(item);
                });
        if (!courseResults.isEmpty()) results.put("Courses", courseResults);

        // Search Students
        List<Map<String, String>> studentResults = new ArrayList<>();
        userRepository.searchUsers(keyword, limit)
                .getContent().forEach(user -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("id", String.valueOf(user.getId()));
                    item.put("title", user.getName());
                    item.put("subtitle", user.getEmail());
                    item.put("url", "/admin/students");
                    item.put("type", "STUDENT");
                    studentResults.add(item);
                });
        if (!studentResults.isEmpty()) results.put("Students", studentResults);

        // Search Orders
        List<Map<String, String>> orderResults = new ArrayList<>();
        ordersRepository.searchOrders(keyword, limit)
                .getContent().forEach(order -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("id", String.valueOf(order.getId()));
                    item.put("title", order.getCourseName());
                    item.put("subtitle", order.getUserEmail() + " • ₹" + order.getCourseAmount());
                    item.put("url", "/admin/orders/" + order.getId());
                    item.put("type", "ORDER");
                    orderResults.add(item);
                });
        if (!orderResults.isEmpty()) results.put("Orders", orderResults);

        // Search Leads
        List<Map<String, String>> leadResults = new ArrayList<>();
        leadRepository.searchAndFilter(keyword, null, null, limit)
                .getContent().forEach(lead -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("id", String.valueOf(lead.getId()));
                    item.put("title", lead.getName());
                    item.put("subtitle", lead.getStatus() != null ? lead.getStatus().name() : "");
                    item.put("url", "/admin/leads/" + lead.getId());
                    item.put("type", "LEAD");
                    leadResults.add(item);
                });
        if (!leadResults.isEmpty()) results.put("Leads", leadResults);

        return results;
    }
}
