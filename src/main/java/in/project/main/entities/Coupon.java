package in.project.main.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String discountType = "PERCENTAGE"; // PERCENTAGE, FLAT

    @Column(nullable = false)
    private String discountValue; // e.g. "20" for 20% or "500" for ₹500

    @Column
    private String minOrderAmount; // Minimum order subtotal to apply

    @Column
    private String maxDiscountCap; // Maximum discount amount for percentage coupons

    @Column
    private String expiryDate; // YYYY-MM-DD or datetime

    @Column
    private Integer usageLimit = 0; // 0 = unlimited

    @Column
    private Integer usedCount = 0;

    @Column
    private Integer perUserLimit = 1;

    @Column
    private String applicableCourseId; // null or empty means applicable to all courses

    @Column
    private String description;

    @Column
    private Boolean isActive = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public String getDiscountValue() { return discountValue; }
    public void setDiscountValue(String discountValue) { this.discountValue = discountValue; }

    public String getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(String minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public String getMaxDiscountCap() { return maxDiscountCap; }
    public void setMaxDiscountCap(String maxDiscountCap) { this.maxDiscountCap = maxDiscountCap; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsedCount() { return usedCount != null ? usedCount : 0; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public Integer getPerUserLimit() { return perUserLimit != null ? perUserLimit : 1; }
    public void setPerUserLimit(Integer perUserLimit) { this.perUserLimit = perUserLimit; }

    public String getApplicableCourseId() { return applicableCourseId; }
    public void setApplicableCourseId(String applicableCourseId) { this.applicableCourseId = applicableCourseId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive != null ? isActive : true; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

}
