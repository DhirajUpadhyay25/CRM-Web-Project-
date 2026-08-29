package in.project.main.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.project.main.entities.Lead;
import in.project.main.entities.Orders;
import in.project.main.entities.User;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.entities.enums.EnquiryStatus;
import in.project.main.entities.enums.LeadStatus;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EnquiryRepository;
import in.project.main.repositories.LeadRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private EnquiryRepository enquiryRepository;

    public Map<String, Object> getAdminDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Student metrics
        long totalUsers = userRepository.count();
        long activeStudents = userRepository.countByBanStatusFalse();
        metrics.put("totalUsers", totalUsers);
        metrics.put("activeStudents", activeStudents);

        // Course metrics
        long publishedCourses = courseRepository.countByStatus(CourseStatus.PUBLISHED);
        long draftCourses = courseRepository.countByStatus(CourseStatus.DRAFT);
        metrics.put("publishedCourses", publishedCourses);
        metrics.put("draftCourses", draftCourses);

        // Order & Revenue metrics
        long totalOrders = ordersRepository.count();
        metrics.put("totalOrders", totalOrders);

        Double totalRevenue = 0.0;
        try {
            totalRevenue = ordersRepository.calculateTotalRevenue();
            if (totalRevenue == null) totalRevenue = 0.0;
        } catch (Exception e) {
            // Method might not exist yet or data parsing issue
        }
        metrics.put("totalRevenue", totalRevenue);

        // CRM metrics
        long newLeads = 0;
        try {
            newLeads = leadRepository.countByStatus(LeadStatus.NEW);
        } catch (Exception e) {
            // Table might not exist yet on first run
        }
        metrics.put("newLeads", newLeads);

        long pendingEnquiries = 0;
        try {
            pendingEnquiries = enquiryRepository.countByStatus(EnquiryStatus.NEW);
        } catch (Exception e) {
            // Table might not exist yet on first run
        }
        metrics.put("pendingEnquiries", pendingEnquiries);

        return metrics;
    }

    public List<Map<String, String>> getRecentOrders(int limit) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            List<Orders> orders = ordersRepository.findTop10ByOrderByIdDesc();
            for (Orders o : orders) {
                if (result.size() >= limit) break;
                Map<String, String> item = new HashMap<>();
                item.put("id", String.valueOf(o.getId()));
                item.put("courseName", o.getCourseName() != null ? o.getCourseName() : "—");
                item.put("userEmail", o.getUserEmail() != null ? o.getUserEmail() : "—");
                item.put("amount", o.getCourseAmount() != null ? o.getCourseAmount() : "0");
                item.put("date", o.getDateOfPurchase() != null ? o.getDateOfPurchase() : "—");
                item.put("orderId", o.getOrderId() != null ? o.getOrderId() : "—");
                result.add(item);
            }
        } catch (Exception e) {
            // Graceful fallback
        }
        return result;
    }

    public List<Map<String, String>> getRecentStudents(int limit) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            List<User> users = userRepository.findTop10ByOrderByIdDesc();
            for (User u : users) {
                if (result.size() >= limit) break;
                Map<String, String> item = new HashMap<>();
                item.put("id", String.valueOf(u.getId()));
                item.put("name", u.getName() != null ? u.getName() : "—");
                item.put("email", u.getEmail() != null ? u.getEmail() : "—");
                item.put("city", u.getCity() != null ? u.getCity() : "—");
                item.put("status", u.isBanStatus() ? "Banned" : "Active");
                result.add(item);
            }
        } catch (Exception e) {
            // Graceful fallback
        }
        return result;
    }

    public List<Map<String, String>> getRecentLeads(int limit) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            List<Lead> leads = leadRepository.findTop10ByOrderByCreatedAtDesc();
            for (Lead l : leads) {
                if (result.size() >= limit) break;
                Map<String, String> item = new HashMap<>();
                item.put("id", String.valueOf(l.getId()));
                item.put("name", l.getName() != null ? l.getName() : "—");
                item.put("source", l.getSource() != null ? l.getSource() : "—");
                item.put("status", l.getStatus() != null ? l.getStatus().name() : "—");
                item.put("createdAt", l.getCreatedAt() != null ? l.getCreatedAt().toLocalDate().toString() : "—");
                result.add(item);
            }
        } catch (Exception e) {
            // Graceful fallback
        }
        return result;
    }

    public List<Map<String, String>> getAttentionItems() {
        List<Map<String, String>> items = new ArrayList<>();

        // Pending enquiries
        try {
            long pendingEnq = enquiryRepository.countByStatus(EnquiryStatus.NEW);
            if (pendingEnq > 0) {
                Map<String, String> item = new HashMap<>();
                item.put("icon", "bi-question-circle-fill");
                item.put("color", "orange");
                item.put("label", pendingEnq + " pending enquir" + (pendingEnq == 1 ? "y" : "ies"));
                item.put("url", "/admin/enquiries");
                items.add(item);
            }
        } catch (Exception e) {}

        // Draft/unpublished courses
        try {
            long drafts = courseRepository.countByStatus(CourseStatus.DRAFT);
            if (drafts > 0) {
                Map<String, String> item = new HashMap<>();
                item.put("icon", "bi-play-btn");
                item.put("color", "blue");
                item.put("label", drafts + " unpublished course" + (drafts == 1 ? "" : "s"));
                item.put("url", "/admin/courses?status=DRAFT");
                items.add(item);
            }
        } catch (Exception e) {}

        // New leads needing attention
        try {
            long newLeads = leadRepository.countByStatus(LeadStatus.NEW);
            if (newLeads > 0) {
                Map<String, String> item = new HashMap<>();
                item.put("icon", "bi-funnel-fill");
                item.put("color", "purple");
                item.put("label", newLeads + " new lead" + (newLeads == 1 ? "" : "s") + " awaiting contact");
                item.put("url", "/admin/leads?status=NEW");
                items.add(item);
            }
        } catch (Exception e) {}

        // Banned students
        try {
            long banned = userRepository.count() - userRepository.countByBanStatusFalse();
            if (banned > 0) {
                Map<String, String> item = new HashMap<>();
                item.put("icon", "bi-person-x-fill");
                item.put("color", "red");
                item.put("label", banned + " banned student" + (banned == 1 ? "" : "s"));
                item.put("url", "/admin/students");
                items.add(item);
            }
        } catch (Exception e) {}

        return items;
    }
}
