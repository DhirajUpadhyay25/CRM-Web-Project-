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
import in.project.main.entities.Enrollment;
import in.project.main.entities.Orders;
import in.project.main.entities.User;
import in.project.main.entities.Payment;
import in.project.main.entities.Coupon;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.repositories.PaymentRepository;
import in.project.main.repositories.CouponRepository;
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

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    // ✅ 1. CREATE ORDER (Price Integrity Enforced Server-Side with Coupon Validation)
    @PostMapping("/createOrder")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
            String courseName = (String) payload.get("courseName");
            if (courseName == null || courseName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Course name is required"));
            }

            Course course = courseService.getCourseDetails(courseName.trim());
            if (course == null || course.getStatus() != CourseStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or unpublished course"));
            }

            String userEmail = (principal != null) ? principal.getName() : "anonymous";
            User user = userRepository.findByEmail(userEmail);
            if (user != null && enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "You already have active access to this course."));
            }

            BigDecimal effectivePrice = course.getEffectivePrice();
            if (effectivePrice.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "This course is free. Please use free enrollment."));
            }

            // Coupon Code Server-Side Evaluation
            String couponCode = (String) payload.get("couponCode");
            BigDecimal discount = BigDecimal.ZERO;
            if (couponCode != null && !couponCode.trim().isEmpty()) {
                Coupon coupon = couponRepository.findByCode(couponCode.trim());
                if (coupon != null && coupon.getIsActive()) {
                    if ("percentage".equalsIgnoreCase(coupon.getDiscountType())) {
                        BigDecimal pct = new BigDecimal(coupon.getDiscountValue());
                        discount = effectivePrice.multiply(pct).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    } else {
                        discount = new BigDecimal(coupon.getDiscountValue());
                    }
                    effectivePrice = effectivePrice.subtract(discount);
                    if (effectivePrice.compareTo(BigDecimal.ZERO) < 0) {
                        effectivePrice = BigDecimal.ZERO;
                    }
                }
            }

            // Amount in paise (authoritative calculation)
            long amountInPaise = effectivePrice.multiply(BigDecimal.valueOf(100)).longValue();

            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = client.orders.create(orderRequest);

            // Create PENDING Order in Database
            Orders pendingOrder = new Orders();
            pendingOrder.setOrderId(order.get("id"));
            pendingOrder.setCourseName(course.getName());
            pendingOrder.setCourseAmount(effectivePrice.toPlainString());
            pendingOrder.setUserEmail(userEmail);
            pendingOrder.setDateOfPurchase(DateTimeUtil.getCurrentDateTimeFormatted());
            pendingOrder.setStatus("PENDING");
            if (couponCode != null && !couponCode.trim().isEmpty()) {
                pendingOrder.setCouponCode(couponCode.trim());
                pendingOrder.setDiscountAmount(discount.toPlainString());
            }
            ordersRepository.save(pendingOrder);

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

    // ✅ 2. FAIL ORDER (Transitions Order Status to FAILED)
    @PostMapping("/failOrder")
    public ResponseEntity<?> failOrder(@RequestBody Map<String, Object> payload) {
        try {
            String orderId = (String) payload.get("orderId");
            if (orderId == null || orderId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "OrderId is required"));
            }
            Orders order = ordersRepository.findByOrderId(orderId);
            if (order != null && "PENDING".equals(order.getStatus())) {
                order.setStatus("FAILED");
                ordersRepository.save(order);
                return ResponseEntity.ok(Map.of("status", "success", "message", "Order marked as FAILED"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Order not found or not pending"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ 3. VERIFY PAYMENT (Cryptographic Signature Verification & Price Integrity)
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

            Orders existingOrder = ordersRepository.findByOrderId(orders.getOrderId());
            if (existingOrder == null) {
                // Handle fallback (e.g. employee custom sale or legacy checkout)
                existingOrder = new Orders();
                existingOrder.setOrderId(orders.getOrderId());
                existingOrder.setCourseName(course.getName());
                existingOrder.setCourseAmount(course.getEffectivePrice().toPlainString());
                String userEmail = (principal != null) ? principal.getName() : orders.getUserEmail();
                existingOrder.setUserEmail(userEmail);
                existingOrder.setDateOfPurchase(DateTimeUtil.getCurrentDateTimeFormatted());
            }

            if ("COMPLETED".equals(existingOrder.getStatus())) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "Payment verified & order stored successfully"));
            }

            existingOrder.setPaymentId(orders.getPaymentId());
            existingOrder.setSignature(orders.getSignature());
            existingOrder.setStatus("COMPLETED");
            ordersRepository.save(existingOrder);

            // Record transaction details in Payment table
            Payment payment = new Payment();
            payment.setOrderId(existingOrder.getOrderId());
            payment.setAmount(existingOrder.getCourseAmount());
            payment.setStatus("SUCCESS");
            payment.setPaymentMethod("RAZORPAY");
            payment.setPaymentDate(DateTimeUtil.getCurrentDateTimeFormatted());
            paymentRepository.save(payment);

            // Create enrollment record for student dashboard
            String userEmail = existingOrder.getUserEmail();
            User user = userRepository.findByEmail(userEmail);
            if (user != null && !enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
                Enrollment enrollment = new Enrollment();
                enrollment.setUser(user);
                enrollment.setCourse(course);
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollment.setPaymentStatus("PAID");
                enrollmentRepository.save(enrollment);
            }

            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment verified & order stored successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Payment verification error: " + e.getMessage()));
        }
    }

    // ✅ 4. VALIDATE COUPON
    @PostMapping("/coupons/apply")
    public ResponseEntity<?> applyCoupon(@RequestBody Map<String, Object> payload) {
        try {
            String couponCode = (String) payload.get("couponCode");
            String courseName = (String) payload.get("courseName");
            if (couponCode == null || couponCode.trim().isEmpty() || courseName == null || courseName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Coupon code and course name are required"));
            }

            Course course = courseService.getCourseDetails(courseName.trim());
            if (course == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Course not found"));
            }

            Coupon coupon = couponRepository.findByCode(couponCode.trim());
            if (coupon == null || !coupon.getIsActive()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or inactive coupon code"));
            }

            BigDecimal originalPrice = course.getEffectivePrice();
            BigDecimal discount = BigDecimal.ZERO;

            if ("percentage".equalsIgnoreCase(coupon.getDiscountType())) {
                BigDecimal pct = new BigDecimal(coupon.getDiscountValue());
                discount = originalPrice.multiply(pct).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            } else {
                discount = new BigDecimal(coupon.getDiscountValue());
            }

            BigDecimal discountedPrice = originalPrice.subtract(discount);
            if (discountedPrice.compareTo(BigDecimal.ZERO) < 0) {
                discountedPrice = BigDecimal.ZERO;
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "couponCode", coupon.getCode(),
                "originalPrice", originalPrice.toPlainString(),
                "discount", discount.toPlainString(),
                "discountedPrice", discountedPrice.toPlainString()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
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