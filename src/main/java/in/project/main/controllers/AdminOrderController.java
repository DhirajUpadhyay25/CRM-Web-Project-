package in.project.main.controllers;

import java.io.PrintWriter;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.*;
import in.project.main.repositories.*;
import in.project.main.services.CourseService;
import in.project.main.services.PaymentService;
import in.project.main.services.RefundService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private RefundService refundService;

    @GetMapping
    public String listOrders(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            Model model) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("id").descending());
        Page<Orders> ordersPage;

        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim() : null;

        if (cleanSearch != null) {
            ordersPage = ordersRepository.searchOrders(cleanSearch, pageable);
        } else {
            ordersPage = ordersRepository.findAll(pageable);
        }

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalRevenue", ordersRepository.calculateTotalRevenue());
        model.addAttribute("totalOrders", ordersRepository.count());

        // For manual order creation modal
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("students", userRepository.findAll());

        return "admin/commerce/orders/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Order not found");
            return "redirect:/admin/orders";
        }

        Course course = courseService.getCourseDetails(order.getCourseName());
        User user = userRepository.findByEmail(order.getUserEmail());
        
        Enrollment enrollment = null;
        if (course != null && user != null) {
            enrollment = enrollmentRepository.findByUserEmailAndCourseId(user.getEmail(), course.getId()).orElse(null);
        }

        Refund refund = refundRepository.findByOrderId(order.getOrderId());
        Payment payment = paymentRepository.findByOrderId(order.getOrderId());

        model.addAttribute("order", order);
        model.addAttribute("course", course);
        model.addAttribute("user", user);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("refund", refund);
        model.addAttribute("payment", payment);

        return "admin/commerce/orders/detail";
    }

    @PostMapping("/create")
    public String createManualOrder(
            @RequestParam("userEmail") String userEmail,
            @RequestParam("courseName") String courseName,
            @RequestParam("amount") String amount,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(name = "notes", required = false) String notes,
            Principal principal,
            RedirectAttributes ra) {

        String adminEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Payment payment = paymentService.recordManualPayment(null, userEmail, courseName, amount, paymentMethod, notes, adminEmail);
            ra.addFlashAttribute("successMsg", "Sales order and enrollment created successfully for " + userEmail + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to create order: " + e.getMessage());
        }

        return "redirect:/admin/orders";
    }

    @PostMapping("/refunds/{id}/approve")
    public String approveRefund(
            @PathVariable("id") Long id,
            @RequestParam(name = "adminNote", required = false) String adminNote,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String adminEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Refund refund = refundService.approveAndProcessRefund(id, adminEmail, adminNote);
            Orders order = ordersRepository.findByOrderId(refund.getOrderId());
            redirectAttributes.addFlashAttribute("successMsg", "Refund approved successfully. Order #" + refund.getOrderId() + " refunded and enrollment cancelled.");
            return "redirect:/admin/orders" + (order != null ? "/" + order.getId() : "");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to approve refund: " + e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    @PostMapping("/refunds/{id}/reject")
    public String rejectRefund(
            @PathVariable("id") Long id,
            @RequestParam(name = "rejectionReason", required = false) String rejectionReason,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String adminEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            Refund refund = refundService.rejectRefund(id, adminEmail, rejectionReason);
            Orders order = ordersRepository.findByOrderId(refund.getOrderId());
            redirectAttributes.addFlashAttribute("successMsg", "Refund request declined.");
            return "redirect:/admin/orders" + (order != null ? "/" + order.getId() : "");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to reject refund: " + e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    @GetMapping("/export")
    public void exportOrdersCsv(
            @RequestParam(name = "search", required = false) String search,
            HttpServletResponse response) {

        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"orders_export_" + System.currentTimeMillis() + ".csv\"");

            Pageable unpaged = PageRequest.of(0, 5000, Sort.by("id").descending());
            Page<Orders> page;
            if (search != null && !search.trim().isEmpty()) {
                page = ordersRepository.searchOrders(search.trim(), unpaged);
            } else {
                page = ordersRepository.findAll(unpaged);
            }

            PrintWriter writer = response.getWriter();
            writer.println("Order ID,Student Email,Course Name,Amount,Status,Date Of Purchase,Payment ID,Coupon Code,Discount Amount");

            for (Orders o : page.getContent()) {
                writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        o.getOrderId() != null ? o.getOrderId() : "",
                        o.getUserEmail() != null ? o.getUserEmail() : "",
                        o.getCourseName() != null ? o.getCourseName().replace("\"", "\"\"") : "",
                        o.getCourseAmount() != null ? o.getCourseAmount() : "0",
                        o.getStatus() != null ? o.getStatus() : "COMPLETED",
                        o.getDateOfPurchase() != null ? o.getDateOfPurchase() : "",
                        o.getPaymentId() != null ? o.getPaymentId() : "",
                        o.getCouponCode() != null ? o.getCouponCode() : "",
                        o.getDiscountAmount() != null ? o.getDiscountAmount() : "0"
                ));
            }
            writer.flush();
        } catch (Exception ignored) {}
    }
}
