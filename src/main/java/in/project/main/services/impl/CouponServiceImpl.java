package in.project.main.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Coupon;
import in.project.main.entities.Course;
import in.project.main.entities.Orders;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.CouponRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.services.AuditLogService;
import in.project.main.services.CouponService;
import in.project.main.services.CourseService;

@Service
public class CouponServiceImpl implements CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponServiceImpl.class);

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private CourseService courseService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Override
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    public Coupon getCouponById(Long id) {
        return couponRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Coupon createCoupon(Coupon coupon, String actorEmail) {
        if (coupon == null || coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Coupon code is required");
        }

        String normalizedCode = coupon.getCode().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException("Coupon code '" + normalizedCode + "' already exists");
        }

        coupon.setCode(normalizedCode);
        if (coupon.getDiscountType() == null || coupon.getDiscountType().trim().isEmpty()) {
            coupon.setDiscountType("PERCENTAGE");
        } else {
            coupon.setDiscountType(coupon.getDiscountType().trim().toUpperCase());
        }

        if (coupon.getIsActive() == null) {
            coupon.setIsActive(true);
        }
        if (coupon.getUsedCount() == null) {
            coupon.setUsedCount(0);
        }

        Coupon saved = couponRepository.save(coupon);

        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "COUPON_CREATED",
                    "Admin created coupon '" + saved.getCode() + "' (" + saved.getDiscountValue() + " " + saved.getDiscountType() + ")."
            )
            .withActor(null, actorEmail, "Admin", "ADMIN")
            .withEntity("COUPON", String.valueOf(saved.getId()), saved.getCode())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        return saved;
    }

    @Override
    @Transactional
    public Coupon updateCoupon(Long id, Coupon incoming, String actorEmail) {
        Coupon existing = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        if (incoming.getCode() != null && !incoming.getCode().trim().isEmpty()) {
            String newCode = incoming.getCode().trim().toUpperCase();
            if (!newCode.equalsIgnoreCase(existing.getCode()) && couponRepository.existsByCodeIgnoreCase(newCode)) {
                throw new IllegalArgumentException("Another coupon with code '" + newCode + "' already exists");
            }
            existing.setCode(newCode);
        }

        if (incoming.getDiscountType() != null) {
            existing.setDiscountType(incoming.getDiscountType().trim().toUpperCase());
        }
        if (incoming.getDiscountValue() != null) {
            existing.setDiscountValue(incoming.getDiscountValue().trim());
        }
        existing.setMinOrderAmount(incoming.getMinOrderAmount());
        existing.setMaxDiscountCap(incoming.getMaxDiscountCap());
        existing.setExpiryDate(incoming.getExpiryDate());
        existing.setUsageLimit(incoming.getUsageLimit() != null ? incoming.getUsageLimit() : 0);
        existing.setPerUserLimit(incoming.getPerUserLimit() != null ? incoming.getPerUserLimit() : 1);
        existing.setApplicableCourseId(incoming.getApplicableCourseId());
        existing.setDescription(incoming.getDescription());
        if (incoming.getIsActive() != null) {
            existing.setIsActive(incoming.getIsActive());
        }

        Coupon saved = couponRepository.save(existing);

        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "COUPON_UPDATED",
                    "Admin updated coupon '" + saved.getCode() + "'."
            )
            .withActor(null, actorEmail, "Admin", "ADMIN")
            .withEntity("COUPON", String.valueOf(saved.getId()), saved.getCode())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        return saved;
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id, String actorEmail) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        couponRepository.delete(coupon);

        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "COUPON_DELETED",
                    "Admin deleted coupon '" + coupon.getCode() + "' (ID: #" + id + ")."
            )
            .withActor(null, actorEmail, "Admin", "ADMIN")
            .withEntity("COUPON", String.valueOf(id), coupon.getCode())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.LOW);

            auditLogService.record(audit);
        }
    }

    @Override
    @Transactional
    public boolean toggleCouponStatus(Long id, String actorEmail) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

        boolean newState = !(coupon.getIsActive() != null && coupon.getIsActive());
        coupon.setIsActive(newState);
        couponRepository.save(coupon);

        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail,
                    AuditEventType.SETTINGS_CHANGED,
                    "COUPON_STATUS_TOGGLED",
                    "Admin toggled coupon '" + coupon.getCode() + "' status to " + (newState ? "ACTIVE" : "INACTIVE") + "."
            )
            .withActor(null, actorEmail, "Admin", "ADMIN")
            .withEntity("COUPON", String.valueOf(id), coupon.getCode())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(audit);
        }

        return newState;
    }

    @Override
    public Map<String, Object> validateAndApplyCoupon(String rawCode, String courseName, String userEmail) {
        Map<String, Object> result = new HashMap<>();

        if (rawCode == null || rawCode.trim().isEmpty()) {
            result.put("valid", false);
            result.put("message", "Please provide a coupon code.");
            return result;
        }

        String code = rawCode.trim().toUpperCase();
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code);

        if (coupon == null) {
            result.put("valid", false);
            result.put("message", "Invalid coupon code '" + code + "'.");
            return result;
        }

        if (coupon.getIsActive() == null || !coupon.getIsActive()) {
            result.put("valid", false);
            result.put("message", "Coupon code '" + code + "' is no longer active.");
            return result;
        }

        // Check Expiry Date
        if (coupon.getExpiryDate() != null && !coupon.getExpiryDate().trim().isEmpty()) {
            try {
                LocalDate expiry = LocalDate.parse(coupon.getExpiryDate().trim());
                if (LocalDate.now().isAfter(expiry)) {
                    result.put("valid", false);
                    result.put("message", "Coupon code '" + code + "' expired on " + expiry + ".");
                    return result;
                }
            } catch (Exception ignored) {}
        }

        // Check Global Usage Limit
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() > 0) {
            int used = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
            if (used >= coupon.getUsageLimit()) {
                result.put("valid", false);
                result.put("message", "Coupon code '" + code + "' has reached its maximum usage limit.");
                return result;
            }
        }

        // Resolve Course List Price
        Course course = courseService.getCourseDetails(courseName != null ? courseName.trim() : "");
        if (course == null) {
            result.put("valid", false);
            result.put("message", "Target course not found.");
            return result;
        }

        // Check Applicable Course Restriction
        if (coupon.getApplicableCourseId() != null && !coupon.getApplicableCourseId().trim().isEmpty()) {
            try {
                long allowedCourseId = Long.parseLong(coupon.getApplicableCourseId().trim());
                if (course.getId() != null && course.getId() != allowedCourseId) {
                    result.put("valid", false);
                    result.put("message", "This coupon is only valid for a specific course.");
                    return result;
                }
            } catch (NumberFormatException ignored) {}
        }

        BigDecimal listPrice = course.getEffectivePrice();

        // Check Minimum Order Amount
        if (coupon.getMinOrderAmount() != null && !coupon.getMinOrderAmount().trim().isEmpty()) {
            try {
                BigDecimal minAmount = new BigDecimal(coupon.getMinOrderAmount().trim());
                if (listPrice.compareTo(minAmount) < 0) {
                    result.put("valid", false);
                    result.put("message", "Minimum order amount of ₹" + minAmount + " required to use this coupon.");
                    return result;
                }
            } catch (Exception ignored) {}
        }

        // Calculate Discount
        BigDecimal discount = BigDecimal.ZERO;
        String discountType = coupon.getDiscountType() != null ? coupon.getDiscountType().toUpperCase() : "PERCENTAGE";
        BigDecimal discountVal = BigDecimal.ZERO;
        try {
            discountVal = new BigDecimal(coupon.getDiscountValue() != null ? coupon.getDiscountValue().trim() : "0");
        } catch (Exception ignored) {}

        if ("PERCENTAGE".equals(discountType) || "PERCENT".equals(discountType)) {
            discount = listPrice.multiply(discountVal)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Apply Max Cap if configured
            if (coupon.getMaxDiscountCap() != null && !coupon.getMaxDiscountCap().trim().isEmpty()) {
                try {
                    BigDecimal maxCap = new BigDecimal(coupon.getMaxDiscountCap().trim());
                    if (maxCap.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxCap) > 0) {
                        discount = maxCap;
                    }
                } catch (Exception ignored) {}
            }
        } else { // FLAT
            discount = discountVal;
            if (discount.compareTo(listPrice) > 0) {
                discount = listPrice;
            }
        }

        BigDecimal payable = listPrice.subtract(discount);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }

        result.put("valid", true);
        result.put("code", coupon.getCode());
        result.put("discountType", discountType);
        result.put("discountValue", coupon.getDiscountValue());
        result.put("discountAmount", discount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        result.put("originalPrice", listPrice.setScale(2, RoundingMode.HALF_UP).toPlainString());
        result.put("finalPayablePrice", payable.setScale(2, RoundingMode.HALF_UP).toPlainString());
        result.put("message", "Coupon '" + coupon.getCode() + "' applied! You saved ₹" + discount.setScale(2, RoundingMode.HALF_UP) + ".");

        return result;
    }

    @Override
    @Transactional
    public void incrementCouponUsage(String code) {
        if (code == null || code.trim().isEmpty()) return;
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim());
        if (coupon != null) {
            coupon.setUsedCount((coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) + 1);
            couponRepository.save(coupon);
        }
    }

    @Override
    public Map<String, Object> getCouponStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Coupon> all = couponRepository.findAll();

        long total = all.size();
        long active = all.stream().filter(c -> c.getIsActive() != null && c.getIsActive()).count();
        long totalRedemptions = all.stream().mapToLong(c -> c.getUsedCount() != null ? c.getUsedCount() : 0).sum();

        stats.put("totalCoupons", total);
        stats.put("activeCoupons", active);
        stats.put("totalRedemptions", totalRedemptions);

        return stats;
    }
}
