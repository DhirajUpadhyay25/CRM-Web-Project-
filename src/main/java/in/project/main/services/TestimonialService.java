package in.project.main.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Course;
import in.project.main.entities.Testimonial;
import in.project.main.entities.User;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.TestimonialSource;
import in.project.main.entities.enums.TestimonialStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.TestimonialRepository;
import in.project.main.repositories.UserRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class TestimonialService {

    private static final Logger logger = LoggerFactory.getLogger(TestimonialService.class);

    @Autowired
    private TestimonialRepository testimonialRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    // ==========================================
    // Query & Filtering
    // ==========================================

    @Transactional(readOnly = true)
    public Page<Testimonial> getTestimonials(String keyword, Long courseId, TestimonialStatus status, Integer rating, TestimonialSource source, Boolean isFeatured, Pageable pageable) {
        Specification<Testimonial> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate studentNamePred = cb.like(cb.lower(root.get("studentName")), pattern);
                Predicate titlePred = cb.like(cb.lower(root.get("title")), pattern);
                Predicate contentPred = cb.like(cb.lower(root.get("content")), pattern);
                Predicate orgPred = cb.like(cb.lower(root.get("organization")), pattern);
                Predicate desigPred = cb.like(cb.lower(root.get("designation")), pattern);
                predicates.add(cb.or(studentNamePred, titlePred, contentPred, orgPred, desigPred));
            }

            if (courseId != null) {
                predicates.add(cb.equal(root.get("course").get("id"), courseId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (rating != null && rating > 0) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }

            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }

            if (isFeatured != null) {
                predicates.add(cb.equal(root.get("isFeatured"), isFeatured));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return testimonialRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Testimonial> getTestimonialById(Long id) {
        return testimonialRepository.findById(id).filter(t -> !Boolean.TRUE.equals(t.getDeleted()));
    }

    @Transactional(readOnly = true)
    public List<Testimonial> getAllPublishedTestimonials() {
        return testimonialRepository.findAllPublished();
    }

    @Transactional(readOnly = true)
    public List<Testimonial> getFeaturedTestimonials() {
        return testimonialRepository.findFeaturedPublished();
    }

    @Transactional(readOnly = true)
    public List<Testimonial> getFeaturedOrPublished(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Order.desc("isFeatured"), Sort.Order.asc("displayOrder"), Sort.Order.desc("publishedAt")));
        return testimonialRepository.findFeaturedOrPublished(pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        long totalStories = testimonialRepository.countByDeletedFalse();
        long publishedStories = testimonialRepository.countByStatusAndDeletedFalse(TestimonialStatus.PUBLISHED);
        long pendingReview = testimonialRepository.countByStatusAndDeletedFalse(TestimonialStatus.PENDING);
        long rejectedStories = testimonialRepository.countByStatusAndDeletedFalse(TestimonialStatus.REJECTED);
        long archivedStories = testimonialRepository.countByStatusAndDeletedFalse(TestimonialStatus.ARCHIVED);
        long featuredStories = testimonialRepository.countByIsFeaturedTrueAndDeletedFalse();
        double avgRating = testimonialRepository.getAverageRating();

        metrics.put("totalStories", totalStories);
        metrics.put("publishedStories", publishedStories);
        metrics.put("pendingReview", pendingReview);
        metrics.put("rejectedStories", rejectedStories);
        metrics.put("archivedStories", archivedStories);
        metrics.put("featuredStories", featuredStories);
        metrics.put("averageRating", Math.round(avgRating * 10.0) / 10.0);

        return metrics;
    }

    // ==========================================
    // CRUD & Moderation Operations
    // ==========================================

    @Transactional
    public Testimonial saveTestimonial(Testimonial input, Long courseId, Long studentId, Long actorId, String actorEmail) {
        if (input == null) {
            throw new IllegalArgumentException("Testimonial data cannot be null.");
        }

        boolean isNew = (input.getId() == null || input.getId() == 0L);
        Testimonial testimonial;

        if (!isNew) {
            testimonial = testimonialRepository.findById(input.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Testimonial not found with ID: " + input.getId()));
        } else {
            testimonial = new Testimonial();
            testimonial.setCreatedAt(LocalDateTime.now());
        }

        // Student Info
        if (studentId != null) {
            User student = userRepository.findById(studentId).orElse(null);
            testimonial.setStudent(student);
            if (student != null && (input.getStudentName() == null || input.getStudentName().isBlank())) {
                testimonial.setStudentName(student.getName());
            }
        }
        if (input.getStudentName() != null && !input.getStudentName().isBlank()) {
            testimonial.setStudentName(input.getStudentName().trim());
        }

        // Course Info
        if (courseId != null) {
            Course course = courseRepository.findById(courseId).orElse(null);
            testimonial.setCourse(course);
            if (course != null) {
                testimonial.setCourseName(course.getName());
            }
        } else if (input.getCourseName() != null && !input.getCourseName().isBlank()) {
            testimonial.setCourseName(input.getCourseName().trim());
        }

        testimonial.setTitle(input.getTitle() != null ? input.getTitle().trim() : "Student Success Story");
        testimonial.setContent(input.getContent() != null ? input.getContent().trim() : input.getReview());
        testimonial.setReview(testimonial.getContent());
        testimonial.setDesignation(input.getDesignation() != null ? input.getDesignation().trim() : "");
        testimonial.setOrganization(input.getOrganization() != null ? input.getOrganization().trim() : "");
        testimonial.setStudentPhoto(input.getStudentPhoto() != null ? input.getStudentPhoto().trim() : "");
        testimonial.setDisplayOrder(input.getDisplayOrder() != null ? input.getDisplayOrder() : 0);
        testimonial.setRating(input.getRating() != null ? Math.min(5, Math.max(1, input.getRating())) : 5);
        testimonial.setSource(input.getSource() != null ? input.getSource() : TestimonialSource.ADMIN_CREATED);
        testimonial.setStatus(input.getStatus() != null ? input.getStatus() : TestimonialStatus.PUBLISHED);
        testimonial.setIsFeatured(Boolean.TRUE.equals(input.getIsFeatured()));
        testimonial.setConsentName(Boolean.TRUE.equals(input.getConsentName()));
        testimonial.setConsentPhoto(Boolean.TRUE.equals(input.getConsentPhoto()));
        testimonial.setConsentPublish(Boolean.TRUE.equals(input.getConsentPublish()));
        testimonial.setModerationReason(input.getModerationReason());
        testimonial.setDeleted(Boolean.FALSE);

        if (testimonial.getStatus() == TestimonialStatus.PUBLISHED) {
            testimonial.setIsApproved(Boolean.TRUE);
            if (testimonial.getPublishedAt() == null) {
                testimonial.setPublishedAt(LocalDateTime.now());
            }
        } else {
            testimonial.setIsApproved(Boolean.FALSE);
        }

        testimonial.setUpdatedAt(LocalDateTime.now());

        Testimonial saved = testimonialRepository.save(testimonial);

        logAudit(
                actorEmail,
                isNew ? AuditEventType.TESTIMONIAL_CREATED : AuditEventType.TESTIMONIAL_UPDATED,
                isNew ? "TESTIMONIAL_CREATE" : "TESTIMONIAL_UPDATE",
                "Testimonial " + (isNew ? "created" : "updated") + " for " + saved.getDisplayStudentName() + " (" + saved.getStatus() + ")",
                saved.getId(),
                saved.getDisplayStudentName()
        );

        return saved;
    }

    @Transactional
    public Testimonial updateStatus(Long id, TestimonialStatus newStatus, String reason, String actorEmail) {
        Testimonial testimonial = testimonialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Testimonial not found with ID: " + id));

        testimonial.setStatus(newStatus);
        if (reason != null && !reason.isBlank()) {
            testimonial.setModerationReason(reason.trim());
        }

        if (newStatus == TestimonialStatus.PUBLISHED) {
            testimonial.setIsApproved(Boolean.TRUE);
            if (testimonial.getPublishedAt() == null) {
                testimonial.setPublishedAt(LocalDateTime.now());
            }
        } else {
            testimonial.setIsApproved(Boolean.FALSE);
        }

        testimonial.setUpdatedAt(LocalDateTime.now());
        Testimonial saved = testimonialRepository.save(testimonial);

        logAudit(
                actorEmail,
                AuditEventType.TESTIMONIAL_UPDATED,
                "TESTIMONIAL_STATUS_CHANGE",
                "Changed status of testimonial #" + id + " to " + newStatus,
                id,
                saved.getDisplayStudentName()
        );

        return saved;
    }

    @Transactional
    public boolean toggleFeatured(Long id, String actorEmail) {
        Optional<Testimonial> opt = testimonialRepository.findById(id);
        if (opt.isPresent()) {
            Testimonial testimonial = opt.get();
            boolean newState = !Boolean.TRUE.equals(testimonial.getIsFeatured());
            testimonial.setIsFeatured(newState);
            testimonial.setUpdatedAt(LocalDateTime.now());
            testimonialRepository.save(testimonial);

            logAudit(
                    actorEmail,
                    AuditEventType.TESTIMONIAL_UPDATED,
                    "TESTIMONIAL_TOGGLE_FEATURED",
                    "Testimonial #" + id + " featured set to " + newState,
                    id,
                    testimonial.getDisplayStudentName()
            );
            return newState;
        }
        return false;
    }

    @Transactional
    public void softDelete(Long id, String actorEmail) {
        testimonialRepository.findById(id).ifPresent(testimonial -> {
            testimonial.setDeleted(Boolean.TRUE);
            testimonial.setUpdatedAt(LocalDateTime.now());
            testimonialRepository.save(testimonial);

            logAudit(
                    actorEmail,
                    AuditEventType.TESTIMONIAL_DELETED,
                    "TESTIMONIAL_DELETE",
                    "Deleted testimonial #" + id + " (" + testimonial.getDisplayStudentName() + ")",
                    id,
                    testimonial.getDisplayStudentName()
            );
        });
    }

    // ==========================================
    // Starter Seeder
    // ==========================================

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            if (testimonialRepository.countByDeletedFalse() == 0) {
                logger.info("No testimonials found in database. Seeding initial student success stories...");
                seedDefaultTestimonials();
            }
        } catch (Exception e) {
            logger.warn("Testimonial startup seeder notice: {}", e.getMessage());
        }
    }

    @Transactional
    public int seedDefaultTestimonials() {
        int seededCount = 0;

        seededCount += createOrUpdateSeed(
                "Aarav Sharma",
                "Software Engineer",
                "Google India",
                "Landed My Dream SWE Role at Google!",
                "The Java Fullstack Masterclass gave me the architectural understanding and live project confidence that directly mirrored questions in my system design and coding rounds.",
                5,
                "Java Fullstack Masterclass",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
                true,
                1
        );

        seededCount += createOrUpdateSeed(
                "Priya Patel",
                "Senior Frontend Architect",
                "Microsoft",
                "Transformed from Junior to Lead Engineer",
                "The React & Next.js Pro curriculum is cutting-edge. The real-world production optimization techniques and state management patterns elevated my engineering standards completely.",
                5,
                "React & Next.js Advanced Bootcamp",
                "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&auto=format&fit=crop&q=80",
                true,
                2
        );

        seededCount += createOrUpdateSeed(
                "Rohan Verma",
                "Cloud Solutions Architect",
                "Amazon AWS",
                "Mastered Cloud & Microservices in 4 Months",
                "Hands down the best DevOps & Microservices training available. The hands-on labs with Kubernetes, Docker, and CI/CD pipelines gave me the practical edge to crack AWS interviews.",
                5,
                "Cloud & DevOps Architecture",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                true,
                3
        );

        seededCount += createOrUpdateSeed(
                "Ananya Iyer",
                "AI/ML Research Engineer",
                "Meta",
                "From Zero to Building LLM & Neural Pipelines",
                "The depth of machine learning and GenAI covered by industry mentors here is unparalleled. Working on end-to-end transformers and deployment pipelines made all the difference.",
                5,
                "AI & Machine Learning Specialization",
                "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150&auto=format&fit=crop&q=80",
                true,
                4
        );

        seededCount += createOrUpdateSeed(
                "Vikramaditya Rao",
                "Cybersecurity Lead",
                "Palo Alto Networks",
                "Exceptional Real-World Security Lab Experience",
                "The offensive and defensive security modules with hands-on CTFs were exhilarating. Mentors provided 1-on-1 guidance on real vulnerabilities and modern enterprise defense.",
                5,
                "Certified Cybersecurity Specialist",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80",
                false,
                5
        );

        seededCount += createOrUpdateSeed(
                "Sneha Kulkarni",
                "Staff Product Manager",
                "Adobe",
                "Essential Bridge Between Tech and Product Vision",
                "EduTake helped me deeply understand scalable engineering architectures and agile development cycles, enabling me to communicate effectively with engineering teams.",
                5,
                "Tech Product Management Masterclass",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80",
                false,
                6
        );

        logger.info("Successfully seeded {} default student testimonials.", seededCount);
        return seededCount;
    }

    private int createOrUpdateSeed(String name, String designation, String org, String title, String content, int rating, String courseName, String photo, boolean isFeatured, int displayOrder) {
        List<Testimonial> existing = testimonialRepository.findAll();
        for (Testimonial t : existing) {
            if (!Boolean.TRUE.equals(t.getDeleted()) && name.equalsIgnoreCase(t.getStudentName())) {
                return 0;
            }
        }

        Testimonial t = new Testimonial();
        t.setStudentName(name);
        t.setDesignation(designation);
        t.setOrganization(org);
        t.setTitle(title);
        t.setContent(content);
        t.setReview(content);
        t.setRating(rating);
        t.setCourseName(courseName);
        t.setStudentPhoto(photo);
        t.setStatus(TestimonialStatus.PUBLISHED);
        t.setIsApproved(Boolean.TRUE);
        t.setIsFeatured(isFeatured);
        t.setDisplayOrder(displayOrder);
        t.setSource(TestimonialSource.ADMIN_CREATED);
        t.setConsentName(Boolean.TRUE);
        t.setConsentPhoto(Boolean.TRUE);
        t.setConsentPublish(Boolean.TRUE);
        t.setPublishedAt(LocalDateTime.now().minusDays(displayOrder * 3L));
        t.setDeleted(Boolean.FALSE);

        testimonialRepository.save(t);
        return 1;
    }

    private void logAudit(String actorEmail, AuditEventType eventType, String action, String description, Long entityId, String entityName) {
        if (auditLogService == null) return;
        try {
            PlatformAuditEvent event = PlatformAuditEvent.of(
                    actorEmail != null ? actorEmail : "SYSTEM",
                    eventType != null ? eventType : AuditEventType.TESTIMONIAL_UPDATED,
                    action,
                    description
            )
            .withCategory(AuditCategory.SYSTEM)
            .withEntity("TESTIMONIAL", entityId != null ? String.valueOf(entityId) : null, entityName)
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(event);
        } catch (Exception e) {
            logger.debug("Failed to record audit log for testimonial: {}", e.getMessage());
        }
    }
}
