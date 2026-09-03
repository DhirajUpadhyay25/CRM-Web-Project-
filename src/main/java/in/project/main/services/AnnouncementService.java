package in.project.main.services;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import in.project.main.entities.Announcement;

public interface AnnouncementService {

    Page<Announcement> getAnnouncementsPage(String search, String targetAudience, String priority, Boolean isActive, Pageable pageable);

    List<Announcement> getAllActiveAnnouncements();

    List<Announcement> getActiveAnnouncementsForAudience(String role);

    Announcement getAnnouncementById(Long id);

    Announcement createAnnouncement(Announcement announcement, boolean broadcastNotification, String actorEmail);

    Announcement updateAnnouncement(Long id, Announcement incoming, String actorEmail);

    void deleteAnnouncement(Long id, String actorEmail);

    Announcement toggleActiveStatus(Long id, String actorEmail);

    Announcement togglePinnedStatus(Long id, String actorEmail);

    Map<String, Object> getAnnouncementMetrics();
}
