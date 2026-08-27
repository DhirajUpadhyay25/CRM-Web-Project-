package in.project.main.api;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;

import in.project.main.entities.Course;
import in.project.main.entities.Orders;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.repositories.OrdersRepository;
import in.project.main.services.CourseService;
import in.project.main.services.OrderService;
import in.project.main.util.DateTimeUtil;

@RestController
@RequestMapping("/api")
public class OrdersApi {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private OrdersRepository ordersRepository;

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    // ✅ 1. CREATE ORDER (Price Integrity Enforced Server-Side)
    @PostMapping("/createOrder")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload) {
        try {
            String courseName = (String) payload.get("courseName");
            if (courseName == null || courseName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Course name is required"));
            }

            Course course = courseService.getCourseDetails(courseName.trim());
            if (course == null || course.getStatus() != CourseStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or unpublished course"));
            }

            BigDecimal effectivePrice = course.getEffectivePrice();
            if (effectivePrice.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "This course is free. Please use free enrollment."));
            }

            // Amount in paise (authoritative calculation)
            long amountInPaise = effectivePrice.multiply(BigDecimal.valueOf(100)).longValue();

            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = client.orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("courseName", course.getName());
            response.put("effectivePrice", effectivePrice.toPlainString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to initiate payment: " + e.getMessage()));
        }
    }

    // ✅ 2. VERIFY PAYMENT (Cryptographic Signature Verification & Price Integrity)
    @PostMapping("/verifyPayment")
    public ResponseEntity<?> verifyPayment(@RequestBody Orders orders, Principal principal) {
        try {
            if (orders.getOrderId() == null || orders.getPaymentId() == null || orders.getSignature() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing payment verification parameters"));
            }

            Course course = courseService.getCourseDetails(orders.getCourseName());
            if (course == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Course not found"));
            }

            // Cryptographic HMAC SHA256 Signature Verification
            String generatedSignature = hmacSHA256(
                    orders.getOrderId() + "|" + orders.getPaymentId(),
                    keySecret
            );

            if (!generatedSignature.equals(orders.getSignature())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid payment signature"));
            }

            // Set authoritative user email and price
            String userEmail = (principal != null) ? principal.getName() : orders.getUserEmail();
            orders.setUserEmail(userEmail);
            orders.setCourseAmount(course.getEffectivePrice().toPlainString());
            orders.setDateOfPurchase(DateTimeUtil.getCurrentDateTimeFormatted());

            // Prevent duplicate insertion if already recorded
            if (!ordersRepository.existsByUserEmailAndCourseName(userEmail, course.getName())) {
                orderService.storeUserOrders(orders);
            }

            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment verified & order stored successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Payment verification error: " + e.getMessage()));
        }
    }

    // ✅ HMAC SHA256 Helper Method
    private String hmacSHA256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes());

        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}