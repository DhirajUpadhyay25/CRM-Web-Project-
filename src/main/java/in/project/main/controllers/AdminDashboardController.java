package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import in.project.main.services.DashboardService;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String openAdminDashboard(Model model) {
        // KPI metrics
        Map<String, Object> metrics = dashboardService.getAdminDashboardMetrics();
        model.addAllAttributes(metrics);

        // Recent activity
        model.addAttribute("recentOrders", dashboardService.getRecentOrders(5));
        model.addAttribute("recentStudents", dashboardService.getRecentStudents(5));
        model.addAttribute("recentLeads", dashboardService.getRecentLeads(5));

        // Attention items
        model.addAttribute("attentionItems", dashboardService.getAttentionItems());

        return "admin/dashboard";
    }
}
