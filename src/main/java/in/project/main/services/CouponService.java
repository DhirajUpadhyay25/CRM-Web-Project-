package in.project.main.services;

import java.util.List;
import java.util.Map;
import in.project.main.entities.Coupon;

public interface CouponService {
    List<Coupon> getAllCoupons();
    Coupon getCouponById(Long id);
    Coupon createCoupon(Coupon coupon, String actorEmail);
    Coupon updateCoupon(Long id, Coupon coupon, String actorEmail);
    void deleteCoupon(Long id, String actorEmail);
    boolean toggleCouponStatus(Long id, String actorEmail);
    Map<String, Object> validateAndApplyCoupon(String code, String courseName, String userEmail);
    void incrementCouponUsage(String code);
    Map<String, Object> getCouponStats();
}
