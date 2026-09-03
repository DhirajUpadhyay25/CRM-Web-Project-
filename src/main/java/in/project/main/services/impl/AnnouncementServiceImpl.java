package in.project.main.services.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Announcement;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.NotificationCategory;
import in.project.main.entities.enums.NotificationPriority;
import in.project.main.entities.enums.NotificationType;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AnnouncementRepository;
import in.project.main.services.AnnouncementService;
import in.project.main.services.AuditLogService;
import in.project.main.services.NotificationService;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementServiceImpl.class);

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<Announcement> getAnnouncementsPage(
            String search, String targetAudience, String priority, Boolean isActive, Pageable pageable) {
        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String audience = (targetAudience != null && !targetAudience.trim().isEmpty() && !"ALL_AUDIENCES".equalsIgnoreCase(targetAudience)) ? targetAudience.trim() : null;
        String prio = (priority != null && !priority.trim().isEmpty() && !"ALL_PRIORITIES".equalsIgnoreCase(priority)) ? priority.trim() : null;
        return announcementRepository.searchAnnouncements(query, audience, prio, isActive, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Announcement> getAllActiveAnnouncements() {
        return announcementRepository.findByIsActiveTrueOrderByPinnedDescIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Announcement> getActiveAnnouncementsForAudience(String role) {
        if ("STUDENT".equalsIgnoreCase(role)) {
            return announcementRepository.findByTargetAudienceInAndIsActiveTrueOrderByPinnedDescIdDesc(Arrays.asList("ALL", "STUDENTS"));
        } else if ("INSTRUCTOR".equalsIgnoreCase(role)) {
            return announcementRepository.findByTargetAudienceInAndIsActiveTrueOrderByPinnedDescIdDesc(Arrays.asList("ALL", "INSTRUCTORS"));
        }
        return announcementRepository.findByIsActiveTrueOrderByPinnedDescIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Announcement getAnnouncementById(Long id) {
        return announcementRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Announcement createAnnouncement(Announcement announcement, boolean broadcastNotification, String actorEmail) {
        if (announcement.getTitle() == null || announcement.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Announcement title is required.");
        }
        if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Announcement content is required.");
        }
        if (announcement.getPublishDate() == null || announcement.getPublishDate().trim().isEmpty()) {
            announcement.setPublishDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (announcement.getTargetAudience() == null || announcement.getTargetAudience().trim().isEmpty()) {
            announcement.setTargetAudience("ALL");
        }
        if (announcement.getPriority() == null || announcement.getPriority().trim().isEmpty()) {
            announcement.setPriority("NORMAL");
        }
        if (announcement.getCategory() == null || announcement.getCategory().trim().isEmpty()) {
            announcement.setCategory("GENERAL");
        }
        if (announcement.getPinned() == null) {
            announcement.setPinned(false);
        }
        if (announcement.getIsActive() == null) {
            announcement.setIsActive(true);
        }
        announcement.setAuthorEmail(actorEmail);

        Announcement saved = announcementRepository.save(announcement);

        // Optional broadcast notification
        if (broadcastNotification && Boolean.TRUE.equals(saved.getIsActive())) {
            try {
                NotificationPriority notifPriority = "URGENT".equalsIgnoreCase(saved.getPriority()) ? NotificationPriority.CRITICAL :
                        ("HIGH".equalsIgnoreCase(saved.getPriority()) ? NotificationPriority.HIGH : NotificationPriority.NORMAL);

                notificationService.broadcastNotification(
                        "📢 " + saved.getTitle(),
                        saved.getContent().length() > 200 ? saved.getContent().substring(0, 197) + "..." : saved.getContent(),
                        saved.getTargetAudience(),
                        NotificationCategory.ANNOUNCEMENT,
                        notifPriority,
                        "/announcements",
                        actorEmail
                );
            } catch (Exception ex) {
                log.warn("Failed to broadcast in-app notification for announcement {}: {}", saved.getId(), ex.getMessage());
            }
        }

        // Audit Log
        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail != null ? actorEmail : "admin@edutake.com",
                    AuditEventType.ANNOUNCEMENT_CREATED,
                    "ANNOUNCEMENT_CREATED",
                    "Admin published announcement '" + saved.getTitle() + "' to audience '" + saved.getTargetAudience() + "'."
            )
            .withEntity("ANNOUNCEMENT", String.valueOf(saved.getId()), saved.getTitle())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);
            auditLogService.record(audit);
        }

        return saved;
    }

    @Override
    @Transactional
    public Announcement updateAnnouncement(Long id, Announcement incoming, String actorEmail) {
        Announcement existing = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found with ID: " + id));

        existing.setTitle(incoming.getTitle());
        existing.setContent(incoming.getContent());
        if (incoming.getTargetAudience() != null) existing.setTargetAudience(incoming.getTargetAudience());
        if (incoming.getCategory() != null) existing.setCategory(incoming.getCategory());
        if (incoming.getPriority() != null) existing.setPriority(incoming.getPriority());
        if (incoming.getPinned() != null) existing.setPinned(incoming.getPinned());
        if (incoming.getCourseId() != null) existing.setCourseId(incoming.getCourseId());
        if (incoming.getCourseName() != null) existing.setCourseName(incoming.getCourseName());
        if (incoming.getPublishDate() != null) existing.setPublishDate(incoming.getPublishDate());
        if (incoming.getExpiresAt() != null) existing.setExpiresAt(incoming.getExpiresAt());
        if (incoming.getIsActive() != null) existing.setIsActive(incoming.getIsActive());

        Announcement saved = announcementRepository.save(existing);

        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail != null ? actorEmail : "admin@edutake.com",
                    AuditEventType.ANNOUNCEMENT_CREATED,
                    "ANNOUNCEMENT_UPDATED",
                    "Admin updated announcement '" + saved.getTitle() + "'."
            )
            .withEntity("ANNOUNCEMENT", String.valueOf(saved.getId()), saved.getTitle())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);
            auditLogService.record(audit);
        }

        return saved;
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id, String actorEmail) {
        Announcement existing = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found with ID: " + id));

        announcementRepository.delete(existing);

        if (auditLogService != null) {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                    actorEmail != null ? actorEmail : "admin@edutake.com",
                    AuditEventType.ANNOUNCEMENT_DELETED,
                    "ANNOUNCEMENT_DELETED",
                    "Admin deleted announcement '" + existing.getTitle() + "' (ID: " + id + ")."
            )
            .withEntity("ANNOUNCEMENT", String.valueOf(id), existing.getTitle())
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.MEDIUM);
            auditLogService.record(audit);
        }
    }

    @Override
    @Transactional
    public Announcement toggleActiveStatus(Long id, String actorEmail) {
        Announcement existing = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found with ID: " + id));

        existing.setIsActive(!Boolean.TRUE.equals(existing.getIsActive()));
        return announcementRepository.save(existing);
    }

    @Override
    @Transactional
    public Announcement togglePinnedStatus(Long id, String actorEmail) {
        Announcement existing = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Announcement not found with ID: " + id));

        existing.setPinned(!Boolean.TRUE.equals(existing.getPinned()));
        return announcementRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAnnouncementMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        long total = announcementRepository.count();
        long active = announcementRepository.countByIsActive(true);
        long pinned = announcementRepository.countByPinned(true);
        metrics.put("total", total);
        metrics.put("active", active);
        metrics.put("pinned", pinned);
        metrics.put("inactive", total - active);
        return metrics;
    }
}
