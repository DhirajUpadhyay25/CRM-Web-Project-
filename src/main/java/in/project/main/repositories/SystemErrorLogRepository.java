package in.project.main.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.SystemErrorLog;

@Repository
public interface SystemErrorLogRepository extends JpaRepository<SystemErrorLog, Long> {

    Optional<SystemErrorLog> findByErrorSignatureAndStatus(String errorSignature, String status);

    Optional<SystemErrorLog> findByErrorSignature(String errorSignature);

    Page<SystemErrorLog> findByStatusOrderByLastOccurredAtDesc(String status, Pageable pageable);

    Page<SystemErrorLog> findAllByOrderByLastOccurredAtDesc(Pageable pageable);

    List<SystemErrorLog> findTop10ByOrderByLastOccurredAtDesc();

    long countByStatus(String status);

    @Query("SELECT COUNT(e) FROM SystemErrorLog e WHERE e.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT SUM(e.occurrenceCount) FROM SystemErrorLog e WHERE e.createdAt >= :since")
    Long sumTotalOccurrencesSince(@Param("since") LocalDateTime since);
}
