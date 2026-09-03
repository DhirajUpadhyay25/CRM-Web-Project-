package in.project.main.repositories;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import in.project.main.entities.Announcement;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByIsActiveTrueOrderByPinnedDescIdDesc();

    List<Announcement> findByTargetAudienceInAndIsActiveTrueOrderByPinnedDescIdDesc(List<String> targetAudiences);

    long countByIsActive(Boolean isActive);

    long countByPinned(Boolean pinned);

    @Query("SELECT a FROM Announcement a WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.content) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.authorName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:targetAudience IS NULL OR :targetAudience = '' OR a.targetAudience = :targetAudience) AND " +
           "(:priority IS NULL OR :priority = '' OR a.priority = :priority) AND " +
           "(:isActive IS NULL OR a.isActive = :isActive)")
    Page<Announcement> searchAnnouncements(
            @Param("search") String search,
            @Param("targetAudience") String targetAudience,
            @Param("priority") String priority,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
