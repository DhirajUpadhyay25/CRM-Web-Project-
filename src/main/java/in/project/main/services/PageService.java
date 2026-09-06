package in.project.main.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.ContentStatus;
import in.project.main.entities.enums.ContentVisibility;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.PageRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class PageService {

    private static final Logger logger = LoggerFactory.getLogger(PageService.class);
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\-]+");
    private static final Pattern MULTI_DASH = Pattern.compile("-+");

    @Autowired
    private PageRepository pageRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    // =========================================================================
    // 1. QUERY & SEARCH
    // =========================================================================

    public Page<in.project.main.entities.Page> getPagesPaged(
            String keyword,
            ContentStatus status,
            ContentVisibility visibility,
            Pageable pageable) {

        Specification<in.project.main.entities.Page> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<Predicate>();

            // Exclude soft-deleted
            predicates.add(cb.or(cb.isNull(root.get("deleted")), cb.isFalse(root.get("deleted"))));

            // Keyword filter (title, slug, summary)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate slugMatch = cb.like(cb.lower(root.get("slug")), pattern);
                Predicate summaryMatch = cb.like(cb.lower(root.get("summary")), pattern);
                predicates.add(cb.or(titleMatch, slugMatch, summaryMatch));
            }

            // Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Visibility filter
            if (visibility != null) {
                predicates.add(cb.equal(root.get("visibility"), visibility));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return pageRepository.findAll(spec, pageable);
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> metrics = new HashMap<>();
        long total = pageRepository.countByDeletedFalse();
        long published = pageRepository.countByStatusAndDeletedFalse(ContentStatus.PUBLISHED);
        long draft = pageRepository.countByStatusAndDeletedFalse(ContentStatus.DRAFT);
        long scheduled = pageRepository.countByStatusAndDeletedFalse(ContentStatus.SCHEDULED);
        long archived = pageRepository.countByStatusAndDeletedFalse(ContentStatus.ARCHIVED);

        metrics.put("totalCount", total);
        metrics.put("publishedCount", published);
        metrics.put("draftCount", draft);
        metrics.put("scheduledCount", scheduled);
        metrics.put("archivedCount", archived);
        return metrics;
    }

    public Optional<in.project.main.entities.Page> findById(Long id) {
        if (id == null) return Optional.empty();
        return pageRepository.findById(id).filter(p -> !p.isDeleted());
    }

    public Optional<in.project.main.entities.Page> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return pageRepository.findBySlugAndDeletedFalse(slug.trim().toLowerCase());
    }

    // =========================================================================
    // 2. SLUG UTILITIES
    // =========================================================================

    public String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "page-" + System.currentTimeMillis();
        }
        String normalized = input.trim().toLowerCase();
        normalized = normalized.replaceAll("[\\s_]+", "-");
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll("");
        normalized = MULTI_DASH.matcher(normalized).replaceAll("-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isBlank() ? "page-" + System.currentTimeMillis() : normalized;
    }

    public String ensureUniqueSlug(String baseSlug, Long excludeId) {
        String cleanSlug = slugify(baseSlug);
        String candidate = cleanSlug;
        int counter = 1;

        while (true) {
            boolean exists;
            if (excludeId != null) {
                exists = pageRepository.existsBySlugAndIdNot(candidate, excludeId);
            } else {
                exists = pageRepository.existsBySlug(candidate);
            }

            if (!exists) {
                return candidate;
            }
            candidate = cleanSlug + "-" + counter;
            counter++;
        }
    }

    // =========================================================================
    // 3. CRUD MUTATIONS
    // =========================================================================

    @Transactional
    public in.project.main.entities.Page createPage(in.project.main.entities.Page page, String authorEmail) {
        if (page.getSlug() == null || page.getSlug().isBlank()) {
            page.setSlug(slugify(page.getTitle()));
        } else {
            page.setSlug(slugify(page.getSlug()));
        }
        page.setSlug(ensureUniqueSlug(page.getSlug(), null));

        if (page.getStatus() == null) {
            page.setStatus(ContentStatus.DRAFT);
        }
        if (page.getVisibility() == null) {
            page.setVisibility(ContentVisibility.PUBLIC);
        }
        if (authorEmail != null) {
            page.setAuthorEmail(authorEmail);
        }
        if (page.getStatus() == ContentStatus.PUBLISHED && page.getPublishedAt() == null) {
            page.setPublishedAt(LocalDateTime.now());
        }
        page.setDeleted(false);

        in.project.main.entities.Page saved = pageRepository.save(page);
        logAudit(authorEmail, AuditEventType.COURSE_CREATED, "PAGE_CREATE",
                "Created new CMS page '" + saved.getTitle() + "' (" + saved.getSlug() + ")", saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public in.project.main.entities.Page updatePage(Long id, in.project.main.entities.Page form, String editorEmail) {
        in.project.main.entities.Page existing = pageRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Page not found with ID: " + id));

        existing.setTitle(form.getTitle());

        String targetSlug = (form.getSlug() != null && !form.getSlug().isBlank())
                ? form.getSlug()
                : form.getTitle();
        existing.setSlug(ensureUniqueSlug(targetSlug, id));

        existing.setSummary(form.getSummary());
        existing.setContent(form.getContent());
        existing.setFeaturedImage(form.getFeaturedImage());

        ContentStatus prevStatus = existing.getStatus();
        existing.setStatus(form.getStatus() != null ? form.getStatus() : ContentStatus.DRAFT);
        existing.setVisibility(form.getVisibility() != null ? form.getVisibility() : ContentVisibility.PUBLIC);

        if (existing.getStatus() == ContentStatus.PUBLISHED && (existing.getPublishedAt() == null || prevStatus != ContentStatus.PUBLISHED)) {
            existing.setPublishedAt(LocalDateTime.now());
        }
        existing.setScheduledAt(form.getScheduledAt());

        // SEO Fields
        existing.setSeoTitle(form.getSeoTitle());
        existing.setSeoDescription(form.getSeoDescription());
        existing.setSeoKeywords(form.getSeoKeywords());
        existing.setCanonicalUrl(form.getCanonicalUrl());

        in.project.main.entities.Page updated = pageRepository.save(existing);
        logAudit(editorEmail, AuditEventType.COURSE_UPDATED, "PAGE_UPDATE",
                "Updated CMS page '" + updated.getTitle() + "' (ID: " + updated.getId() + ")", updated.getId(), updated.getTitle());
        return updated;
    }

    @Transactional
    public in.project.main.entities.Page toggleStatus(Long id, String editorEmail) {
        in.project.main.entities.Page page = pageRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Page not found with ID: " + id));

        if (page.getStatus() == ContentStatus.PUBLISHED) {
            page.setStatus(ContentStatus.DRAFT);
        } else {
            page.setStatus(ContentStatus.PUBLISHED);
            if (page.getPublishedAt() == null) {
                page.setPublishedAt(LocalDateTime.now());
            }
        }

        in.project.main.entities.Page saved = pageRepository.save(page);
        logAudit(editorEmail, AuditEventType.COURSE_UPDATED, "PAGE_STATUS_TOGGLE",
                "Changed status of page '" + saved.getTitle() + "' to " + saved.getStatus(), saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public void deletePage(Long id, String editorEmail) {
        in.project.main.entities.Page page = pageRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Page not found with ID: " + id));

        page.setDeleted(true);
        pageRepository.save(page);

        logAudit(editorEmail, AuditEventType.COURSE_DELETED, "PAGE_DELETE",
                "Soft-deleted CMS page '" + page.getTitle() + "' (ID: " + page.getId() + ")", page.getId(), page.getTitle());
    }

    // =========================================================================
    // 4. DEFAULT STARTER PAGES SEEDER
    // =========================================================================

    @Transactional
    public int seedDefaultPagesIfEmpty(String actorEmail) {
        long count = pageRepository.countByDeletedFalse();
        if (count > 0) {
            return 0;
        }

        int seeded = 0;

        // 1. About Us
        in.project.main.entities.Page about = new in.project.main.entities.Page();
        about.setTitle("About EduTake");
        about.setSlug("about-us");
        about.setSummary("Discover our mission, vision, and passion for empowering learners with real-world industry skills.");
        about.setContent("""
                <h2>Empowering Careers Through High-Impact Learning</h2>
                <p>Welcome to <strong>EduTake</strong>, a modern learning management and career acceleration ecosystem. Our platform bridges the critical gap between academic theory and industry reality by offering outcome-driven, project-based courses taught by seasoned professionals.</p>
                
                <h3>Our Core Mission</h3>
                <p>We believe high-quality education should be accessible, engaging, and practical. Whether you are mastering full-stack web engineering, artificial intelligence, cloud architecture, or data analytics, our curriculum is engineered to build portfolio-ready skills.</p>
                
                <h3>Why Students Choose EduTake</h3>
                <ul>
                    <li><strong>Hands-On Projects:</strong> Learn by building real, production-ready software solutions.</li>
                    <li><strong>Expert Mentorship:</strong> Direct guidance from industry leads and experienced educators.</li>
                    <li><strong>Verified Certifications:</strong> Tamper-proof, cryptographically verifiable certificates for your career credentials.</li>
                    <li><strong>Lifetime Learning Community:</strong> Connect with ambitious peers, alumni, and hiring partners.</li>
                </ul>
                
                <h3>Contact & Campus</h3>
                <p>Have questions or wish to partner with us? Reach out directly via our <a href="/contact">Contact Page</a> or write to <code>support@edutake.com</code>.</p>
                """);
        about.setStatus(ContentStatus.PUBLISHED);
        about.setVisibility(ContentVisibility.PUBLIC);
        about.setSeoTitle("About EduTake | Transforming Online Education");
        about.setSeoDescription("Learn about EduTake's mission, expert instructors, and practical learning methodology designed for modern professionals.");
        about.setSeoKeywords("edutake, about us, online learning, edtech, coding bootcamp, career acceleration");
        about.setPublishedAt(LocalDateTime.now().minusDays(30));
        about.setAuthorEmail(actorEmail != null ? actorEmail : "admin@edutake.com");
        createPage(about, actorEmail);
        seeded++;

        // 2. Terms of Service
        in.project.main.entities.Page terms = new in.project.main.entities.Page();
        terms.setTitle("Terms of Service");
        terms.setSlug("terms-of-service");
        terms.setSummary("Standard legal terms and conditions governing the use of EduTake platform and courses.");
        terms.setContent("""
                <h2>Terms and Conditions of Use</h2>
                <p><em>Last Updated: September 2026</em></p>
                <p>Please read these Terms of Service carefully before accessing or using EduTake. By enrolling in courses or accessing our platform, you agree to be bound by these terms.</p>
                
                <h3>1. Account Registration and Security</h3>
                <p>Users must provide accurate, complete information during registration. You are solely responsible for maintaining the confidentiality of your account credentials and for all activities that occur under your account.</p>
                
                <h3>2. Course Enrollment and Intellectual Property</h3>
                <p>All video lectures, assignments, source code, and educational materials provided on EduTake are protected by copyright and intellectual property laws. Enrollment grants a personal, non-transferable, single-user license to view the materials. Unauthorized reproduction, distribution, or public broadcasting is strictly prohibited.</p>
                
                <h3>3. Payments and Pricing</h3>
                <p>All prices are listed in INR / USD as specified at checkout. Transactions are processed securely through authorized payment gateways (e.g., Razorpay). EduTake reserves the right to modify course prices or launch promotional discounts at any time.</p>
                
                <h3>4. Code of Conduct</h3>
                <p>Students and instructors must maintain a respectful, professional learning environment. Hate speech, harassment, academic dishonesty, plagiarism, or unauthorized spamming will lead to immediate account termination without refund.</p>
                
                <h3>5. Limitation of Liability</h3>
                <p>EduTake provides courses on an "as-is" basis. While we strive for excellence, we make no guarantees regarding specific employment outcomes or salary increments.</p>
                """);
        terms.setStatus(ContentStatus.PUBLISHED);
        terms.setVisibility(ContentVisibility.PUBLIC);
        terms.setSeoTitle("Terms of Service | EduTake Official Legal Policy");
        terms.setSeoDescription("Official terms of service governing user accounts, payments, intellectual property, and platform usage at EduTake.");
        terms.setSeoKeywords("terms of service, legal, user agreement, edutake terms, copyright");
        terms.setPublishedAt(LocalDateTime.now().minusDays(25));
        terms.setAuthorEmail(actorEmail != null ? actorEmail : "admin@edutake.com");
        createPage(terms, actorEmail);
        seeded++;

        // 3. Privacy Policy
        in.project.main.entities.Page privacy = new in.project.main.entities.Page();
        privacy.setTitle("Privacy Policy");
        privacy.setSlug("privacy-policy");
        privacy.setSummary("How EduTake collects, secures, and handles user personal information and learning analytics.");
        privacy.setContent("""
                <h2>Privacy and Data Protection Policy</h2>
                <p><em>Effective Date: September 2026</em></p>
                <p>At EduTake, your privacy and data security are our top priorities. This Privacy Policy details the types of information we collect, how it is used, and the steps we take to protect your data.</p>
                
                <h3>1. Information We Collect</h3>
                <ul>
                    <li><strong>Personal Details:</strong> Name, email address, phone number, and profile details provided during signup.</li>
                    <li><strong>Learning Analytics:</strong> Course progress, quiz scores, assignment submissions, video watch duration, and certificates earned.</li>
                    <li><strong>Payment Data:</strong> Transaction tokens and receipts (we do not store raw credit/debit card numbers; transactions are handled securely by Razorpay).</li>
                </ul>
                
                <h3>2. How We Use Your Information</h3>
                <p>We use your information strictly to provide educational services, issue course completion certificates, notify you of updates, and continuously improve platform performance.</p>
                
                <h3>3. Data Sharing & Third Parties</h3>
                <p>We do not sell or rent your personal data to advertisers. Data is only shared with essential service providers (cloud infrastructure, transactional email gateways, payment processors) required for platform operation.</p>
                
                <h3>4. Data Security</h3>
                <p>We utilize enterprise-grade SSL/TLS encryption, salted password hashing, role-based access control, and routine vulnerability scanning to safeguard your information.</p>
                
                <h3>5. Contact Us Regarding Privacy</h3>
                <p>For data privacy queries or account deletion requests, email our Data Protection Officer at <code>privacy@edutake.com</code>.</p>
                """);
        privacy.setStatus(ContentStatus.PUBLISHED);
        privacy.setVisibility(ContentVisibility.PUBLIC);
        privacy.setSeoTitle("Privacy Policy | EduTake Data Protection Guidelines");
        privacy.setSeoDescription("Read EduTake's Privacy Policy to understand how your personal data, payments, and learning history are kept secure.");
        privacy.setSeoKeywords("privacy policy, data security, gdpr, edutake privacy, user data protection");
        privacy.setPublishedAt(LocalDateTime.now().minusDays(20));
        privacy.setAuthorEmail(actorEmail != null ? actorEmail : "admin@edutake.com");
        createPage(privacy, actorEmail);
        seeded++;

        // 4. Refund Policy
        in.project.main.entities.Page refund = new in.project.main.entities.Page();
        refund.setTitle("Refund and Cancellation Policy");
        refund.setSlug("refund-policy");
        refund.setSummary("Clear 7-day money-back guarantee terms and transparent refund processing procedures.");
        refund.setContent("""
                <h2>Refund & Cancellation Policy</h2>
                <p>We want you to be completely satisfied with your learning experience at EduTake. That's why we offer a transparent, student-first refund policy.</p>
                
                <h3>7-Day Money-Back Guarantee</h3>
                <p>If you are not satisfied with a self-paced video course you purchased, you may request a full refund within <strong>7 days</strong> of the purchase date, provided:</p>
                <ul>
                    <li>You have completed less than <strong>25%</strong> of the course video content.</li>
                    <li>You have not downloaded course certificates or final project solutions.</li>
                </ul>
                
                <h3>Non-Refundable Items</h3>
                <p>Refunds are not applicable for live 1-on-1 mentorship sessions already conducted, corporate customized workshops, or purchases made using non-refundable special event discount codes.</p>
                
                <h3>How to Request a Refund</h3>
                <p>To initiate a refund, submit a request via your student dashboard or email <code>support@edutake.com</code> with your Order ID and registered email address. Approved refunds are credited back to the original payment method within 5–7 business days.</p>
                """);
        refund.setStatus(ContentStatus.PUBLISHED);
        refund.setVisibility(ContentVisibility.PUBLIC);
        refund.setSeoTitle("Refund Policy | 7-Day Money Back Guarantee at EduTake");
        refund.setSeoDescription("Transparent 7-day refund guarantee for EduTake online courses and certifications.");
        refund.setSeoKeywords("refund policy, money back guarantee, course cancellation, edutake refund");
        refund.setPublishedAt(LocalDateTime.now().minusDays(15));
        refund.setAuthorEmail(actorEmail != null ? actorEmail : "admin@edutake.com");
        createPage(refund, actorEmail);
        seeded++;

        // 5. Student Handbook
        in.project.main.entities.Page handbook = new in.project.main.entities.Page();
        handbook.setTitle("Student Handbook & Guidelines");
        handbook.setSlug("student-handbook");
        handbook.setSummary("Essential guidelines, grading policies, discussion board etiquette, and career support resources.");
        handbook.setContent("""
                <h2>Welcome to Your Learning Journey</h2>
                <p>This Student Handbook is your compass for succeeding in courses, collaborating with fellow learners, and achieving your career milestones on EduTake.</p>
                
                <h3>1. Learning Best Practices</h3>
                <p>To maximize retention and job-readiness:</p>
                <ul>
                    <li>Follow along with hands-on coding exercises rather than passively watching videos.</li>
                    <li>Complete all module quizzes and capstone project assignments.</li>
                    <li>Participate actively in the discussion forums and peer review sessions.</li>
                </ul>
                
                <h3>2. Discussion Etiquette</h3>
                <p>Our community is collaborative and welcoming to developers of all backgrounds. Treat peers with respect, avoid sharing complete assignment solutions in public threads, and provide constructive feedback during code reviews.</p>
                
                <h3>3. Certificate Criteria</h3>
                <p>Certificates of Completion are awarded when all mandatory lessons are completed and final assessments achieve a minimum score of 70%. Your certificate includes a unique verification code recognized by hiring partners.</p>
                """);
        handbook.setStatus(ContentStatus.PUBLISHED);
        handbook.setVisibility(ContentVisibility.PUBLIC);
        handbook.setSeoTitle("Student Handbook | EduTake Learning Guidelines & Tips");
        handbook.setSeoDescription("A complete guide for students on course navigation, assignment submissions, certificate verification, and career assistance.");
        handbook.setSeoKeywords("student handbook, learning guide, certificate criteria, code of conduct");
        handbook.setPublishedAt(LocalDateTime.now().minusDays(10));
        handbook.setAuthorEmail(actorEmail != null ? actorEmail : "admin@edutake.com");
        createPage(handbook, actorEmail);
        seeded++;

        logger.info("Successfully seeded {} default CMS pages.", seeded);
        return seeded;
    }

    private void logAudit(String actorEmail, AuditEventType eventType, String action, String description, Long entityId, String entityName) {
        if (auditLogService == null) return;
        try {
            PlatformAuditEvent event = PlatformAuditEvent.of(
                    actorEmail != null ? actorEmail : "SYSTEM",
                    eventType != null ? eventType : AuditEventType.COURSE_CREATED,
                    action,
                    description
            )
            .withCategory(AuditCategory.SYSTEM)
            .withEntity("PAGE", entityId != null ? String.valueOf(entityId) : null, entityName)
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(event);
        } catch (Exception e) {
            logger.debug("Failed to record audit log for page: {}", e.getMessage());
        }
    }
}
