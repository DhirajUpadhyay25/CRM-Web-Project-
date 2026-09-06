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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Blog;
import in.project.main.entities.BlogCategory;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.ContentStatus;
import in.project.main.entities.enums.ContentVisibility;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.BlogCategoryRepository;
import in.project.main.repositories.BlogRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class BlogService {

    private static final Logger logger = LoggerFactory.getLogger(BlogService.class);
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\-]+");
    private static final Pattern MULTI_DASH = Pattern.compile("-+");

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private BlogCategoryRepository blogCategoryRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    // =========================================================================
    // 1. QUERY & SEARCH
    // =========================================================================

    public Page<Blog> getBlogsPaged(
            String keyword,
            ContentStatus status,
            Long categoryId,
            ContentVisibility visibility,
            Boolean isFeatured,
            Pageable pageable) {

        Specification<Blog> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<Predicate>();

            // Exclude soft-deleted
            predicates.add(cb.or(cb.isNull(root.get("deleted")), cb.isFalse(root.get("deleted"))));

            // Keyword filter
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate slugMatch = cb.like(cb.lower(root.get("slug")), pattern);
                Predicate excerptMatch = cb.like(cb.lower(root.get("excerpt")), pattern);
                Predicate tagsMatch = cb.like(cb.lower(root.get("tags")), pattern);
                Predicate authorMatch = cb.like(cb.lower(root.get("author")), pattern);
                predicates.add(cb.or(titleMatch, slugMatch, excerptMatch, tagsMatch, authorMatch));
            }

            // Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Category filter
            if (categoryId != null && categoryId > 0) {
                predicates.add(cb.equal(root.get("blogCategory").get("id"), categoryId));
            }

            // Visibility filter
            if (visibility != null) {
                predicates.add(cb.equal(root.get("visibility"), visibility));
            }

            // Featured filter
            if (isFeatured != null) {
                predicates.add(cb.equal(root.get("isFeatured"), isFeatured));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return blogRepository.findAll(spec, pageable);
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> metrics = new HashMap<>();
        long total = blogRepository.countByDeletedFalse();
        long published = blogRepository.countByStatusAndDeletedFalse(ContentStatus.PUBLISHED);
        long draft = blogRepository.countByStatusAndDeletedFalse(ContentStatus.DRAFT);
        long scheduled = blogRepository.countByStatusAndDeletedFalse(ContentStatus.SCHEDULED);
        long archived = blogRepository.countByStatusAndDeletedFalse(ContentStatus.ARCHIVED);

        // Compute total views from all active articles
        long totalViews = 0L;
        List<Blog> allBlogs = blogRepository.findAll();
        for (Blog b : allBlogs) {
            if (!b.isDeleted() && b.getViewCount() != null) {
                totalViews += b.getViewCount();
            }
        }

        metrics.put("totalCount", total);
        metrics.put("publishedCount", published);
        metrics.put("draftCount", draft);
        metrics.put("scheduledCount", scheduled);
        metrics.put("archivedCount", archived);
        metrics.put("totalViews", totalViews);
        return metrics;
    }

    public Optional<Blog> findById(Long id) {
        if (id == null) return Optional.empty();
        return blogRepository.findById(id).filter(b -> !b.isDeleted());
    }

    public Optional<Blog> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return blogRepository.findBySlugAndDeletedFalse(slug.trim().toLowerCase());
    }

    public List<Blog> getLatestPublishedBlogs(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        return blogRepository.findPublished(pageable).getContent();
    }

    public List<Blog> getFeaturedOrLatestPublished(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        List<Blog> featured = blogRepository.findFeaturedPublished(pageable);
        if (featured.size() >= limit) {
            return featured;
        }

        // Fill remaining with latest published
        List<Blog> latest = blogRepository.findPublished(pageable).getContent();
        Map<Long, Blog> combined = new java.util.LinkedHashMap<>();
        for (Blog b : featured) combined.put(b.getId(), b);
        for (Blog b : latest) {
            if (combined.size() < limit) {
                combined.put(b.getId(), b);
            }
        }
        return new java.util.ArrayList<>(combined.values());
    }

    public List<BlogCategory> getAllCategories() {
        return blogCategoryRepository.findAllByOrderBySortOrderAsc();
    }

    public List<BlogCategory> getActiveCategories() {
        return blogCategoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    // =========================================================================
    // 2. SLUG UTILITIES
    // =========================================================================

    public String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "article-" + System.currentTimeMillis();
        }
        String normalized = input.trim().toLowerCase();
        normalized = normalized.replaceAll("[\\s_]+", "-");
        normalized = NON_ALPHANUMERIC.matcher(normalized).replaceAll("");
        normalized = MULTI_DASH.matcher(normalized).replaceAll("-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isBlank() ? "article-" + System.currentTimeMillis() : normalized;
    }

    public String ensureUniqueSlug(String baseSlug, Long excludeId) {
        String cleanSlug = slugify(baseSlug);
        String candidate = cleanSlug;
        int counter = 1;

        while (true) {
            boolean exists;
            if (excludeId != null) {
                exists = blogRepository.existsBySlugAndIdNot(candidate, excludeId);
            } else {
                exists = blogRepository.existsBySlug(candidate);
            }

            if (!exists) {
                return candidate;
            }
            candidate = cleanSlug + "-" + counter;
            counter++;
        }
    }

    // =========================================================================
    // 3. MUTATIONS & CRUD
    // =========================================================================

    @Transactional
    public Blog createBlog(Blog blog, String authorEmail) {
        if (blog.getSlug() == null || blog.getSlug().isBlank()) {
            blog.setSlug(slugify(blog.getTitle()));
        } else {
            blog.setSlug(slugify(blog.getSlug()));
        }
        blog.setSlug(ensureUniqueSlug(blog.getSlug(), null));

        if (blog.getStatus() == null) {
            blog.setStatus(ContentStatus.DRAFT);
        }
        if (blog.getVisibility() == null) {
            blog.setVisibility(ContentVisibility.PUBLIC);
        }
        if (authorEmail != null) {
            blog.setAuthorEmail(authorEmail);
            if (blog.getAuthor() == null || blog.getAuthor().isBlank()) {
                blog.setAuthor(authorEmail);
            }
        }
        if (blog.getStatus() == ContentStatus.PUBLISHED && blog.getPublishedAt() == null) {
            blog.setPublishedAt(LocalDateTime.now());
        }
        if (blog.getViewCount() == null) {
            blog.setViewCount(0L);
        }
        blog.setDeleted(false);

        Blog saved = blogRepository.save(blog);
        logAudit(authorEmail, AuditEventType.COURSE_CREATED, "BLOG_CREATE",
                "Created article '" + saved.getTitle() + "' (" + saved.getSlug() + ")", saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public Blog updateBlog(Long id, Blog form, Long categoryId, String editorEmail) {
        Blog existing = blogRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Blog not found with ID: " + id));

        existing.setTitle(form.getTitle());

        String targetSlug = (form.getSlug() != null && !form.getSlug().isBlank())
                ? form.getSlug()
                : form.getTitle();
        existing.setSlug(ensureUniqueSlug(targetSlug, id));

        existing.setExcerpt(form.getExcerpt());
        existing.setContent(form.getContent());
        existing.setFeaturedImage(form.getFeaturedImage());
        existing.setAuthor(form.getAuthor());
        existing.setTags(form.getTags());

        // Category association
        if (categoryId != null && categoryId > 0) {
            BlogCategory cat = blogCategoryRepository.findById(categoryId).orElse(null);
            existing.setBlogCategory(cat);
        } else {
            existing.setBlogCategory(null);
        }

        existing.setRelatedCourse(form.getRelatedCourse());

        ContentStatus prevStatus = existing.getStatus();
        existing.setStatus(form.getStatus() != null ? form.getStatus() : ContentStatus.DRAFT);
        existing.setVisibility(form.getVisibility() != null ? form.getVisibility() : ContentVisibility.PUBLIC);
        existing.setIsFeatured(form.isFeatured());

        if (existing.getStatus() == ContentStatus.PUBLISHED && (existing.getPublishedAt() == null || prevStatus != ContentStatus.PUBLISHED)) {
            existing.setPublishedAt(LocalDateTime.now());
        }
        existing.setScheduledAt(form.getScheduledAt());

        // SEO Fields
        existing.setSeoTitle(form.getSeoTitle());
        existing.setSeoDescription(form.getSeoDescription());

        Blog updated = blogRepository.save(existing);
        logAudit(editorEmail, AuditEventType.COURSE_UPDATED, "BLOG_UPDATE",
                "Updated article '" + updated.getTitle() + "' (ID: " + updated.getId() + ")", updated.getId(), updated.getTitle());
        return updated;
    }

    @Transactional
    public Blog toggleStatus(Long id, String editorEmail) {
        Blog blog = blogRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Blog not found with ID: " + id));

        if (blog.getStatus() == ContentStatus.PUBLISHED) {
            blog.setStatus(ContentStatus.DRAFT);
        } else {
            blog.setStatus(ContentStatus.PUBLISHED);
            if (blog.getPublishedAt() == null) {
                blog.setPublishedAt(LocalDateTime.now());
            }
        }

        Blog saved = blogRepository.save(blog);
        logAudit(editorEmail, AuditEventType.COURSE_UPDATED, "BLOG_STATUS_TOGGLE",
                "Changed status of article '" + saved.getTitle() + "' to " + saved.getStatus(), saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public Blog toggleFeatured(Long id, String editorEmail) {
        Blog blog = blogRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Blog not found with ID: " + id));

        blog.setIsFeatured(!blog.isFeatured());
        Blog saved = blogRepository.save(blog);

        logAudit(editorEmail, AuditEventType.COURSE_UPDATED, "BLOG_FEATURED_TOGGLE",
                "Set featured status of article '" + saved.getTitle() + "' to " + saved.isFeatured(), saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public void incrementViewCount(Long id) {
        try {
            blogRepository.findById(id).ifPresent(b -> {
                long current = (b.getViewCount() != null) ? b.getViewCount() : 0L;
                b.setViewCount(current + 1);
                blogRepository.save(b);
            });
        } catch (Exception e) {
            logger.debug("Could not increment view count for blog ID {}: {}", id, e.getMessage());
        }
    }

    @Transactional
    public void deleteBlog(Long id, String editorEmail) {
        Blog blog = blogRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Blog not found with ID: " + id));

        blog.setDeleted(true);
        blogRepository.save(blog);

        logAudit(editorEmail, AuditEventType.COURSE_DELETED, "BLOG_DELETE",
                "Soft-deleted article '" + blog.getTitle() + "' (ID: " + blog.getId() + ")", blog.getId(), blog.getTitle());
    }

    // =========================================================================
    // 4. DEFAULT STARTER ARTICLES & CATEGORIES SEEDER
    // =========================================================================

    @Transactional
    public int seedDefaultBlogsIfEmpty(String actorEmail) {
        // 1. Normalize existing blogs with missing slugs or status
        List<Blog> existingBlogs = blogRepository.findAll();
        for (Blog b : existingBlogs) {
            boolean changed = false;
            if (b.getSlug() == null || b.getSlug().isBlank()) {
                String fallbackTitle = (b.getTitle() != null && !b.getTitle().isBlank()) ? b.getTitle() : "article-" + b.getId();
                b.setSlug(ensureUniqueSlug(slugify(fallbackTitle), b.getId()));
                changed = true;
            }
            if (b.getStatus() == null) {
                b.setStatus(ContentStatus.PUBLISHED);
                changed = true;
            }
            if (b.getVisibility() == null) {
                b.setVisibility(ContentVisibility.PUBLIC);
                changed = true;
            }
            if (b.getPublishedAt() == null) {
                b.setPublishedAt(b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.now().minusDays(30));
                changed = true;
            }
            if (b.getViewCount() == null) {
                b.setViewCount(24L);
                changed = true;
            }
            if (b.getExcerpt() == null || b.getExcerpt().isBlank()) {
                b.setExcerpt(b.getExcerptOrTruncatedContent());
                changed = true;
            }
            if (changed) {
                blogRepository.save(b);
            }
        }

        // Seed Categories first
        BlogCategory webDev = getOrCreateCategory("Web Development", "web-development", "Frontend, Backend, and Modern Web Architectures", 1);
        BlogCategory backendCloud = getOrCreateCategory("Backend & Cloud", "backend-cloud", "Java, Spring Boot, Microservices, and Cloud Native", 2);
        BlogCategory dataAi = getOrCreateCategory("Data Science & AI", "data-science-ai", "Machine Learning, Python, and Artificial Intelligence", 3);
        BlogCategory career = getOrCreateCategory("Career & Interviews", "career-interviews", "Resume tips, coding interview questions, and tech hiring guides", 4);

        int seeded = 0;
        String defaultAuthor = (actorEmail != null) ? actorEmail : "EduTake Editorial Team";

        // 1. Full-Stack Roadmap 2026
        if (blogRepository.findBySlug("full-stack-developer-roadmap-2026").isEmpty()) {
            Blog b1 = new Blog();
            b1.setTitle("Full-Stack Developer Roadmap 2026: From Beginner to Pro");
            b1.setSlug("full-stack-developer-roadmap-2026");
            b1.setExcerpt("A comprehensive, battle-tested roadmap covering HTML, modern JavaScript/TypeScript, React, Spring Boot, databases, and Docker.");
            b1.setContent("""
                    <h2>The Modern Path to Becoming a Job-Ready Full-Stack Engineer</h2>
                    <p>The tech landscape moves fast, but foundational core principles remain timeless. In 2026, engineering teams look for developers who understand both responsive, accessible client-side UI and resilient, scalable backend architectures.</p>
                    
                    <h3>1. Frontend Fundamentals</h3>
                    <p>Master semantic HTML5, modern CSS flexbox/grid, and Tailwind CSS. Dive deep into JavaScript ES6+ (Promises, Async/Await, Closures, DOM manipulation) before jumping into React or Next.js.</p>
                    
                    <h3>2. Robust Enterprise Backend</h3>
                    <p>Java with <strong>Spring Boot 3</strong> remains the industry powerhouse for high-throughput enterprise systems. Understand Dependency Injection, RESTful API design, Hibernate JPA, Spring Security, and transaction management.</p>
                    
                    <h3>3. Database Architecture</h3>
                    <p>Learn relational schema design in PostgreSQL or MySQL, indexing strategies, normalization, and ACID guarantees, complemented by caching with Redis.</p>
                    
                    <h3>4. DevOps & Cloud Deployments</h3>
                    <p>Containerize applications with Docker, write CI/CD GitHub Actions pipelines, and deploy onto AWS or Linux cloud servers.</p>
                    
                    <blockquote>"The secret to mastering coding is deliberate daily practice. Build real projects, inspect bugs patiently, and read documentation thoroughly."</blockquote>
                    """);
            b1.setFeaturedImage("https://images.unsplash.com/photo-1498050108023-c5249f4df085?q=80&w=800");
            b1.setBlogCategory(webDev);
            b1.setTags("fullstack, javascript, java, springboot, react, webdev");
            b1.setAuthor(defaultAuthor);
            b1.setAuthorEmail(actorEmail != null ? actorEmail : "admin@edutake.com");
            b1.setStatus(ContentStatus.PUBLISHED);
            b1.setVisibility(ContentVisibility.PUBLIC);
            b1.setIsFeatured(true);
            b1.setViewCount(342L);
            b1.setSeoTitle("Full-Stack Developer Roadmap 2026 | Complete Learning Guide");
            b1.setSeoDescription("Step-by-step roadmap to become a professional full-stack software engineer in 2026.");
            b1.setPublishedAt(LocalDateTime.now().minusDays(18));
            createBlog(b1, actorEmail);
            seeded++;
        }

        // 2. Mastering Spring Boot 3
        if (blogRepository.findBySlug("mastering-spring-boot-3-microservices").isEmpty()) {
            Blog b2 = new Blog();
            b2.setTitle("Mastering Spring Boot 3 & Microservices Architecture");
            b2.setSlug("mastering-spring-boot-3-microservices");
            b2.setExcerpt("Deep dive into building scalable distributed systems, REST APIs, JPA optimizations, and secure JWT authentication in Spring Boot 3.");
            b2.setContent("""
                    <h2>Building Resilient Distributed Systems with Java & Spring</h2>
                    <p>Spring Boot 3 represents a massive leap forward for the Java ecosystem, featuring native image compilation with GraalVM, improved observability via Micrometer, and Jakarta EE 10 integration.</p>
                    
                    <h3>Key Architecture Principles for Microservices:</h3>
                    <ul>
                        <li><strong>Single Responsibility:</strong> Each service owns its data and business domain boundary.</li>
                        <li><strong>Stateless Authentication:</strong> Use Spring Security with JWT or OAuth2 tokens for zero-session backend clusters.</li>
                        <li><strong>Database Per Service:</strong> Decouple schemas to prevent cross-service database locks and coupling.</li>
                        <li><strong>API Gateway & Routing:</strong> Centralize CORS, rate limiting, and SSL termination.</li>
                    </ul>
                    
                    <h3>Database Performance & Connection Pooling</h3>
                    <p>Always tune HikariCP connection pools and avoid N+1 query traps in Hibernate by using entity graphs and targeted DTO projections.</p>
                    """);
            b2.setFeaturedImage("https://images.unsplash.com/photo-1555066931-4365d14bab8c?q=80&w=800");
            b2.setBlogCategory(backendCloud);
            b2.setTags("java, springboot, microservices, backend, architecture, api");
            b2.setAuthor(defaultAuthor);
            b2.setAuthorEmail(actorEmail != null ? actorEmail : "admin@edutake.com");
            b2.setStatus(ContentStatus.PUBLISHED);
            b2.setVisibility(ContentVisibility.PUBLIC);
            b2.setIsFeatured(true);
            b2.setViewCount(518L);
            b2.setSeoTitle("Mastering Spring Boot 3 & Microservices Architecture | EduTake");
            b2.setSeoDescription("Learn how to architect and scale production microservices with Spring Boot 3 and Java.");
            b2.setPublishedAt(LocalDateTime.now().minusDays(12));
            createBlog(b2, actorEmail);
            seeded++;
        }

        // 3. Top Interview Tips
        if (blogRepository.findBySlug("top-15-coding-interview-tips").isEmpty()) {
            Blog b3 = new Blog();
            b3.setTitle("Top 15 System Design & Coding Interview Tips for Tech Jobs");
            b3.setSlug("top-15-coding-interview-tips");
            b3.setExcerpt("Proven strategies from senior software engineers to clear data structures, algorithms, and system design interviews with confidence.");
            b3.setContent("""
                    <h2>Ace Your Next Technical Interview with Structured Preparation</h2>
                    <p>Cracking top-tier software engineering interviews requires more than just memorizing LeetCode algorithms—it requires clear technical communication and systematic problem breakdown.</p>
                    
                    <h3>1. Understand the Problem Space First</h3>
                    <p>Never start coding immediately. Ask clarifying questions regarding edge cases, input scale, expected time/space complexity constraints, and memory limits.</p>
                    
                    <h3>2. Think Out Loud</h3>
                    <p>Interviewers evaluate your thought process. Talk through naive brute-force solutions first, identify bottlenecks, and explain why a particular data structure (hash map, two-pointer, priority queue) provides the optimal trade-off.</p>
                    
                    <h3>3. System Design Framework</h3>
                    <p>When designing large-scale systems (like a URL shortener, rate limiter, or notification engine):</p>
                    <ul>
                        <li>Clarify functional & non-functional requirements (latency, availability, consistency).</li>
                        <li>Estimate capacity, storage, and QPS (Queries Per Second).</li>
                        <li>Design high-level API contracts and database schema.</li>
                        <li>Discuss scaling bottlenecks (caching, database sharding, load balancing).</li>
                    </ul>
                    """);
            b3.setFeaturedImage("https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?q=80&w=800");
            b3.setBlogCategory(career);
            b3.setTags("interview, career, dsa, system-design, tech-jobs, hiring");
            b3.setAuthor(defaultAuthor);
            b3.setAuthorEmail(actorEmail != null ? actorEmail : "admin@edutake.com");
            b3.setStatus(ContentStatus.PUBLISHED);
            b3.setVisibility(ContentVisibility.PUBLIC);
            b3.setIsFeatured(true);
            b3.setViewCount(684L);
            b3.setSeoTitle("Top 15 System Design & Coding Interview Tips | EduTake Career Guide");
            b3.setSeoDescription("Practical guide to clear technical software engineering and system design interviews.");
            b3.setPublishedAt(LocalDateTime.now().minusDays(7));
            createBlog(b3, actorEmail);
            seeded++;
        }

        // 4. Intro to Generative AI
        if (blogRepository.findBySlug("intro-to-generative-ai-python").isEmpty()) {
            Blog b4 = new Blog();
            b4.setTitle("Introduction to Generative AI & LLM Applications with Python");
            b4.setSlug("intro-to-generative-ai-python");
            b4.setExcerpt("A beginner-friendly guide to Large Language Models, embeddings, vector databases, and building AI agents using Python.");
            b4.setContent("""
                    <h2>Demystifying Generative AI and Modern Language Models</h2>
                    <p>Artificial Intelligence is transforming how we build software. As software engineers, integrating LLM capabilities into real-world applications is rapidly becoming a core skill.</p>
                    
                    <h3>Core Concepts to Understand:</h3>
                    <ul>
                        <li><strong>Tokens & Embeddings:</strong> How text is converted into high-dimensional vector space representations.</li>
                        <li><strong>RAG (Retrieval-Augmented Generation):</strong> Feeding private domain knowledge to LLMs using vector databases like ChromaDB or Pinecone.</li>
                        <li><strong>Prompt Engineering & Function Calling:</strong> Directing models to return structured JSON and execute tools.</li>
                    </ul>
                    <p>Start with simple Python scripts calling model APIs, build small chatbots with vector context, and evaluate hallucination mitigation strategies.</p>
                    """);
            b4.setFeaturedImage("https://images.unsplash.com/photo-1677442136019-21780ecad995?q=80&w=800");
            b4.setBlogCategory(dataAi);
            b4.setTags("ai, python, machine-learning, llm, rag, data-science");
            b4.setAuthor(defaultAuthor);
            b4.setAuthorEmail(actorEmail != null ? actorEmail : "admin@edutake.com");
            b4.setStatus(ContentStatus.PUBLISHED);
            b4.setVisibility(ContentVisibility.PUBLIC);
            b4.setIsFeatured(false);
            b4.setViewCount(275L);
            b4.setSeoTitle("Intro to Generative AI with Python | EduTake Machine Learning");
            b4.setSeoDescription("Learn the fundamentals of Generative AI, embeddings, and RAG architectures in Python.");
            b4.setPublishedAt(LocalDateTime.now().minusDays(4));
            createBlog(b4, actorEmail);
            seeded++;
        }

        logger.info("Successfully seeded {} default blog articles and categories.", seeded);
        return seeded;
    }

    private BlogCategory getOrCreateCategory(String name, String slug, String description, int sortOrder) {
        return blogCategoryRepository.findBySlug(slug).orElseGet(() -> {
            BlogCategory cat = new BlogCategory();
            cat.setName(name);
            cat.setSlug(slug);
            cat.setDescription(description);
            cat.setSortOrder(sortOrder);
            cat.setActive(true);
            return blogCategoryRepository.save(cat);
        });
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
            .withEntity("BLOG", entityId != null ? String.valueOf(entityId) : null, entityName)
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO);

            auditLogService.record(event);
        } catch (Exception e) {
            logger.debug("Failed to record audit log for blog: {}", e.getMessage());
        }
    }
}
