package in.project.main.controllers;

import in.project.main.entities.*;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.*;
import in.project.main.services.LearningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportsController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private EnrollmentRepository enrollmentRepo;

    @Autowired
    private OrdersRepository ordersRepo;

    @Autowired
    private QuizRepository quizRepo;

    @Autowired
    private QuizAttemptRepository attemptRepo;

    @Autowired
    private StudentActivityRepository activityRepo;

    @Autowired
    private LearningService learningService;

    @GetMapping
    public String viewReportsDashboard(Model model) {
        
        // 1. STUDENT REPORT
        List<User> students = userRepo.findAll();
        List<Map<String, Object>> studentReport = new ArrayList<>();
        for (User u : students) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("city", u.getCity() != null ? u.getCity() : "N/A");
            
            long active = enrollmentRepo.countByUserEmailAndStatus(u.getEmail(), EnrollmentStatus.ACTIVE);
            long completed = enrollmentRepo.countByUserEmailAndStatus(u.getEmail(), EnrollmentStatus.COMPLETED);
            map.put("enrolledCount", active + completed);
            map.put("completedCount", completed);
            
            List<StudentActivity> acts = activityRepo.findByUserEmailOrderByCreatedAtDesc(u.getEmail(), org.springframework.data.domain.PageRequest.of(0, 1));
            if (!acts.isEmpty()) {
                StudentActivity last = acts.get(0);
                map.put("lastActivityTime", last.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
                map.put("lastActivityDesc", last.getDescription());
            } else {
                map.put("lastActivityTime", "No activity");
                map.put("lastActivityDesc", "—");
            }
            studentReport.add(map);
        }

        // 2. COURSE REPORT
        List<Course> courses = courseRepo.findAll();
        List<Map<String, Object>> courseReport = new ArrayList<>();
        for (Course c : courses) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", c.getName());
            map.put("level", c.getLevel().name());
            map.put("price", c.isFree() ? "Free" : "₹" + c.getEffectivePrice().toPlainString());

            long totalEnrolls = enrollmentRepo.countByCourseId(c.getId());
            long completedEnrolls = enrollmentRepo.countByCourseIdAndStatus(c.getId(), EnrollmentStatus.COMPLETED);
            map.put("enrollmentsCount", totalEnrolls);
            map.put("completionsCount", completedEnrolls);
            
            double completionRate = totalEnrolls > 0 ? (completedEnrolls * 100.0) / totalEnrolls : 0.0;
            map.put("completionRate", Math.round(completionRate * 10.0) / 10.0);

            // Average Progress
            List<Enrollment> enrollments = enrollmentRepo.findByCourseId(c.getId());
            double sumProgress = 0.0;
            long studentCount = 0;
            for (Enrollment e : enrollments) {
                if (e.getStatus() == EnrollmentStatus.ACTIVE || e.getStatus() == EnrollmentStatus.COMPLETED) {
                    sumProgress += learningService.getCourseProgressPercent(e.getUser().getEmail(), c.getId());
                    studentCount++;
                }
            }
            double avgProgress = studentCount > 0 ? sumProgress / studentCount : 0.0;
            map.put("avgProgress", Math.round(avgProgress * 10.0) / 10.0);
            
            courseReport.add(map);
        }

        // 3. REVENUE REPORT
        List<Orders> orders = ordersRepo.findAll();
        Map<String, Map<String, Object>> revenueMap = new HashMap<>();
        
        // Initialize for all courses
        for (Course c : courses) {
            if (!c.isFree()) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", c.getName());
                map.put("salesCount", 0L);
                map.put("totalRevenue", BigDecimal.ZERO);
                revenueMap.put(c.getName(), map);
            }
        }

        for (Orders o : orders) {
            if (o.getCourseAmount() != null) {
                try {
                    BigDecimal amt = new BigDecimal(o.getCourseAmount());
                    if (amt.compareTo(BigDecimal.ZERO) > 0) {
                        Map<String, Object> map = revenueMap.computeIfAbsent(o.getCourseName(), k -> {
                            Map<String, Object> newMap = new HashMap<>();
                            newMap.put("name", o.getCourseName());
                            newMap.put("salesCount", 0L);
                            newMap.put("totalRevenue", BigDecimal.ZERO);
                            return newMap;
                        });
                        map.put("salesCount", (Long) map.get("salesCount") + 1);
                        map.put("totalRevenue", ((BigDecimal) map.get("totalRevenue")).add(amt));
                    }
                } catch (Exception ignored) {}
            }
        }
        List<Map<String, Object>> revenueReport = new ArrayList<>(revenueMap.values());
        revenueReport.sort((o1, o2) -> ((BigDecimal) o2.get("totalRevenue")).compareTo((BigDecimal) o1.get("totalRevenue")));

        // 4. ASSESSMENT REPORT
        List<Quiz> quizzes = quizRepo.findAll();
        List<Map<String, Object>> quizReport = new ArrayList<>();
        for (Quiz q : quizzes) {
            Map<String, Object> map = new HashMap<>();
            map.put("title", q.getTitle());
            
            Course c = courseRepo.findById(q.getCourseId()).orElse(null);
            map.put("courseName", c != null ? c.getName() : "Unknown Course");
            
            List<QuizAttempt> attempts = attemptRepo.findByQuizId(q.getId());
            long totalAttempts = attempts.size();
            long passCount = attempts.stream().filter(QuizAttempt::isPassed).count();
            double passRate = totalAttempts > 0 ? (passCount * 100.0) / totalAttempts : 0.0;
            double avgScore = totalAttempts > 0 ? attempts.stream().mapToInt(QuizAttempt::getScore).average().orElse(0.0) : 0.0;

            map.put("attemptsCount", totalAttempts);
            map.put("passCount", passCount);
            map.put("passRate", Math.round(passRate * 10.0) / 10.0);
            map.put("avgScore", Math.round(avgScore * 10.0) / 10.0);

            quizReport.add(map);
        }

        model.addAttribute("studentReport", studentReport);
        model.addAttribute("courseReport", courseReport);
        model.addAttribute("revenueReport", revenueReport);
        model.addAttribute("quizReport", quizReport);

        return "admin/reports/index";
    }
}
