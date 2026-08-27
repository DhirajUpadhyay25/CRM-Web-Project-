package in.project.main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.entities.enums.CourseStatus;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    public Map<String, Object> getAdminDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        long totalUsers = userRepository.count();
        metrics.put("totalUsers", totalUsers);

        long publishedCourses = courseRepository.countByStatus(CourseStatus.PUBLISHED);
        metrics.put("publishedCourses", publishedCourses);

        long totalOrders = ordersRepository.count();
        metrics.put("totalOrders", totalOrders);

        // Revenue will be calculated if there's a custom method, else 0
        Double totalRevenue = 0.0;
        try {
            totalRevenue = ordersRepository.calculateTotalRevenue();
            if (totalRevenue == null) totalRevenue = 0.0;
        } catch (Exception e) {
            // Method might not exist yet, ignore
        }
        metrics.put("totalRevenue", totalRevenue);

        metrics.put("newLeads", 0);

        return metrics;
    }
}
