package in.project.main.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.project.main.entities.ContentRevision;

public interface ContentRevisionRepository extends JpaRepository<ContentRevision, Long> {

    List<ContentRevision> findByEntityTypeAndEntityIdOrderByRevisionNumberDesc(String entityType, Long entityId);

    @Query("SELECT COALESCE(MAX(r.revisionNumber), 0) FROM ContentRevision r WHERE r.entityType = :entityType AND r.entityId = :entityId")
    Integer findMaxRevisionNumber(@Param("entityType") String entityType, @Param("entityId") Long entityId);
}
