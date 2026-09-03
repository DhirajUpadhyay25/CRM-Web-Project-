package in.project.main.repositories;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByOrderId(String orderId);
    List<Payment> findByUserEmail(String userEmail);
    long countByStatus(String status);

    @Query("SELECT p FROM Payment p WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(p.orderId) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.paymentId) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.userEmail) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR p.status = :status) AND " +
           "(:paymentMethod IS NULL OR :paymentMethod = '' OR p.paymentMethod = :paymentMethod)")
    Page<Payment> searchPayments(
            @Param("search") String search,
            @Param("status") String status,
            @Param("paymentMethod") String paymentMethod,
            Pageable pageable);
}
