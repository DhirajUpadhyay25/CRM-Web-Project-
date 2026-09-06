package in.project.main.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import in.project.main.entities.Faq;
import in.project.main.entities.FaqCategory;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.ContentVisibility;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.FaqCategoryRepository;
import in.project.main.repositories.FaqRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class FaqService {

    private static final Logger logger = LoggerFactory.getLogger(FaqService.class);

    @Autowired
    private FaqRepository faqRepository;

    @Autowired
    private FaqCategoryRepository faqCategoryRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    // ==========================================
    // Query & Filtering
    // ==========================================

    @Transactional(readOnly = true)
    public Page<Faq> getFaqs(String keyword, Long categoryId, String contextTag, Boolean isActive, ContentVisibility visibility, Pageable pageable) {
        Specification<Faq> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate qPredicate = cb.like(cb.lower(root.get("question")), searchPattern);
                Predicate aPredicate = cb.like(cb.lower(root.get("answer")), searchPattern);
                predicates.add(cb.or(qPredicate, aPredicate));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("faqCategory").get("id"), categoryId));
            }

            if (contextTag != null && !contextTag.trim().isEmpty() && !"ALL".equalsIgnoreCase(contextTag.trim())) {
                predicates.add(cb.equal(root.get("faqCategory").get("contextTag"), contextTag.trim()));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (visibility != null) {
                predicates.add(cb.equal(root.get("visibility"), visibility));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return faqRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Faq> getFaqById(Long id) {
        return faqRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Faq> getActiveFaqs() {
        return faqRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Faq> getActiveFaqsByCategory(Long categoryId) {
        return faqRepository.findByFaqCategoryIdAndIsActiveTrueOrderBySortOrderAsc(categoryId);
    }

    @Transactional(readOnly = true)
    public List<Faq> getActiveFaqsByCategorySlug(String slug) {
        return faqRepository.findByCategorySlug(slug);
    }

    @Transactional(readOnly = true)
    public List<Faq> getActiveFaqsByContextTag(String contextTag) {
        return faqRepository.findByContextTag(contextTag);
    }

    @Transactional(readOnly = true)
    public List<Faq> searchActiveFaqs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getActiveFaqs();
        }
        return faqRepository.searchActive(keyword.trim());
    }

    @Transactional(readOnly = true)
    public Map<FaqCategory, List<Faq>> getPublicFaqsGroupedByCategory(String search) {
        Map<FaqCategory, List<Faq>> grouped = new LinkedHashMap<>();
        List<FaqCategory> categories = faqCategoryRepository.findByIsActiveTrueOrderBySortOrderAsc();

        for (FaqCategory cat : categories) {
            List<Faq> faqs;
            if (search != null && !search.trim().isEmpty()) {
                String term = search.trim().toLowerCase();
                faqs = faqRepository.findByFaqCategoryIdAndIsActiveTrueOrderBySortOrderAsc(cat.getId())
                        .stream()
                        .filter(f -> (f.getQuestion() != null && f.getQuestion().toLowerCase().contains(term))
                                || (f.getAnswer() != null && f.getAnswer().toLowerCase().contains(term)))
                        .toList();
            } else {
                faqs = faqRepository.findByFaqCategoryIdAndIsActiveTrueOrderBySortOrderAsc(cat.getId());
            }

            if (!faqs.isEmpty()) {
                grouped.put(cat, faqs);
            }
        }
        return grouped;
    }

    // ==========================================
    // Analytics & Metrics
    // ==========================================

    @Transactional(readOnly = true)
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        long totalFaqs = faqRepository.count();
        long activeFaqs = faqRepository.countByIsActiveTrue();
        long inactiveFaqs = totalFaqs - activeFaqs;
        long totalCategories = faqCategoryRepository.count();
        long totalViews = faqRepository.sumTotalViews();
        long totalHelpful = faqRepository.sumHelpfulCount();
        long totalNotHelpful = faqRepository.sumNotHelpfulCount();

        long feedbackTotal = totalHelpful + totalNotHelpful;
        int satisfactionRate = feedbackTotal > 0 ? (int) Math.round(((double) totalHelpful / feedbackTotal) * 100) : 100;

        metrics.put("totalFaqs", totalFaqs);
        metrics.put("activeFaqs", activeFaqs);
        metrics.put("inactiveFaqs", inactiveFaqs);
        metrics.put("totalCategories", totalCategories);
        metrics.put("totalViews", totalViews);
        metrics.put("totalHelpful", totalHelpful);
        metrics.put("totalNotHelpful", totalNotHelpful);
        metrics.put("satisfactionRate", satisfactionRate);

        return metrics;
    }

    // ==========================================
    // CRUD & Management Operations
    // ==========================================

    @Transactional
    public Faq saveFaq(Faq faq, Long categoryId, Long actorId, String actorEmail) {
        if (faq.getQuestion() == null || faq.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be blank.");
        }

        if (categoryId != null) {
            FaqCategory category = faqCategoryRepository.findById(categoryId).orElse(null);
            faq.setFaqCategory(category);
            if (category != null) {
                faq.setCategory(category.getName());
            }
        }

        if (faq.getSortOrder() == null) {
            faq.setSortOrder(0);
        }
        if (faq.getIsActive() == null) {
            faq.setIsActive(true);
        }
        if (faq.getVisibility() == null) {
            faq.setVisibility(ContentVisibility.PUBLIC);
        }

        boolean isNew = (faq.getId() == null);
        if (isNew) {
            faq.setCreatedBy(actorId);
            faq.setCreatedByEmail(actorEmail);
        }

        Faq saved = faqRepository.save(faq);

        logAudit(
                actorEmail,
                isNew ? AuditEventType.FAQ_CREATED : AuditEventType.FAQ_UPDATED,
                isNew ? "FAQ_CREATE" : "FAQ_UPDATE",
                "FAQ " + (isNew ? "created" : "updated") + ": '" + saved.getQuestion() + "'",
                saved.getId(),
                saved.getQuestion()
        );

        return saved;
    }

    @Transactional
    public boolean toggleActive(Long id, String actorEmail) {
        Optional<Faq> optionalFaq = faqRepository.findById(id);
        if (optionalFaq.isPresent()) {
            Faq faq = optionalFaq.get();
            boolean newState = !faq.isIsActive();
            faq.setIsActive(newState);
            faqRepository.save(faq);

            logAudit(
                    actorEmail,
                    AuditEventType.FAQ_UPDATED,
                    "FAQ_TOGGLE_ACTIVE",
                    "FAQ '" + faq.getQuestion() + "' set active=" + newState,
                    faq.getId(),
                    faq.getQuestion()
            );
            return newState;
        }
        return false;
    }

    @Transactional
    public void deleteFaq(Long id, String actorEmail) {
        faqRepository.findById(id).ifPresent(faq -> {
            faqRepository.delete(faq);
            logAudit(
                    actorEmail,
                    AuditEventType.FAQ_DELETED,
                    "FAQ_DELETE",
                    "Deleted FAQ ID " + id + ": '" + faq.getQuestion() + "'",
                    id,
                    faq.getQuestion()
            );
        });
    }

    @Transactional
    public void incrementView(Long id) {
        faqRepository.findById(id).ifPresent(faq -> {
            faq.setViewCount((faq.getViewCount() != null ? faq.getViewCount() : 0L) + 1);
            faqRepository.save(faq);
        });
    }

    @Transactional
    public Map<String, Object> voteHelpful(Long id, boolean helpful) {
        Map<String, Object> response = new HashMap<>();
        Optional<Faq> optionalFaq = faqRepository.findById(id);
        if (optionalFaq.isPresent()) {
            Faq faq = optionalFaq.get();
            if (helpful) {
                faq.setHelpfulCount((faq.getHelpfulCount() != null ? faq.getHelpfulCount() : 0L) + 1);
            } else {
                faq.setNotHelpfulCount((faq.getNotHelpfulCount() != null ? faq.getNotHelpfulCount() : 0L) + 1);
            }
            faqRepository.save(faq);

            response.put("success", true);
            response.put("helpfulCount", faq.getHelpfulCount());
            response.put("notHelpfulCount", faq.getNotHelpfulCount());
            response.put("satisfactionRate", faq.getHelpfulnessRate());
            return response;
        }
        response.put("success", false);
        response.put("error", "FAQ not found");
        return response;
    }

    // ==========================================
    // Category Operations
    // ==========================================

    @Transactional(readOnly = true)
    public List<FaqCategory> getAllCategories() {
        return faqCategoryRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<FaqCategory> getActiveCategories() {
        return faqCategoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public Optional<FaqCategory> getCategoryById(Long id) {
        return faqCategoryRepository.findById(id);
    }

    @Transactional
    public FaqCategory saveCategory(FaqCategory category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        if (category.getSlug() == null || category.getSlug().trim().isEmpty()) {
            category.setSlug(slugify(category.getName()));
        }
        if (category.getIconClass() == null || category.getIconClass().trim().isEmpty()) {
            category.setIconClass("bi-question-circle");
        }
        if (category.getContextTag() == null || category.getContextTag().trim().isEmpty()) {
            category.setContextTag("GENERAL");
        }
        return faqCategoryRepository.save(category);
    }

    @Transactional
    public boolean deleteCategory(Long id) {
        long count = faqRepository.countByFaqCategoryId(id);
        if (count > 0) {
            return false; // Cannot delete category that has FAQs attached
        }
        if (faqCategoryRepository.existsById(id)) {
            faqCategoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // ==========================================
    // Default Starter Data Seeder
    // ==========================================

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            seedDefaults(false);
        } catch (Exception e) {
            logger.warn("Could not auto-seed FAQ defaults on startup: {}", e.getMessage());
        }
    }

    @Transactional
    public int seedDefaults(boolean force) {
        if (!force && faqRepository.count() >= 5) {
            return 0;
        }

        logger.info("Seeding comprehensive FAQ categories and standard questions...");

        // 1. Categories
        FaqCategory catGeneral = getOrCreateCategory("General & Platform", "general-platform", "Everything about EduTake, accounts, and platform features.", "bi-info-circle", 1, "GENERAL");
        FaqCategory catAdmissions = getOrCreateCategory("Admissions & Enrollment", "admissions-enrollment", "Course discovery, batch selection, prerequisites, and registration.", "bi-mortarboard", 2, "ENROLLMENT");
        FaqCategory catClasses = getOrCreateCategory("Live Classes & LMS", "live-classes-lms", "Live sessions, class recordings, assignments, and mentor support.", "bi-camera-video", 3, "CLASS");
        FaqCategory catCertificates = getOrCreateCategory("Certificates & Placement", "certificates-placement", "Certification issuance, verification, resume reviews, and placement assistance.", "bi-award", 4, "CERTIFICATE");
        FaqCategory catPayments = getOrCreateCategory("Payments & Refunds", "payments-refunds", "Payment options, EMI, invoices, discounts, and money-back guarantees.", "bi-credit-card", 5, "PAYMENT");
        FaqCategory catTechnical = getOrCreateCategory("Technical & Support", "technical-support", "Login issues, browser compatibility, mobile access, and security.", "bi-cpu", 6, "TECHNICAL");

        int seeded = 0;

        // 2. FAQs
        seeded += seedSingleFaq(
                "How do I access my enrolled courses and live workshops?",
                "Once your enrollment or purchase is confirmed, log in to your EduTake account and navigate to <strong>My Learning / Dashboard</strong> from the top navigation bar. All active courses, video modules, live session schedules, and learning materials will be immediately accessible with lifetime validity.",
                catGeneral, 1, ContentVisibility.PUBLIC, 342L, 48L, 2L
        );

        seeded += seedSingleFaq(
                "What is the format of the courses (Self-paced vs Live)?",
                "EduTake provides hybrid learning formats. Courses include comprehensive on-demand video lectures, interactive coding sandboxes, quizzes, and weekly live mentorship workshops where industry mentors review capstone projects and resolve doubts in real-time.",
                catGeneral, 2, ContentVisibility.PUBLIC, 215L, 30L, 1L
        );

        seeded += seedSingleFaq(
                "Can I enroll in multiple courses simultaneously?",
                "Yes, you can enroll in multiple courses or learning tracks simultaneously. Your unified student dashboard will display progress indicators, upcoming batch deadlines, and schedules for each enrolled track separately.",
                catAdmissions, 1, ContentVisibility.PUBLIC, 180L, 24L, 0L
        );

        seeded += seedSingleFaq(
                "Are there any prerequisites before enrolling in advanced tracks?",
                "Each course detail page outlines recommended prerequisites. While foundational tracks are beginner-friendly with zero prior coding required, specialized masterclasses (e.g. Microservices with Spring Boot, Advanced System Design) assume basic programming literacy in Java or Python.",
                catAdmissions, 2, ContentVisibility.PUBLIC, 160L, 19L, 1L
        );

        seeded += seedSingleFaq(
                "What happens if I miss a scheduled live class or interactive workshop?",
                "Do not worry! Every live workshop and interactive mentoring session is automatically recorded in HD quality and uploaded to your student portal within 12 hours of the class conclusion, along with accompanying code repos and lecture slides.",
                catClasses, 1, ContentVisibility.PUBLIC, 410L, 62L, 3L
        );

        seeded += seedSingleFaq(
                "How do I submit assignments and get instructor feedback?",
                "Under each module in your Course Player, you will find an <em>Assignments</em> tab. You can upload project archives or submit GitHub repository URLs. Your instructor will grade your submission, provide code review annotations, and assign a score within 48 business hours.",
                catClasses, 2, ContentVisibility.PUBLIC, 280L, 35L, 2L
        );

        seeded += seedSingleFaq(
                "Do you provide industry-recognized, verifiable certificates?",
                "Yes! Upon achieving a 100% course completion rate and passing the final capstone assessment, a cryptographically signed Digital Certificate of Completion is issued with a unique verification URL and QR code suitable for LinkedIn and employer validation.",
                catCertificates, 1, ContentVisibility.PUBLIC, 520L, 78L, 4L
        );

        seeded += seedSingleFaq(
                "Do you offer career guidance and placement assistance?",
                "Students enrolled in our Pro and Master tracks receive complimentary resume reviews, LinkedIn profile optimization, mock interview simulations with tech leads, and direct referrals through our 150+ corporate hiring network.",
                catCertificates, 2, ContentVisibility.PUBLIC, 395L, 56L, 1L
        );

        seeded += seedSingleFaq(
                "What payment methods and EMI options are supported?",
                "We accept all major credit cards (Visa, MasterCard, American Express), debit cards, UPI / QR payments, Net Banking, and flexible no-cost EMI options across leading banks (3, 6, 9, and 12-month tenures).",
                catPayments, 1, ContentVisibility.PUBLIC, 310L, 40L, 2L
        );

        seeded += seedSingleFaq(
                "What is your refund policy if I am not satisfied?",
                "We stand by the quality of our curriculum with a <strong>7-Day No-Questions-Asked Money-Back Guarantee</strong>. If you feel the course is not right for you within 7 days of purchase, submit a refund request from your dashboard or email support@edutake.com for a 100% prompt refund.",
                catPayments, 2, ContentVisibility.PUBLIC, 490L, 72L, 5L
        );

        seeded += seedSingleFaq(
                "Which browsers and operating systems are supported?",
                "EduTake is fully optimized for all modern web browsers including Google Chrome, Mozilla Firefox, Microsoft Edge, and Safari on Windows, macOS, Linux, Android, and iOS devices without requiring third-party plugins.",
                catTechnical, 1, ContentVisibility.PUBLIC, 195L, 28L, 0L
        );

        seeded += seedSingleFaq(
                "How do I reset my password or update my registered email?",
                "Click on the 'Forgot Password' link on the login page to receive a secure password reset link via email. To update your profile details or registered email, visit your <strong>Profile Settings</strong> inside the student control panel.",
                catTechnical, 2, ContentVisibility.PUBLIC, 230L, 31L, 1L
        );

        logger.info("Successfully seeded {} default FAQs across {} categories.", seeded, 6);
        return seeded;
    }

    private FaqCategory getOrCreateCategory(String name, String slug, String description, String iconClass, int sortOrder, String contextTag) {
        return faqCategoryRepository.findBySlug(slug).orElseGet(() -> {
            FaqCategory cat = new FaqCategory();
            cat.setName(name);
            cat.setSlug(slug);
            cat.setDescription(description);
            cat.setIconClass(iconClass);
            cat.setSortOrder(sortOrder);
            cat.setActive(true);
            cat.setContextTag(contextTag);
            return faqCategoryRepository.save(cat);
        });
    }

    private int seedSingleFaq(String question, String answer, FaqCategory category, int sortOrder, ContentVisibility visibility, long views, long helpful, long notHelpful) {
        List<Faq> existing = faqRepository.searchActive(question.trim());
        if (!existing.isEmpty()) {
            return 0;
        }

        Faq faq = new Faq();
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setFaqCategory(category);
        faq.setCategory(category != null ? category.getName() : "General");
        faq.setSortOrder(sortOrder);
        faq.setIsActive(true);
        faq.setVisibility(visibility != null ? visibility : ContentVisibility.PUBLIC);
        faq.setViewCount(views);
        faq.setHelpfulCount(helpful);
        faq.setNotHelpfulCount(notHelpful);
        faq.setCreatedByEmail("system@edutake.com");

        faqRepository.save(faq);
        return 1;
    }

    private String slugify(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private void logAudit(String actorEmail, AuditEventType eventType, String action, String description, Long entityId, String entityName) {
        if (auditLogService == null) return;
        try {
            PlatformAuditEvent event = PlatformAuditEvent.of(
                    actorEmail != null ? actorEmail : "SYSTEM",
                    eventType != null ? eventType : AuditEventType.FAQ_UPDATED,
                    action,
                    description
            )
            .withCategory(AuditCategory.SYSTEM)
            .withEntity("FAQ", entityId != null ? String.valueOf(entityId) : null, entityName)
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(event);
        } catch (Exception e) {
            logger.debug("Failed to record audit log for FAQ: {}", e.getMessage());
        }
    }
}
