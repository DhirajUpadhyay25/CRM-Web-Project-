package in.project.main.services;

import in.project.main.entities.*;
import in.project.main.entities.enums.*;
import in.project.main.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @Autowired private EmployeeRepository employeeRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private LeadRepository leadRepo;
    @Autowired private EnquiryRepository enquiryRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private OrdersRepository ordersRepo;

    @Transactional
    public void seedAll() {
        seedAdmin();
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
        seedLeads();
        seedEnquiries();
        seedStudentPanelData();
        migratePlaintextPasswords();
    }

    private void seedAdmin() {
        if (employeeRepo.findByEmail("admin@edutake.com") != null) return;
        Employee admin = new Employee();
        admin.setName("Administrator");
        admin.setEmail("admin@edutake.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setPhoneno("9999999999");
        admin.setCity("System");
        admin.setRole(Role.ADMIN);
        employeeRepo.save(admin);
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

    private void seedLeads() {
        if (leadRepo.count() > 0) return;
        Lead l1 = new Lead(); l1.setName("Rahul Sharma"); l1.setEmail("rahul@example.com"); l1.setPhone("9876543210"); l1.setSource("Website"); l1.setInterestedIn("Java Development"); l1.setStatus(in.project.main.entities.enums.LeadStatus.NEW);
        Lead l2 = new Lead(); l2.setName("Priya Patel"); l2.setEmail("priya@example.com"); l2.setPhone("9876543211"); l2.setSource("Referral"); l2.setInterestedIn("Data Science"); l2.setStatus(in.project.main.entities.enums.LeadStatus.CONTACTED);
        leadRepo.saveAll(Arrays.asList(l1, l2));
    }

    private void seedEnquiries() {
        if (enquiryRepo.count() > 0) return;
        Enquiry e1 = new Enquiry(); e1.setName("Amit Kumar"); e1.setEmail("amit@example.com"); e1.setPhone("9876543212"); e1.setSubject("Course duration"); e1.setMessage("How long is the Java course?"); e1.setType(in.project.main.entities.enums.EnquiryType.COURSE_ENQUIRY);
        enquiryRepo.save(e1);
    }

    @Transactional
    public void seedStudentPanelData() {
        seedCategories();
        seedCourses();
        seedStudents();
        seedEnrollmentsAndOrders();
        seedStudentNotifications();
    }

    private void seedCategories() {
        if (categoryRepo.count() > 0) return;
        String[][] categories = {
            {"Web Development", "web-development", "Learn to build modern web applications"},
            {"Data Science", "data-science", "Master data analysis and machine learning"},
            {"Mobile Development", "mobile-development", "Build iOS and Android apps"},
            {"Cloud Computing", "cloud-computing", "AWS, Azure, and GCP certifications"},
            {"Cybersecurity", "cybersecurity", "Protect systems from digital attacks"},
            {"AI & Machine Learning", "ai-machine-learning", "Build intelligent systems"}
        };
        for (String[] c : categories) {
            Category cat = new Category();
            cat.setName(c[0]);
            cat.setSlug(c[1]);
            cat.setDescription(c[2]);
            cat.setActive(true);
            categoryRepo.save(cat);
        }
    }

    private void seedCourses() {
        if (courseRepo.count() > 0) return;
        List<Category> cats = categoryRepo.findAll();
        if (cats.isEmpty()) return;

        Object[][] courses = {
            {"Complete Java Developer", "complete-java-developer", "Master Java from basics to advanced concepts", "John Doe", CourseLevel.ALL_LEVELS, "40 hours", new BigDecimal("4999"), new BigDecimal("2999"), true},
            {"React.js Masterclass", "reactjs-masterclass", "Build modern UIs with React and hooks", "Jane Smith", CourseLevel.BEGINNER, "35 hours", new BigDecimal("3999"), new BigDecimal("1999"), true},
            {"Python for Data Science", "python-for-data-science", "Learn Python programming for data analysis", "Alan Turing", CourseLevel.BEGINNER, "45 hours", new BigDecimal("5999"), new BigDecimal("3499"), true},
            {"Spring Boot Microservices", "spring-boot-microservices", "Build production-ready microservices", "John Doe", CourseLevel.ADVANCED, "50 hours", new BigDecimal("6999"), new BigDecimal("4499"), false},
            {"Flutter App Development", "flutter-app-development", "Create cross-platform mobile apps", "Grace Hopper", CourseLevel.INTERMEDIATE, "30 hours", new BigDecimal("3499"), new BigDecimal("1999"), true},
            {"AWS Cloud Practitioner", "aws-cloud-practitioner", "Prepare for AWS certification exam", "Jane Smith", CourseLevel.BEGINNER, "25 hours", new BigDecimal("2999"), new BigDecimal("1499"), false},
            {"Ethical Hacking Fundamentals", "ethical-hacking-fundamentals", "Learn penetration testing and security", "Alan Turing", CourseLevel.INTERMEDIATE, "38 hours", new BigDecimal("4499"), new BigDecimal("2499"), true},
            {"Machine Learning A-Z", "machine-learning-a-z", "Hands-on ML with Python and scikit-learn", "Grace Hopper", CourseLevel.INTERMEDIATE, "55 hours", new BigDecimal("7999"), new BigDecimal("4999"), false},
            {"JavaScript Deep Dive", "javascript-deep-dive", "Master modern JavaScript and ES6+", "John Doe", CourseLevel.ALL_LEVELS, "32 hours", new BigDecimal("2999"), new BigDecimal("1499"), true},
            {"DevOps Engineering", "devops-engineering", "CI/CD, Docker, Kubernetes, and more", "Jane Smith", CourseLevel.ADVANCED, "42 hours", new BigDecimal("5999"), new BigDecimal("3999"), false},
            {"UI/UX Design Principles", "ui-ux-design-principles", "Create beautiful user interfaces", "Grace Hopper", CourseLevel.BEGINNER, "20 hours", new BigDecimal("1999"), new BigDecimal("999"), true},
            {"Blockchain Development", "blockchain-development", "Build decentralized applications", "Alan Turing", CourseLevel.ADVANCED, "36 hours", new BigDecimal("4999"), new BigDecimal("2999"), false}
        };

        int catIndex = 0;
        for (Object[] c : courses) {
            Course course = new Course();
            course.setName((String) c[0]);
            course.setSlug((String) c[1]);
            course.setShortDescription((String) c[2]);
            course.setInstructor((String) c[3]);
            course.setLevel((CourseLevel) c[4]);
            course.setDuration((String) c[5]);
            course.setOriginalPrice((BigDecimal) c[6]);
            course.setDiscountedPrice((BigDecimal) c[7]);
            course.setFeatured((boolean) c[8]);
            course.setStatus(CourseStatus.PUBLISHED);
            course.setCategory(cats.get(catIndex % cats.size()));
            course.setLanguage("English");
            course.setDescription("Comprehensive course covering all aspects of " + c[0] + ". Includes hands-on projects, real-world examples, and certification preparation.");
            courseRepo.save(course);
            catIndex++;
        }
    }

    private void seedStudents() {
        // Always create test students if they don't exist
        String[] testEmails = {"rahul@student.com", "priya@student.com", "amit@student.com", "sneha@student.com", "vikram@student.com"};
        boolean anyExist = false;
        for (String email : testEmails) {
            if (userRepo.findByEmail(email) != null) {
                anyExist = true;
                break;
            }
        }
        if (anyExist) return;

        Object[][] students = {
            {"Rahul Sharma", "rahul@student.com", "9876543210", "Mumbai"},
            {"Priya Patel", "priya@student.com", "9876543211", "Delhi"},
            {"Amit Kumar", "amit@student.com", "9876543212", "Bangalore"},
            {"Sneha Reddy", "sneha@student.com", "9876543213", "Hyderabad"},
            {"Vikram Singh", "vikram@student.com", "9876543214", "Pune"}
        };
        for (Object[] s : students) {
            User user = new User();
            user.setName((String) s[0]);
            user.setEmail((String) s[1]);
            user.setPassword(passwordEncoder.encode("student123"));
            user.setPhoneno((String) s[2]);
            user.setCity((String) s[3]);
            userRepo.save(user);
        }
    }

    private void seedEnrollmentsAndOrders() {
        List<User> allUsers = new java.util.ArrayList<>(userRepo.findAll());
        List<Course> courses = courseRepo.findAll();
        if (allUsers.isEmpty() || courses.isEmpty()) return;

        Random r = new Random();
        String[] statuses = {"COMPLETED", "ACTIVE", "ACTIVE", "ACTIVE"};

        for (User student : allUsers) {
            // Check how many enrollments this user already has
            long existingCount = enrollmentRepo.findByUserEmailOrderByEnrolledAtDesc(student.getEmail()).size();
            if (existingCount >= 2) continue; // Already has enough data

            int numCourses = 2 + r.nextInt(3);
            List<Course> shuffled = new java.util.ArrayList<>(courses);
            java.util.Collections.shuffle(shuffled, r);

            int created = 0;
            for (int i = 0; i < Math.min(numCourses, shuffled.size()); i++) {
                Course course = shuffled.get(i);
                if (enrollmentRepo.existsByUserIdAndCourseId(student.getId(), course.getId())) continue;

                String status = statuses[r.nextInt(statuses.length)];
                LocalDateTime enrolledDaysAgo = LocalDateTime.now().minusDays(r.nextInt(30) + 1);

                Enrollment enrollment = new Enrollment();
                enrollment.setUser(student);
                enrollment.setCourse(course);
                enrollment.setStatus(EnrollmentStatus.valueOf(status));
                enrollment.setPaymentStatus(course.isFree() ? "FREE" : "PAID");
                if ("COMPLETED".equals(status)) {
                    enrollment.setCompletedAt(enrolledDaysAgo.plusDays(r.nextInt(14) + 7));
                }
                enrollmentRepo.save(enrollment);

                // Also create order if not exists
                if (!ordersRepo.existsByUserEmailAndCourseName(student.getEmail(), course.getName())) {
                    Orders order = new Orders();
                    order.setCourseName(course.getName());
                    order.setCourseAmount(course.getEffectivePrice().toPlainString());
                    order.setUserEmail(student.getEmail());
                    order.setDateOfPurchase(enrolledDaysAgo.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                    order.setOrderId("ORD_" + System.currentTimeMillis() + "_" + r.nextInt(1000));
                    order.setPaymentId("pay_" + System.currentTimeMillis() + "_" + r.nextInt(1000));
                    order.setSignature("sig_verified");
                    ordersRepo.save(order);
                }
                created++;
            }
        }
    }

    private void seedStudentNotifications() {
        List<User> students = new java.util.ArrayList<>(userRepo.findAll());
        if (students.isEmpty()) return;

        Object[][] notifs = {
            {NotificationType.COURSE_ENROLLED, "Welcome to the Course!", "You have been successfully enrolled. Start learning today!"},
            {NotificationType.ORDER_PLACED, "Order Confirmed", "Your course purchase has been confirmed."},
            {NotificationType.PAYMENT_SUCCESS, "Payment Received", "We received your payment successfully."},
            {NotificationType.COURSE_UPDATE, "Course Updated", "New content has been added to your enrolled course."},
            {NotificationType.NEW_LESSON, "New Lesson Available", "A new lesson has been published in your course."},
            {NotificationType.CERTIFICATE_READY, "Certificate Ready", "Congratulations! Your certificate is now available for download."}
        };

        Random r = new Random();
        for (User student : students) {
            int numNotifs = 3 + r.nextInt(4);
            for (int i = 0; i < numNotifs; i++) {
                Object[] n = notifs[r.nextInt(notifs.length)];
                Notification notification = new Notification();
                notification.setRecipientEmail(student.getEmail());
                notification.setType((NotificationType) n[0]);
                notification.setTitle((String) n[1]);
                notification.setMessage((String) n[2]);
                notification.setRead(r.nextBoolean());
                notification.setTargetUrl("/student/dashboard");
                notificationRepo.save(notification);
            }
        }
    }

    @Transactional
    public void migratePlaintextPasswords() {
        // Migrate Employee passwords
        for (Employee emp : employeeRepo.findAll()) {
            if (emp.getPassword() != null && !emp.getPassword().startsWith("$2a$")) {
                emp.setPassword(passwordEncoder.encode(emp.getPassword()));
                employeeRepo.save(emp);
            }
        }
        // Migrate User passwords
        for (User user : userRepo.findAll()) {
            if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userRepo.save(user);
            }
        }
    }
}
