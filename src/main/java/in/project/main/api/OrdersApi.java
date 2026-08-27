package in.project.main.api;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;

import in.project.main.entities.Orders;
import in.project.main.services.OrderService;

@RestController
@RequestMapping("/api")
public class OrdersApi {

    @Autowired
    private OrderService orderService;

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    // ✅ 1. CREATE ORDER (BEFORE PAYMENT)
    @PostMapping("/createOrder")
    public ResponseEntity<?> createOrder(@RequestBody Orders orders) throws Exception {

        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        int amount = Integer.parseInt(orders.getCourseAmount());

        orderRequest.put("amount", amount* 100); // ✅ paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());

        Order order = client.orders.create(orderRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.get("id"));
        response.put("amount", order.get("amount"));

        return ResponseEntity.ok(response);
    }

    // ✅ 2. VERIFY PAYMENT (CRITICAL)
    @PostMapping("/verifyPayment")
    public ResponseEntity<String> verifyPayment(@RequestBody Orders orders) throws Exception {
    	orders.setDateOfPurchase(
    		    java.time.LocalDateTime.now()
    		        .format(java.time.format.DateTimeFormatter
    		        		.ofPattern("dd-MM-yyyy hh:mm a"))
    		);
    	
        String generatedSignature = hmacSHA256(
                orders.getOrderId() + "|" + orders.getPaymentId(),
                keySecret
        );
        

        if (generatedSignature.equals(orders.getSignature())) {

            orderService.storeUserOrders(orders); // ✅ store ONLY after verification
            return ResponseEntity.ok("Payment verified & stored");

        } else {
            return ResponseEntity.badRequest().body("Invalid payment");
        }
    }

    // ✅ HMAC SHA256 METHOD
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