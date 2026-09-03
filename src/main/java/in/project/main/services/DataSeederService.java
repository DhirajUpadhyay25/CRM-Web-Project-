package in.project.main.services;

import in.project.main.entities.*;
import in.project.main.entities.enums.*;
import in.project.main.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

@Service
public class DataSeederService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataSeederService.class);

    /** $2 + revision letter + two digit cost + 53 chars of salt and digest. */
    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[abxy]\\$\\d{2}\\$.{53}$");

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
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizQuestionRepository quizQuestionRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Autowired private LessonProgressRepository lessonProgressRepository;
    @Autowired private StudentActivityRepository studentActivityRepository;

    /**
     * The accounts seedStudents() creates. Every seed step that fabricates learner data is
     * restricted to these addresses.
     *
     * The three student-data seed steps used to iterate userRepo.findAll(), so running the
     * seeder invented enrollments, orders and certificates for real registered users.
     */
    private static final List<String> TEST_STUDENT_EMAILS = Arrays.asList(
            "rahul@student.com", "priya@student.com", "amit@student.com",
            "sneha@student.com", "vikram@student.com");

    /** Password for the seeded admin account. Blank means "do not create it". */
    @org.springframework.beans.factory.annotation.Value("${app.seed.admin-password:}")
    private String seedAdminPassword;

    /**
     * Resolves the seeded test accounts that actually exist. Returns only these, never the
     * full user table, so demo data can never attach itself to a real person's account.
     */
    private List<User> seededTestStudents() {
        List<User> students = new java.util.ArrayList<>();
        for (String email : TEST_STUDENT_EMAILS) {
            User user = userRepo.findByEmail(email);
            if (user != null) {
                students.add(user);
            }
        }
        return students;
    }

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
        if (seedAdminPassword == null || seedAdminPassword.isBlank()) {
            // No password configured, so no account is created. This previously hardcoded
            // "admin123", which meant every database the seeder touched gained a known
            // administrator login.
            log.warn("Skipping admin seed: set app.seed.admin-password to create admin@edutake.com");
            return;
        }
        Employee admin = new Employee();
        admin.setName("Administrator");
        admin.setEmail("admin@edutake.com");
        admin.setPassword(passwordEncoder.encode(seedAdminPassword));
        admin.setPhoneno("9999999999");
        admin.setCity("System");
        admin.setRole(Role.ADMIN);
        employeeRepo.save(admin);
    }

    private void seedInstructors() {
        if (instructorRepo.count() == 0) {
            Object[][] instructorData = {
                {"John", "Doe", "john.doe@example.com", "+1 (555) 234-5678", "Principal Cloud & Java Architect", "Backend Engineering", "12+ years building distributed cloud platforms and guiding engineering teams.", "Java, Spring Boot, Microservices, AWS, Docker, Kubernetes", "San Francisco", "USA"},
                {"Jane", "Smith", "jane.smith@example.com", "+1 (555) 345-6789", "Senior Fullstack & AI Specialist", "Full Stack Development", "Specializes in modern React, Next.js, and enterprise Generative AI applications.", "React, TypeScript, Next.js, Node.js, Python, OpenAI", "New York", "USA"},
                {"Alan", "Turing", "alan.turing@example.com", "+44 (20) 7946-0912", "Chief Data Scientist & Algorithmic Lead", "Data Science & AI", "Leading machine learning researcher and algorithmic design educator.", "Python, TensorFlow, PyTorch, Algorithms, Deep Learning", "London", "UK"},
                {"Grace", "Hopper", "grace.hopper@example.com", "+1 (555) 876-5432", "Systems & DevOps Engineering Lead", "Systems & DevOps", "Specializing in scalable infrastructure, CI/CD pipelines, and high-concurrency systems.", "Rust, Go, CI/CD, Kubernetes, Linux, Performance Tuning", "Austin", "USA"}
            };

            for (Object[] row : instructorData) {
                String firstName = (String) row[0];
                String lastName = (String) row[1];
                String email = (String) row[2];
                String phone = (String) row[3];
                String headline = (String) row[4];
                String spec = (String) row[5];
                String bio = (String) row[6];
                String skills = (String) row[7];
                String city = (String) row[8];
                String country = (String) row[9];

                Instructor i = new Instructor();
                i.setFirstName(firstName);
                i.setLastName(lastName);
                i.setName(firstName + " " + lastName);
                i.setEmail(email);
                i.setPhone(phone);
                i.setHeadline(headline);
                i.setSpecialization(spec);
                i.setBio(bio);
                i.setSkills(skills);
                i.setExperience("8+ Years in Industry");
                i.setEducation("M.S. in Computer Science");
                i.setCertifications("AWS Certified Solutions Architect, Oracle Java Champion");
                i.setLanguages("English, Spanish");
                i.setCity(city);
                i.setCountry(country);
                i.setWebsite("https://" + firstName.toLowerCase() + lastName.toLowerCase() + ".dev");
                i.setLinkedinUrl("https://linkedin.com/in/" + firstName.toLowerCase() + lastName.toLowerCase());
                i.setGithubUrl("https://github.com/" + firstName.toLowerCase() + lastName.toLowerCase());
                i.setStatus(InstructorStatus.ACTIVE);
                i.setVerificationStatus(VerificationStatus.VERIFIED);
                instructorRepo.save(i);

                Employee emp = employeeRepo.findByEmail(email);
                if (emp == null) {
                    emp = new Employee();
                    emp.setEmail(email);
                }
                emp.setName(i.getName());
                emp.setPhoneno(phone);
                emp.setCity(city);
                emp.setPassword(passwordEncoder.encode("instructor123"));
                emp.setRole(Role.INSTRUCTOR);
                employeeRepo.save(emp);
            }
        } else {
            for (Instructor i : instructorRepo.findAll()) {
                boolean changed = false;
                if (i.getStatus() == null) {
                    i.setStatus(InstructorStatus.ACTIVE);
                    changed = true;
                }
                if (i.getVerificationStatus() == null) {
                    i.setVerificationStatus(VerificationStatus.VERIFIED);
                    changed = true;
                }
                if (i.getFirstName() == null || i.getFirstName().isBlank()) {
                    if (i.getName() != null && i.getName().contains(" ")) {
                        String[] parts = i.getName().split(" ", 2);
                        i.setFirstName(parts[0]);
                        i.setLastName(parts[1]);
                    } else {
                        i.setFirstName(i.getName() != null ? i.getName() : "Instructor");
                        i.setLastName("");
                    }
                    changed = true;
                }
                if (changed) {
                    instructorRepo.save(i);
                }
            }
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
        List<Course> courses = courseRepo.findAll();
        for (Course course : courses) {
            String courseId = String.valueOf(course.getId());
            
            // Section 1: Getting Started
            String[] section1 = {"Introduction & Course Roadmap", "Setting Up Your Development Environment", "Your First Hello World Program"};
            for (int i = 0; i < section1.length; i++) {
                Lesson l = new Lesson();
                l.setTitle(section1[i]);
                l.setCourseId(courseId);
                l.setSectionName("Getting Started");
                l.setOrderIndex(i + 1);
                lessonRepo.save(l);
            }

            // Section 2: Core Fundamentals
            String[] section2 = {"Data Types & Variables", "Control Flow Statements", "Working with Collections"};
            for (int i = 0; i < section2.length; i++) {
                Lesson l = new Lesson();
                l.setTitle(section2[i]);
                l.setCourseId(courseId);
                l.setSectionName("Core Fundamentals");
                l.setOrderIndex(i + 4);
                lessonRepo.save(l);
            }

            // Section 3: Advanced Concepts
            String[] section3 = {"Error Handling & Exceptions", "Best Practices & Formatting", "Final Review & Summary"};
            for (int i = 0; i < section3.length; i++) {
                Lesson l = new Lesson();
                l.setTitle(section3[i]);
                l.setCourseId(courseId);
                l.setSectionName("Advanced Concepts");
                l.setOrderIndex(i + 7);
                lessonRepo.save(l);
            }
        }
    }

    private void seedQuizzes() {
        if (quizRepository.count() > 0) return;
        List<Course> courses = courseRepo.findAll();
        for (Course course : courses) {
            Quiz quiz = new Quiz();
            quiz.setCourseId(course.getId());
            quiz.setTitle("Final Knowledge Check - " + course.getName());
            quiz.setDescription("Test your understanding of the core concepts covered in this course. Passing score is 70%.");
            quiz.setPassingScore(70);
            quizRepository.save(quiz);

            // Question 1
            QuizQuestion q1 = new QuizQuestion();
            q1.setQuizId(quiz.getId());
            q1.setQuestionText("What is the primary design goal of this technology?");
            q1.setOptionA("Platform independence and safety");
            q1.setOptionB("Maximum raw performance regardless of safety");
            q1.setOptionC("Restricted to web browsers only");
            q1.setOptionD("To replace all existing operating systems");
            q1.setCorrectOption(1);
            quizQuestionRepository.save(q1);

            // Question 2
            QuizQuestion q2 = new QuizQuestion();
            q2.setQuizId(quiz.getId());
            q2.setQuestionText("Which component is responsible for executing the compiled bytecode?");
            q2.setOptionA("The Compiler");
            q2.setOptionB("The Runtime Engine / VM");
            q2.setOptionC("The Linker");
            q2.setOptionD("The Database Driver");
            q2.setCorrectOption(2);
            quizQuestionRepository.save(q2);

            // Question 3
            QuizQuestion q3 = new QuizQuestion();
            q3.setQuizId(quiz.getId());
            q3.setQuestionText("Which statement is true regarding error handling in this language?");
            q3.setOptionA("Errors can be ignored completely without compilation issues");
            q3.setOptionB("Exceptions are handled using try-catch blocks");
            q3.setOptionC("Errors always crash the user operating system");
            q3.setOptionD("There is no mechanism for error handling");
            q3.setCorrectOption(2);
            quizQuestionRepository.save(q3);
        }
    }

    private void seedAssignments() {
        if (assignmentRepository.count() > 0) return;
        List<Course> courses = courseRepo.findAll();
        for (Course course : courses) {
            Assignment ass = new Assignment();
            ass.setCourseId(course.getId());
            ass.setTitle("Capstone Implementation Project - " + course.getName());
            ass.setDescription("Create a practical application incorporating variables, control flow, object structure, and error handling. Package your source code into a ZIP file (under 5MB) and submit it.");
            ass.setDueDate(LocalDateTime.now().plusDays(7));
            ass.setMaxScore(100);
            assignmentRepository.save(ass);
        }
    }

    private void seedStudentProgress() {
        if (lessonProgressRepository.count() > 0) return;
        List<User> students = seededTestStudents();
        for (User student : students) {
            String email = student.getEmail();
            List<Enrollment> enrollments = enrollmentRepo.findByUserEmailOrderByEnrolledAtDesc(email);
            if (enrollments.isEmpty()) continue;

            // Take the first enrollment (e.g. In Progress) and seed some lesson progress
            Enrollment activeEnrollment = enrollments.get(0);
            Course activeCourse = activeEnrollment.getCourse();
            List<Lesson> lessons = lessonRepo.findByCourseIdOrderByOrderIndexAsc(String.valueOf(activeCourse.getId()));
            
            if (lessons.size() >= 5) {
                // Mark 4 lessons completed, 1 in progress (accessed but not completed)
                for (int i = 0; i < 4; i++) {
                    LessonProgress lp = new LessonProgress();
                    lp.setUserEmail(email);
                    lp.setCourseId(activeCourse.getId());
                    lp.setLessonId(lessons.get(i).getId());
                    lp.setCompleted(true);
                    lp.setCompletedAt(LocalDateTime.now().minusDays(2).plusHours(i));
                    lp.setLastAccessedAt(LocalDateTime.now().minusDays(2).plusHours(i));
                    lessonProgressRepository.save(lp);
                }
                
                // Last accessed but not completed (Continue Learning target!)
                LessonProgress lp = new LessonProgress();
                lp.setUserEmail(email);
                lp.setCourseId(activeCourse.getId());
                lp.setLessonId(lessons.get(4).getId());
                lp.setCompleted(false);
                lp.setLastAccessedAt(LocalDateTime.now().minusHours(1));
                lessonProgressRepository.save(lp);

                // Add activities
                StudentActivity act1 = new StudentActivity();
                act1.setUserEmail(email);
                act1.setActivityType("COURSE_START");
                act1.setDescription("Started the course: " + activeCourse.getName());
                act1.setCreatedAt(LocalDateTime.now().minusDays(2));
                studentActivityRepository.save(act1);

                StudentActivity act2 = new StudentActivity();
                act2.setUserEmail(email);
                act2.setActivityType("LESSON_COMPLETE");
                act2.setDescription("Completed lesson: " + lessons.get(0).getTitle());
                act2.setCreatedAt(LocalDateTime.now().minusDays(2).plusHours(1));
                studentActivityRepository.save(act2);
            }

            // If there's a second enrollment, let's mark it as COMPLETED to test certificates!
            if (enrollments.size() >= 2) {
                Enrollment completedEnrollment = enrollments.get(1);
                completedEnrollment.setStatus(EnrollmentStatus.COMPLETED);
                completedEnrollment.setCompletedAt(LocalDateTime.now().minusDays(5));
                enrollmentRepo.save(completedEnrollment);

                Course compCourse = completedEnrollment.getCourse();
                List<Lesson> compLessons = lessonRepo.findByCourseIdOrderByOrderIndexAsc(String.valueOf(compCourse.getId()));
                for (Lesson l : compLessons) {
                    LessonProgress lp = new LessonProgress();
                    lp.setUserEmail(email);
                    lp.setCourseId(compCourse.getId());
                    lp.setLessonId(l.getId());
                    lp.setCompleted(true);
                    lp.setCompletedAt(LocalDateTime.now().minusDays(6));
                    lp.setLastAccessedAt(LocalDateTime.now().minusDays(6));
                    lessonProgressRepository.save(lp);
                }

                // Quiz attempt (passed)
                List<Quiz> quizzes = quizRepository.findByCourseId(compCourse.getId());
                if (!quizzes.isEmpty()) {
                    QuizAttempt qa = new QuizAttempt();
                    qa.setUserEmail(email);
                    qa.setQuizId(quizzes.get(0).getId());
                    qa.setScore(90);
                    qa.setPassed(true);
                    qa.setAttemptedAt(LocalDateTime.now().minusDays(5));
                    quizAttemptRepository.save(qa);
                }

                // Certificate
                Certificate cert = new Certificate();
                cert.setEnrollment(completedEnrollment);
                cert.setStudent(completedEnrollment.getUser());
                cert.setCourse(completedEnrollment.getCourse());
                cert.setStudentName(completedEnrollment.getUser() != null ? completedEnrollment.getUser().getName() : email);
                cert.setStudentEmail(email);
                cert.setCourseName(completedEnrollment.getCourse().getName());
                cert.setCourseCategory(completedEnrollment.getCourse().getCategory() != null ? completedEnrollment.getCourse().getCategory().getName() : "General");
                cert.setInstructorName(completedEnrollment.getCourse().getInstructor() != null ? completedEnrollment.getCourse().getInstructor() : "EduTake Faculty");
                cert.setCertificateNumber("EDU-2026-00010" + completedEnrollment.getId());
                cert.setVerificationCode("VERIFY" + completedEnrollment.getId() + "A");
                cert.setStatus(in.project.main.entities.enums.CertificateStatus.ISSUED);
                cert.setIssueDate(LocalDate.now().minusDays(5));
                cert.setCompletionDate(LocalDateTime.now().minusDays(5));
                cert.setQrCodeData(in.project.main.utils.QRCodeGeneratorUtil.generateQrSvgDataUri("http://localhost:8080/verify/certificate/VERIFY" + completedEnrollment.getId() + "A", 160));
                certRepo.save(cert);
            }
        }
    }

    private void seedCertificates() {
        // Handled dynamically per enrollment
    }

    private void seedPayments() {
        if (paymentRepo.count() > 0) return;
        List<String> methods = Arrays.asList("RAZORPAY", "UPI", "CARD", "NETBANKING", "CASH");
        Random r = new Random();
        for (int i = 1; i <= 5; i++) {
            Payment p = new Payment();
            p.setPaymentId("PAY_" + (1000 + i));
            p.setOrderId("ORD-00" + i);
            p.setUserEmail(TEST_STUDENT_EMAILS.get(i % TEST_STUDENT_EMAILS.size()));
            p.setAmount(String.valueOf(999 + r.nextInt(4000)));
            p.setPaymentMethod(methods.get(i % methods.size()));
            p.setStatus(i == 1 ? "PENDING" : (i == 4 ? "REFUNDED" : "SUCCESS"));
            p.setPaymentDate("2026-08-" + (10 + i));
            paymentRepo.save(p);
        }
    }

    private void seedCoupons() {
        if (couponRepo.count() > 0) return;
        Coupon c1 = new Coupon(); 
        c1.setCode("WELCOME50"); 
        c1.setDiscountType("PERCENTAGE"); 
        c1.setDiscountValue("50"); 
        c1.setMinOrderAmount("500");
        c1.setMaxDiscountCap("1000");
        c1.setExpiryDate("2028-12-31"); 
        c1.setUsageLimit(500);
        c1.setUsedCount(12);
        c1.setDescription("Welcome gift for new students");
        c1.setIsActive(true);

        Coupon c2 = new Coupon(); 
        c2.setCode("FLAT500"); 
        c2.setDiscountType("FLAT"); 
        c2.setDiscountValue("500"); 
        c2.setMinOrderAmount("999");
        c2.setExpiryDate("2028-11-30"); 
        c2.setUsageLimit(100);
        c2.setUsedCount(5);
        c2.setDescription("Flat ₹500 off on select premium courses");
        c2.setIsActive(true);

        couponRepo.saveAll(Arrays.asList(c1, c2));
    }

    private void seedRefunds() {
        if (refundRepo.count() > 0) return;
        Refund r = new Refund();
        r.setOrderId("ORD-004");
        r.setUserEmail("priya@student.com");
        r.setCourseName("Full Stack Java Masterclass");
        r.setAmount("1500");
        r.setReason("Accidental duplicate purchase during registration");
        r.setStatus("PENDING_REVIEW");
        r.setRefundDate("2026-09-01");
        r.setRequestedAt("2026-09-01 10:30:00");
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
        seedLessons();
        seedQuizzes();
        seedAssignments();
        seedStudentProgress();
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
        List<User> allUsers = seededTestStudents();
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
        List<User> students = seededTestStudents();
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
        int employeesMigrated = 0;
        int usersMigrated = 0;

        for (Employee emp : employeeRepo.findAll()) {
            if (!isAlreadyHashed(emp.getPassword())) {
                emp.setPassword(passwordEncoder.encode(emp.getPassword()));
                employeeRepo.save(emp);
                employeesMigrated++;
            }
        }
        for (User user : userRepo.findAll()) {
            if (!isAlreadyHashed(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userRepo.save(user);
                usersMigrated++;
            }
        }

        // Counts only. Never log the values being read or written here.
        log.info("Password migration complete: {} employee and {} user records re-hashed",
                 employeesMigrated, usersMigrated);
    }

    /**
     * True if the stored value already looks like a BCrypt hash and must be left alone.
     *
     * BCrypt hashes are $2 followed by a revision letter, a two digit cost factor and a
     * 53 character salt+digest. The previous check only recognised the "$2a$" revision, so a
     * hash written by any other revision ($2b$ and $2y$ are both common) would have been
     * treated as plaintext and encoded a second time. That is unrecoverable - the original
     * password no longer verifies and the account is locked out for good.
     *
     * A null password is reported as already-hashed so that this migration never invents a
     * credential for an account that has none.
     */
    private boolean isAlreadyHashed(String password) {
        return password == null || BCRYPT_HASH.matcher(password).matches();
    }
}
