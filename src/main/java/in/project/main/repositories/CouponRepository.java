package in.project.main.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Coupon findByCode(String code);
    Coupon findByCodeIgnoreCase(String code);
    List<Coupon> findByIsActiveTrue();
    long countByIsActive(Boolean isActive);
    boolean existsByCodeIgnoreCase(String code);
}
