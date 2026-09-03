package in.project.main.repositories;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Refund;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByStatus(String status);
    Refund findByOrderId(String orderId);
    List<Refund> findByUserEmail(String userEmail);
    long countByStatus(String status);

    @Query("SELECT r FROM Refund r WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(r.orderId) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.courseName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR r.status = :status)")
    Page<Refund> searchRefunds(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);
}
