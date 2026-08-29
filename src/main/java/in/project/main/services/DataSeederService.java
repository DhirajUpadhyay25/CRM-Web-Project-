package in.project.main.services;

import in.project.main.entities.*;
import in.project.main.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class DataSeederService {

    @Autowired private InstructorRepository instructorRepo;
    @Autowired private BatchRepository batchRepo;
    @Autowired private LessonRepository lessonRepo;
    @Autowired private CertificateRepository certRepo;
    @Autowired private PaymentRepository paymentRepo;
    @Autowired private CouponRepository couponRepo;
    @Autowired private RefundRepository refundRepo;
    @Autowired private AnnouncementRepository announcementRepo;
    @Autowired private MessageRepository messageRepo;
    @Autowired private PageRepository pageRepo;
    @Autowired private BlogRepository blogRepo;
    @Autowired private FaqRepository faqRepo;
    @Autowired private TestimonialRepository testimonialRepo;
    @Autowired private MediaRepository mediaRepo;
    @Autowired private SystemRoleRepository roleRepo;

    public void seedAll() {
        seedInstructors();
        seedBatches();
        seedLessons();
        seedCertificates();
        seedPayments();
        seedCoupons();
        seedRefunds();
        seedAnnouncements();
        seedMessages();
        seedPages();
        seedBlogs();
        seedFaqs();
        seedTestimonials();
        seedMedia();
        seedRoles();
    }

    private void seedInstructors() {
        if (instructorRepo.count() > 0) return;
        List<String> names = Arrays.asList("John Doe", "Jane Smith", "Alan Turing", "Grace Hopper");
        for (String name : names) {
            Instructor i = new Instructor();
            i.setName(name);
            i.setEmail(name.toLowerCase().replace(" ", ".") + "@example.com");
            i.setBio("Experienced instructor in technology.");
            i.setSpecialization("Software Engineering");
            i.setStatus("Active");
            instructorRepo.save(i);
        }
    }

    private void seedBatches() {
        if (batchRepo.count() > 0) return;
        List<String> names = Arrays.asList("Spring Boot Mastery - Batch 1", "React Native 2024", "Fullstack Developer Weekend Batch");
        for (String name : names) {
            Batch b = new Batch();
            b.setName(name);
            b.setCourseId("1");
            b.setStartDate("2024-09-01");
            b.setStatus("Upcoming");
            batchRepo.save(b);
        }
    }

    private void seedLessons() {
        if (lessonRepo.count() > 0) return;
        for (int i = 1; i <= 5; i++) {
            Lesson l = new Lesson();
            l.setTitle("Lesson " + i + ": Introduction to Concepts");
            l.setCourseId("1");
            l.setSectionName("Getting Started");
            l.setOrderIndex(i);
            lessonRepo.save(l);
        }
    }

    private void seedCertificates() {
        if (certRepo.count() > 0) return;
        for (int i = 1; i <= 3; i++) {
            Certificate c = new Certificate();
            c.setCertificateCode("CERT-2024-00" + i);
            c.setEnrollmentId(String.valueOf(i));
            c.setIssueDate("2024-01-15");
            certRepo.save(c);
        }
    }

    private void seedPayments() {
        if (paymentRepo.count() > 0) return;
        List<String> methods = Arrays.asList("Credit Card", "UPI", "Net Banking");
        Random r = new Random();
        for (int i = 1; i <= 5; i++) {
            Payment p = new Payment();
            p.setOrderId("ORD-00" + i);
            p.setAmount(String.valueOf(999 + r.nextInt(4000)));
            p.setPaymentMethod(methods.get(r.nextInt(methods.size())));
            p.setStatus("Completed");
            p.setPaymentDate("2024-08-" + (10 + i));
            paymentRepo.save(p);
        }
    }

    private void seedCoupons() {
        if (couponRepo.count() > 0) return;
        Coupon c1 = new Coupon(); c1.setCode("WELCOME50"); c1.setDiscountType("PERCENTAGE"); c1.setDiscountValue("50"); c1.setExpiryDate("2024-12-31"); c1.setIsActive(true);
        Coupon c2 = new Coupon(); c2.setCode("FLAT500"); c2.setDiscountType("FIXED"); c2.setDiscountValue("500"); c2.setExpiryDate("2024-11-30"); c2.setIsActive(true);
        couponRepo.saveAll(Arrays.asList(c1, c2));
    }

    private void seedRefunds() {
        if (refundRepo.count() > 0) return;
        Refund r = new Refund();
        r.setOrderId("ORD-002");
        r.setAmount("1500");
        r.setReason("Accidental purchase");
        r.setStatus("Processed");
        r.setRefundDate("2024-08-20");
        refundRepo.save(r);
    }

    private void seedAnnouncements() {
        if (announcementRepo.count() > 0) return;
        Announcement a = new Announcement();
        a.setTitle("System Maintenance Scheduled");
        a.setContent("We will be down for maintenance this Sunday at 2 AM.");
        a.setTargetAudience("ALL");
        a.setPublishDate("2024-08-30");
        a.setIsActive(true);
        announcementRepo.save(a);
    }

    private void seedMessages() {
        if (messageRepo.count() > 0) return;
        Message m = new Message();
        m.setSenderEmail("student@example.com");
        m.setRecipientEmail("admin@edutake.com");
        m.setSubject("Question about my course");
        m.setBody("Hi, I cannot access the third module.");
        m.setIsRead(false);
        messageRepo.save(m);
    }

    private void seedPages() {
        if (pageRepo.count() > 0) return;
        Page p1 = new Page(); p1.setTitle("About Us"); p1.setSlug("about-us"); p1.setStatus("Published");
        Page p2 = new Page(); p2.setTitle("Terms of Service"); p2.setSlug("terms"); p2.setStatus("Published");
        pageRepo.saveAll(Arrays.asList(p1, p2));
    }

    private void seedBlogs() {
        if (blogRepo.count() > 0) return;
        Blog b = new Blog();
        b.setTitle("Top 10 Java Features in 2024");
        b.setAuthor("Admin");
        b.setStatus("Published");
        b.setPublishDate("2024-08-01");
        blogRepo.save(b);
    }

    private void seedFaqs() {
        if (faqRepo.count() > 0) return;
        Faq f = new Faq();
        f.setQuestion("How do I access my courses?");
        f.setAnswer("Log in to your dashboard and go to My Courses.");
        f.setCategory("General");
        f.setIsActive(true);
        faqRepo.save(f);
    }

    private void seedTestimonials() {
        if (testimonialRepo.count() > 0) return;
        Testimonial t = new Testimonial();
        t.setStudentName("Alice Johnson");
        t.setCourseName("Advanced Java");
        t.setRating(5);
        t.setReview("This course changed my career!");
        t.setIsApproved(true);
        testimonialRepo.save(t);
    }

    private void seedMedia() {
        if (mediaRepo.count() > 0) return;
        Media m = new Media();
        m.setFileName("hero-banner.jpg");
        m.setFileUrl("/uploads/hero-banner.jpg");
        m.setFileType("image/jpeg");
        m.setSize("1.2MB");
        mediaRepo.save(m);
    }

    private void seedRoles() {
        if (roleRepo.count() > 0) return;
        SystemRole r1 = new SystemRole(); r1.setRoleName("INSTRUCTOR_MANAGER"); r1.setDescription("Manage instructors and batches");
        SystemRole r2 = new SystemRole(); r2.setRoleName("SUPPORT_AGENT"); r2.setDescription("Manage enquiries and messages");
        roleRepo.saveAll(Arrays.asList(r1, r2));
    }
}
