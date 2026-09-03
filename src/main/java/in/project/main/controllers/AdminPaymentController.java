package in.project.main.controllers;

import java.io.PrintWriter;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Course;
import in.project.main.entities.Payment;
import in.project.main.entities.User;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.PaymentService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String listPayments(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "method", required = false) String method,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("id").descending());
        Page<Payment> paymentsPage = paymentService.getPaymentsPage(search, status, method, pageable);

        Map<String, Object> stats = paymentService.getPaymentStats();

        model.addAttribute("paymentsPage", paymentsPage);
        model.addAttribute("payments", paymentsPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("methodFilter", method);
        model.addAttribute("currentPage", page);
        model.addAttribute("stats", stats);

        // Populate dropdowns for manual payment modal
        List<Course> courses = courseRepository.findAll();
        List<User> students = userRepository.findAll();
        model.addAttribute("courses", courses);
        model.addAttribute("students", students);

        return "admin/commerce/payments/list";
    }

    @PostMapping("/manual")
    public String recordManualPayment(
            @RequestParam(name = "orderId", required = false) String orderId,
            @RequestParam("userEmail") String userEmail,
            @RequestParam("courseName") String courseName,
            @RequestParam("amount") String amount,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(name = "notes", required = false) String notes,
            Principal principal,
            RedirectAttributes ra) {

        String adminEmail = principal != null ? principal.getName() : "admin@edutake.com";

        try {
            Payment p = paymentService.recordManualPayment(orderId, userEmail, courseName, amount, paymentMethod, notes, adminEmail);
            ra.addFlashAttribute("successMsg", "Manual payment of ₹" + p.getAmount() + " recorded successfully. Course access granted to " + userEmail + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to record payment: " + e.getMessage());
        }

        return "redirect:/admin/payments";
    }

    @GetMapping("/export")
    public void exportPaymentsCsv(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "method", required = false) String method,
            HttpServletResponse response) {

        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"payments_export_" + System.currentTimeMillis() + ".csv\"");

            Pageable unpaged = PageRequest.of(0, 5000, Sort.by("id").descending());
            Page<Payment> page = paymentService.getPaymentsPage(search, status, method, unpaged);

            PrintWriter writer = response.getWriter();
            writer.println("Payment ID,Order ID,Student Email,Amount,Currency,Method,Status,Payment Date,Verified By,Notes");

            for (Payment p : page.getContent()) {
                writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        p.getPaymentId() != null ? p.getPaymentId() : "",
                        p.getOrderId() != null ? p.getOrderId() : "",
                        p.getUserEmail() != null ? p.getUserEmail() : "",
                        p.getAmount() != null ? p.getAmount() : "0",
                        p.getCurrency() != null ? p.getCurrency() : "INR",
                        p.getPaymentMethod() != null ? p.getPaymentMethod() : "DIRECT",
                        p.getStatus() != null ? p.getStatus() : "SUCCESS",
                        p.getPaymentDate() != null ? p.getPaymentDate() : "",
                        p.getVerifiedBy() != null ? p.getVerifiedBy() : "",
                        p.getNotes() != null ? p.getNotes().replace("\"", "\"\"") : ""
                ));
            }
            writer.flush();
        } catch (Exception e) {
            // ignore
        }
    }
}
