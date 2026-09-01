package in.project.main.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;

import in.project.main.entities.Coupon;
import in.project.main.entities.Course;
import in.project.main.entities.Orders;
import in.project.main.entities.User;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.repositories.CouponRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.CourseService;
import in.project.main.services.OrderService;
import in.project.main.util.DateTimeUtil;

@RestController
@RequestMapping("/api")
public class OrdersApi {

    private static final Logger log = LoggerFactory.getLogger(OrdersApi.class);

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
    private CouponRepository couponRepository;

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    // ------------------------------------------------------------------
    // 1. CREATE ORDER - price is always computed server-side
    // ------------------------------------------------------------------
    @PostMapping("/createOrder")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Please sign in before starting a purchase."));
        }

        try {
            String courseName = (String) payload.get("courseName");
            if (courseName == null || courseName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Course name is required"));
            }

            Course course = courseService.getCourseDetails(courseName.trim());
            if (course == null || course.getStatus() != CourseStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or unpublished course"));
            }

            // Only a student account can hold an enrollment, so refuse to take money from a
            // login that could never be granted access to the course it just paid for.
            String userEmail = principal.getName();
            User user = userRepository.findByEmail(userEmail);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "This account cannot purchase courses. Please sign in with a student account."));
            }

            if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "You already have active access to this course."));
            }

            BigDecimal listPrice = course.getEffectivePrice();
            if (listPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "This course is free. Please use free enrollment."));
            }

            String couponCode = normalise((String) payload.get("couponCode"));
            BigDecimal discount = BigDecimal.ZERO;
            if (couponCode != null) {
                // Reject an unusable coupon rather than silently charging full price: the
                // checkout page has already shown the buyer a discounted total.
                discount = resolveCouponDiscount(couponCode, listPrice);
            }

            BigDecimal payable = listPrice.subtract(discount);
            if (payable.compareTo(BigDecimal.ZERO) < 0) {
                payable = BigDecimal.ZERO;
            }

            long amountInPaise = payable.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
            if (amountInPaise <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "This coupon reduces the price to zero. Please use free enrollment."));
            }

            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = client.orders.create(orderRequest);

            Orders pendingOrder = new Orders();
            pendingOrder.setOrderId(order.get("id"));
            pendingOrder.setCourseName(course.getName());
            pendingOrder.setCourseAmount(payable.toPlainString());
            pendingOrder.setUserEmail(userEmail);
            pendingOrder.setDateOfPurchase(DateTimeUtil.getCurrentDateTimeFormatted());
            pendingOrder.setStatus("PENDING");
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                pendingOrder.setCouponCode(couponCode);
                pendingOrder.setDiscountAmount(discount.toPlainString());
            }
            ordersRepository.save(pendingOrder);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("courseName", course.getName());
            response.put("effectivePrice", payable.toPlainString());
            return ResponseEntity.ok(response);

        } catch (CouponRejectedException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create order for course '{}'", payload.get("courseName"), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "We could not start the payment. Please try again."));
        }
    }

    // ------------------------------------------------------------------
    // 2. FAIL ORDER - only the buyer may abandon their own pending order
    // ------------------------------------------------------------------
    @PostMapping("/failOrder")
    public ResponseEntity<?> failOrder(@RequestBody Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not signed in"));
        }

        try {
            String orderId = normalise((String) payload.get("orderId"));
            if (orderId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "OrderId is required"));
            }

            Orders order = ordersRepository.findByOrderId(orderId);
            if (order == null || !principal.getName().equals(order.getUserEmail())) {
                // Same answer for "does not exist" and "not yours" so this cannot be used to
                // discover other people's order ids.
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Order not found"));
            }

            if (!"PENDING".equals(order.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Order is no longer pending"));
            }

            order.setStatus("FAILED");
            ordersRepository.save(order);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Order marked as FAILED"));

        } catch (Exception e) {
            log.error("Failed to mark order as failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not update the order."));
        }
    }

    // ------------------------------------------------------------------
    // 3. VERIFY PAYMENT
    //
    // Everything that decides what the buyer receives is read from our own PENDING order
    // row, which was written by createOrder. The request body is trusted only for the
    // three Razorpay handshake values, and those are accepted solely because the HMAC
    // signature proves Razorpay produced them with our secret.
    // ------------------------------------------------------------------
    @PostMapping("/verifyPayment")
    public ResponseEntity<?> verifyPayment(@RequestBody Orders orders, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not signed in"));
        }

        try {
            String orderId = normalise(orders.getOrderId());
            String paymentId = normalise(orders.getPaymentId());
            String signature = normalise(orders.getSignature());
            if (orderId == null || paymentId == null || signature == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing payment verification parameters"));
            }

            // Check the signature before touching the database so this endpoint cannot be
            // used to probe which order ids exist.
            String expectedSignature = hmacSHA256(orderId + "|" + paymentId, keySecret);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Rejected payment verification with an invalid signature for order {}", orderId);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid payment signature"));
            }

            Orders storedOrder = ordersRepository.findByOrderId(orderId);
            if (storedOrder == null) {
                // No fallback order creation. An order id we never issued must never be able
                // to mint a COMPLETED order for a course and email chosen by the caller.
                log.warn("Payment verification referenced unknown order {}", orderId);
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown order"));
            }

            if (!principal.getName().equals(storedOrder.getUserEmail())) {
                log.warn("User {} attempted to verify an order belonging to someone else", principal.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "This order belongs to another account"));
            }

            // The course comes from our order row, never from the request body.
            Course course = courseService.getCourseDetails(storedOrder.getCourseName());
            if (course == null) {
                log.error("Order {} references course '{}' which no longer exists", orderId, storedOrder.getCourseName());
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "This course is no longer available. Please contact support."));
            }

            assertPaymentMatchesOrder(paymentId, storedOrder);

            boolean settled = orderService.settleVerifiedPayment(storedOrder, course, paymentId, signature);
            if (settled) {
                log.info("Settled order {} for course '{}'", orderId, course.getName());
            }

            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment verified & order stored successfully"));

        } catch (PaymentMismatchException e) {
            log.error("Refusing to settle payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Payment could not be matched to this order."));
        } catch (Exception e) {
            log.error("Payment verification failed for order {}", orders.getOrderId(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "We could not confirm the payment. Please contact support before paying again."));
        }
    }

    // ------------------------------------------------------------------
    // 4. VALIDATE COUPON (preview only - createOrder re-evaluates)
    // ------------------------------------------------------------------
    @PostMapping("/coupons/apply")
    public ResponseEntity<?> applyCoupon(@RequestBody Map<String, Object> payload) {
        try {
            String couponCode = normalise((String) payload.get("couponCode"));
            String courseName = normalise((String) payload.get("courseName"));
            if (couponCode == null || courseName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Coupon code and course name are required"));
            }

            Course course = courseService.getCourseDetails(courseName);
            if (course == null || course.getStatus() != CourseStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(Map.of("error", "Course not found"));
            }

            BigDecimal originalPrice = course.getEffectivePrice();
            BigDecimal discount = resolveCouponDiscount(couponCode, originalPrice);

            BigDecimal discountedPrice = originalPrice.subtract(discount);
            if (discountedPrice.compareTo(BigDecimal.ZERO) < 0) {
                discountedPrice = BigDecimal.ZERO;
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "couponCode", couponCode,
                    "originalPrice", originalPrice.toPlainString(),
                    "discount", discount.toPlainString(),
                    "discountedPrice", discountedPrice.toPlainString()));

        } catch (CouponRejectedException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Coupon validation failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not validate that coupon."));
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Single source of truth for what a coupon is worth. Both the preview endpoint and the
     * order-creation path go through this, so a coupon can never be priced one way for the
     * quote and another way for the charge.
     */
    private BigDecimal resolveCouponDiscount(String couponCode, BigDecimal price) {
        Coupon coupon = couponRepository.findByCode(couponCode);
        if (coupon == null || !Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new CouponRejectedException("Invalid or inactive coupon code");
        }

        if (isExpired(coupon.getExpiryDate())) {
            throw new CouponRejectedException("This coupon has expired");
        }

        BigDecimal value;
        try {
            value = new BigDecimal(normalise(coupon.getDiscountValue()));
        } catch (Exception e) {
            log.error("Coupon '{}' has a non-numeric discount value '{}'", coupon.getCode(), coupon.getDiscountValue());
            throw new CouponRejectedException("This coupon is misconfigured");
        }

        // A negative discount would increase the price, so treat it as misconfiguration
        // rather than applying it.
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Coupon '{}' has a negative discount value '{}'", coupon.getCode(), value);
            throw new CouponRejectedException("This coupon is misconfigured");
        }

        BigDecimal discount;
        if ("percentage".equalsIgnoreCase(coupon.getDiscountType())) {
            if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                value = BigDecimal.valueOf(100);
            }
            discount = price.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = value.setScale(2, RoundingMode.HALF_UP);
        }

        if (discount.compareTo(price) > 0) {
            discount = price;
        }
        return discount;
    }

    /**
     * Coupon expiry is stored as a String, so tolerate both a plain date and a date-time.
     * An unparseable value is logged and treated as "no expiry" rather than blocking a
     * coupon the admin believes is live.
     */
    private boolean isExpired(String expiryDate) {
        String raw = normalise(expiryDate);
        if (raw == null) {
            return false;
        }
        int timeSeparator = raw.indexOf('T');
        String datePart = (timeSeparator > 0) ? raw.substring(0, timeSeparator) : raw;
        try {
            return LocalDate.parse(datePart).isBefore(LocalDate.now());
        } catch (Exception e) {
            log.warn("Could not read coupon expiry date '{}' - treating the coupon as non-expiring", raw);
            return false;
        }
    }

    /**
     * Defence in depth on top of the signature: ask Razorpay what it actually captured and
     * confirm it belongs to this order and matches the amount we asked for.
     *
     * A definite contradiction stops the settlement. If Razorpay itself is unreachable we
     * log and continue, because the signature has already proven authenticity and failing
     * here would strand a buyer who has genuinely paid.
     */
    private void assertPaymentMatchesOrder(String paymentId, Orders storedOrder) {
        com.razorpay.Payment remotePayment;
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            remotePayment = client.payments.fetch(paymentId);
        } catch (Exception e) {
            log.warn("Could not fetch payment {} from Razorpay for cross-checking; relying on signature only",
                    paymentId, e);
            return;
        }

        try {
            String remoteOrderId = remotePayment.get("order_id");
            if (remoteOrderId != null && !remoteOrderId.equals(storedOrder.getOrderId())) {
                throw new PaymentMismatchException("payment " + paymentId + " belongs to order "
                        + remoteOrderId + ", not " + storedOrder.getOrderId());
            }

            String status = remotePayment.get("status");
            if (status != null && !("captured".equals(status) || "authorized".equals(status))) {
                throw new PaymentMismatchException("payment " + paymentId + " is in state '" + status + "'");
            }

            long expectedPaise = new BigDecimal(storedOrder.getCourseAmount())
                    .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
            Number paidPaise = remotePayment.get("amount");
            if (paidPaise != null && paidPaise.longValue() != expectedPaise) {
                throw new PaymentMismatchException("payment " + paymentId + " paid " + paidPaise.longValue()
                        + " paise but order " + storedOrder.getOrderId() + " expects " + expectedPaise);
            }
        } catch (PaymentMismatchException e) {
            throw e;
        } catch (Exception e) {
            // An unreadable field is a reason to investigate, not a reason to withhold access
            // from someone whose signed payment we have already verified.
            log.warn("Could not fully cross-check payment {} against order {}; relying on signature only",
                    paymentId, storedOrder.getOrderId(), e);
        }
    }

    private String hmacSHA256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private String normalise(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** A coupon the buyer supplied cannot be honoured; the message is safe to show them. */
    private static class CouponRejectedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CouponRejectedException(String message) {
            super(message);
        }
    }

    /** Razorpay's record of the payment contradicts our order row. */
    private static class PaymentMismatchException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        PaymentMismatchException(String message) {
            super(message);
        }
    }
}
