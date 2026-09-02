package in.project.main.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.AuditLog;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    Page<AuditLog> findByActorEmailOrderByCreatedAtDesc(String actorEmail, Pageable pageable);

    List<AuditLog> findTop10ByOrderByCreatedAtDesc();

    // KPI Metrics Queries
    long countByStatus(AuditStatus status);

    long countBySeverity(AuditSeverity severity);

    long countByCategory(AuditCategory category);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.status = :status AND a.createdAt >= :since")
    long countByStatusSince(@Param("status") AuditStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.severity = :severity AND a.createdAt >= :since")
    long countBySeveritySince(@Param("severity") AuditSeverity severity, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT a.actorEmail) FROM AuditLog a WHERE a.createdAt >= :since")
    long countDistinctActorsSince(@Param("since") LocalDateTime since);

    // Security Monitoring: Count failed logins for an email or IP in recent time window
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.eventType = :eventType AND (a.actorEmail = :email OR a.ipAddress = :ip) AND a.createdAt >= :since")
    long countFailedLoginsRecent(@Param("eventType") AuditEventType eventType, @Param("email") String email, @Param("ip") String ip, @Param("since") LocalDateTime since);

    // Find by Request ID for correlation tracking
    List<AuditLog> findByRequestIdOrderByCreatedAtAsc(String requestId);
}
