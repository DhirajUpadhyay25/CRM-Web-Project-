package in.project.main.services;

import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import in.project.main.entities.Refund;

public interface RefundService {
    Page<Refund> getRefundsPage(String search, String status, Pageable pageable);
    Refund getRefundById(Long id);
    Refund getRefundByOrderId(String orderId);
    Refund requestRefundByStudent(String userEmail, Long orderDbId, String reason, String remarks);
    Refund approveAndProcessRefund(Long refundId, String adminEmail, String adminNote);
    Refund rejectRefund(Long refundId, String adminEmail, String rejectionReason);
    Refund adminInitiateRefund(String orderId, String amount, String reason, String adminEmail);
    Map<String, Object> getRefundStats();
}
