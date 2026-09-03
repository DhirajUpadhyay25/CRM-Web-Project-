package in.project.main.services;

import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import in.project.main.entities.Payment;

public interface PaymentService {
    Page<Payment> getPaymentsPage(String search, String status, String paymentMethod, Pageable pageable);
    Payment getPaymentById(Long id);
    Payment getPaymentByOrderId(String orderId);
    Payment recordManualPayment(String orderId, String userEmail, String courseName, String amount, String method, String notes, String adminEmail);
    Map<String, Object> getPaymentStats();
}
