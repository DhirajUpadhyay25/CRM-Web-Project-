package in.project.main.entities;

import java.time.LocalDateTime;

import in.project.main.entities.enums.EnrollmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}),
    indexes = {
        @Index(name = "idx_enrollment_status", columnList = "status"),
        @Index(name = "idx_enrollment_user", columnList = "user_id"),
        @Index(name = "idx_enrollment_course", columnList = "course_id"),
        @Index(name = "idx_enrollment_created", columnList = "enrolledAt")
    }
)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(updatable = false)
    private LocalDateTime enrolledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column
    private LocalDateTime completedAt;

    @Column
    private String paymentStatus;

    @Column
    private String enrollmentType; // FREE, PAID, ADMIN_ASSIGNED, SCHOLARSHIP, TRIAL

    @Column
    private String enrollmentSource; // STUDENT_PURCHASE, FREE_ENROLLMENT, ADMIN_ASSIGNMENT, COUPON_PROMOTION, BULK_ENROLLMENT, MANUAL_IMPORT

    @Column
    private LocalDateTime startDate;

    @Column
    private LocalDateTime expiryDate;

    @Column(length = 1000)
    private String statusReason;

    @Column(length = 2000)
    private String adminNote;

    @Column
    private LocalDateTime lastAccessedAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private String orderId;

    @PrePersist
    protected void onCreate() {
        if (this.enrolledAt == null) {
            this.enrolledAt = LocalDateTime.now();
        }
        if (this.startDate == null) {
            this.startDate = this.enrolledAt;
        }
        this.updatedAt = LocalDateTime.now();
        if (this.enrollmentType == null) {
            this.enrollmentType = "FREE".equalsIgnoreCase(this.paymentStatus) ? "FREE" : "PAID";
        }
        if (this.enrollmentSource == null) {
            this.enrollmentSource = "FREE".equalsIgnoreCase(this.paymentStatus) ? "FREE_ENROLLMENT" : "STUDENT_PURCHASE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canAccess() {
        if (status == null || !status.isAccessAllowed()) {
            return false;
        }
        if (expiryDate != null && LocalDateTime.now().isAfter(expiryDate)) {
            return false;
        }
        return true;
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }

    public String getEnrollmentType() { return enrollmentType; }
    public void setEnrollmentType(String enrollmentType) { this.enrollmentType = enrollmentType; }

    public String getEnrollmentSource() { return enrollmentSource; }
    public void setEnrollmentSource(String enrollmentSource) { this.enrollmentSource = enrollmentSource; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserEmail() {
        return user != null ? user.getEmail() : null;
    }

    public String getUserName() {
        return user != null ? user.getName() : null;
    }
}

