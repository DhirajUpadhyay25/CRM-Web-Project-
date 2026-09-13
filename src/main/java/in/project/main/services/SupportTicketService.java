package in.project.main.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.dto.ProblemReportDTO;
import in.project.main.dto.SupportTicketDTO;
import in.project.main.entities.Course;
import in.project.main.entities.Lesson;
import in.project.main.entities.SupportTicket;
import in.project.main.entities.SupportTicketReply;
import in.project.main.entities.User;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.NotificationType;
import in.project.main.entities.enums.TicketCategory;
import in.project.main.entities.enums.TicketPriority;
import in.project.main.entities.enums.TicketStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.LessonRepository;
import in.project.main.repositories.SupportTicketReplyRepository;
import in.project.main.repositories.SupportTicketRepository;
import in.project.main.repositories.UserRepository;

@Service
public class SupportTicketService {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketService.class);

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private SupportTicketReplyRepository replyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private CourseRepository courseRepository;

    @Autowired(required = false)
    private LessonRepository lessonRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    private final Random random = new Random();

    /**
     * Creates a new persistent support ticket.
     */
    @Transactional
    public SupportTicket createTicket(SupportTicketDTO dto, User authUser) {
        SupportTicket ticket = new SupportTicket();

        // 1. User & Contact details
        if (authUser != null) {
            ticket.setUser(authUser);
            ticket.setUserName(authUser.getName());
            ticket.setUserEmail(authUser.getEmail());
            ticket.setUserRole("STUDENT");
        } else {
            String name = (dto.getName() != null && !dto.getName().isBlank()) ? dto.getName().trim() : "Guest User";
            String email = (dto.getEmail() != null && !dto.getEmail().isBlank()) ? dto.getEmail().trim() : "guest@edutake.com";
            ticket.setUserName(name);
            ticket.setUserEmail(email);
            ticket.setUserRole("GUEST");
        }

        // 2. Ticket properties
        ticket.setTicketNumber("TCK-" + (100000 + random.nextInt(900000)));
        ticket.setCategory(dto.getCategory() != null ? dto.getCategory() : TicketCategory.OTHER);
        ticket.setSubject(dto.getSubject() != null ? dto.getSubject().trim() : "Support Request");
        ticket.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(dto.getPriority() != null ? dto.getPriority() : TicketPriority.MEDIUM);

        // 3. Captured safe context
        ticket.setPageUrl(dto.getPageUrl());
        ticket.setDeviceInfo(dto.getDeviceInfo());
        ticket.setBrowserInfo(dto.getBrowserInfo());

        // 4. Academic context resolution
        if (dto.getCourseId() != null) {
            ticket.setCourseId(dto.getCourseId());
            if (dto.getCourseName() != null && !dto.getCourseName().isBlank()) {
                ticket.setCourseName(dto.getCourseName());
            } else if (courseRepository != null) {
                courseRepository.findById(dto.getCourseId()).ifPresent(c -> ticket.setCourseName(c.getName()));
            }
        }

        if (dto.getLessonId() != null) {
            ticket.setLessonId(dto.getLessonId());
            if (dto.getLessonTitle() != null && !dto.getLessonTitle().isBlank()) {
                ticket.setLessonTitle(dto.getLessonTitle());
            } else if (lessonRepository != null) {
                lessonRepository.findById(dto.getLessonId()).ifPresent(l -> ticket.setLessonTitle(l.getTitle()));
            }
        }

        SupportTicket savedTicket = ticketRepository.save(ticket);
        logger.info("Created support ticket #{}: {}", savedTicket.getTicketNumber(), savedTicket.getSubject());

        // 5. Notify administrators
        dispatchAdminNotification(savedTicket);

        // 6. Audit logging
        recordAudit(ticket.getUserEmail(), ticket.getUserName(), "SUPPORT_TICKET_CREATED",
                "Created support ticket #" + savedTicket.getTicketNumber() + " [" + savedTicket.getCategory() + "]",
                savedTicket.getId());

        return savedTicket;
    }

    /**
     * Reports a lightweight problem/bug directly from LMS context.
     */
    @Transactional
    public SupportTicket reportProblem(ProblemReportDTO dto, User authUser) {
        SupportTicketDTO ticketDTO = new SupportTicketDTO();
        ticketDTO.setName(dto.getName());
        ticketDTO.setEmail(dto.getEmail());

        TicketCategory category = TicketCategory.TECHNICAL_ISSUE;
        String issueType = dto.getIssueType() != null ? dto.getIssueType().toUpperCase() : "TECHNICAL_ISSUE";
        if (issueType.contains("VIDEO")) category = TicketCategory.LESSON_VIDEO;
        else if (issueType.contains("QUIZ")) category = TicketCategory.QUIZ_ASSESSMENT;
        else if (issueType.contains("ASSIGNMENT")) category = TicketCategory.ASSIGNMENT;

        ticketDTO.setCategory(category);
        ticketDTO.setSubject("Problem Report: " + formatIssueType(dto.getIssueType()));
        ticketDTO.setDescription(dto.getDescription());
        ticketDTO.setPriority(TicketPriority.HIGH);
        ticketDTO.setPageUrl(dto.getPageUrl());
        ticketDTO.setBrowserInfo(dto.getBrowserInfo());
        ticketDTO.setDeviceInfo(dto.getDeviceInfo());
        ticketDTO.setCourseId(dto.getCourseId());
        ticketDTO.setLessonId(dto.getLessonId());

        return createTicket(ticketDTO, authUser);
    }

    private String formatIssueType(String type) {
        if (type == null) return "General Issue";
        switch (type.toUpperCase()) {
            case "BROKEN_PAGE": return "Broken Page / 404";
            case "VIDEO_ISSUE": return "Video Playback Issue";
            case "QUIZ_ISSUE": return "Quiz Assessment Problem";
            case "ASSIGNMENT_ISSUE": return "Assignment Submission Failure";
            case "UI_BUG": return "UI / Layout Glitch";
            case "INCORRECT_CONTENT": return "Incorrect Content / Typo";
            default: return type.replace("_", " ");
        }
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> searchTickets(String keyword, TicketStatus status, TicketCategory category, TicketPriority priority, Pageable pageable) {
        return ticketRepository.searchTickets(keyword, status, category, priority, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<SupportTicket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<SupportTicket> getTicketByNumber(String ticketNumber) {
        return ticketRepository.findByTicketNumber(ticketNumber);
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> getTicketsByUserEmail(String email) {
        return ticketRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    @Transactional
    public SupportTicketReply addReply(Long ticketId, String senderType, String senderName, String senderEmail, String message) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found: " + ticketId));

        SupportTicketReply reply = new SupportTicketReply(ticket, senderType, senderName, senderEmail, message);
        SupportTicketReply savedReply = replyRepository.save(reply);

        // Update ticket status
        if ("ADMIN".equalsIgnoreCase(senderType) || "SUPPORT_AGENT".equalsIgnoreCase(senderType)) {
            if (ticket.getStatus() == TicketStatus.OPEN) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            }
        } else if ("USER".equalsIgnoreCase(senderType)) {
            if (ticket.getStatus() == TicketStatus.WAITING_FOR_USER) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
            }
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        logger.info("Added reply to ticket #{} by {}", ticket.getTicketNumber(), senderEmail);
        return savedReply;
    }

    @Transactional
    public SupportTicket updateStatus(Long ticketId, TicketStatus newStatus, String resolution, String updatedByEmail) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found: " + ticketId));

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        if (resolution != null && !resolution.isBlank()) {
            ticket.setResolution(resolution.trim());
        }
        if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        SupportTicket saved = ticketRepository.save(ticket);

        // Auto-add a system reply
        String statusNote = "Status changed from " + oldStatus.getDisplayName() + " to " + newStatus.getDisplayName() +
                (resolution != null && !resolution.isBlank() ? ". Resolution: " + resolution : "");
        replyRepository.save(new SupportTicketReply(ticket, "SYSTEM", "EduTake System", "system@edutake.com", statusNote));

        recordAudit(updatedByEmail, updatedByEmail, "SUPPORT_TICKET_STATUS_UPDATED",
                "Updated ticket #" + ticket.getTicketNumber() + " status to " + newStatus, ticket.getId());

        return saved;
    }

    @Transactional
    public SupportTicket assignTicket(Long ticketId, String adminEmail, String adminName) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found: " + ticketId));

        ticket.setAssignedTo(adminName);
        ticket.setAssignedToEmail(adminEmail);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        SupportTicket saved = ticketRepository.save(ticket);

        replyRepository.save(new SupportTicketReply(ticket, "SYSTEM", "EduTake System", "system@edutake.com",
                "Ticket assigned to " + adminName + " (" + adminEmail + ")"));

        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", ticketRepository.count());
        stats.put("open", ticketRepository.countByStatus(TicketStatus.OPEN));
        stats.put("inProgress", ticketRepository.countByStatus(TicketStatus.IN_PROGRESS));
        stats.put("waiting", ticketRepository.countByStatus(TicketStatus.WAITING_FOR_USER));
        stats.put("resolved", ticketRepository.countByStatus(TicketStatus.RESOLVED));
        stats.put("closed", ticketRepository.countByStatus(TicketStatus.CLOSED));
        stats.put("active", ticketRepository.countActiveTickets());
        stats.put("urgent", ticketRepository.countByPriority(TicketPriority.URGENT));
        return stats;
    }

    private void dispatchAdminNotification(SupportTicket ticket) {
        if (notificationService == null) return;
        try {
            notificationService.sendToAdmins(
                    NotificationType.SYSTEM_WARNING,
                    "New Support Ticket #" + ticket.getTicketNumber(),
                    ticket.getUserName() + " (" + ticket.getUserEmail() + ") opened a " +
                            ticket.getPriority().getDisplayName() + " priority ticket: \"" + ticket.getSubject() + "\"",
                    "/admin/support/" + ticket.getId(),
                    "SUPPORT_TICKET",
                    String.valueOf(ticket.getId())
            );
        } catch (Exception e) {
            logger.warn("Could not dispatch admin notification for ticket #{}: {}", ticket.getTicketNumber(), e.getMessage());
        }
    }

    private void recordAudit(String email, String name, String action, String description, Long entityId) {
        if (auditLogService == null) return;
        try {
            PlatformAuditEvent event = PlatformAuditEvent.of(
                    email != null ? email : "anonymous",
                    null,
                    action,
                    description
            ).withActor(null, email, name, "USER")
             .withEntity("SupportTicket", String.valueOf(entityId), "Ticket");
            auditLogService.record(event);
        } catch (Exception e) {
            logger.warn("Could not write audit log: {}", e.getMessage());
        }
    }
}
