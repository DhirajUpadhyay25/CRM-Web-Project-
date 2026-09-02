package in.project.main.controllers;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Course;
import in.project.main.entities.Enrollment;
import in.project.main.entities.Orders;
import in.project.main.entities.Role;
import in.project.main.entities.User;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.NotificationType;
import in.project.main.entities.Lesson;
import in.project.main.entities.Certificate;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.repositories.LessonRepository;
import in.project.main.repositories.CertificateRepository;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.CategoryService;
import in.project.main.services.CourseService;
import in.project.main.services.OrderService;
import in.project.main.services.NotificationService;
import in.project.main.util.DateTimeUtil;

@Controller
public class PublicController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private in.project.main.services.AuditLogService auditLogService;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    private Sort resolvePublicSort(String sort) {
        if ("price_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "originalPrice");
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "originalPrice");
        } else if ("title_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    // ==========================================
    // 1. PUBLIC COURSE CATALOG
    // ==========================================
    @GetMapping("/courses")
    public String openCoursesPage(
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "level", required = false) CourseLevel level,
            @RequestParam(name = "pricingType", required = false) String pricingType,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            org.springframework.security.core.Authentication authentication) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), resolvePublicSort(sort));
        Page<Course> coursesPage = courseService.getPublicStorefrontCourses(keyword, categoryId, level, pricingType, pageable);

        // Fetch lesson counts
        Map<Long, Integer> lessonCounts = new HashMap<>();
        for (Course c : coursesPage.getContent()) {
            lessonCounts.put(c.getId(), lessonRepository.findByCourseId(String.valueOf(c.getId())).size());
        }

        // Fetch enrolled course IDs if student
        List<Long> enrolledCourseIds = new ArrayList<>();
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            String email = authentication.getName();
            enrolledCourseIds = enrollmentRepository.findByUserEmailOrderByEnrolledAtDesc(email)
                    .stream()
                    .map(e -> e.getCourse().getId())
                    .toList();
        }

        model.addAttribute("coursesPage", coursesPage);
        model.addAttribute("lessonCounts", lessonCounts);
        model.addAttribute("enrolledCourseIds", enrolledCourseIds);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("levels", CourseLevel.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("level", level);
        model.addAttribute("pricingType", pricingType);
        model.addAttribute("sort", sort);

        return "public/courses";
    }

    // ==========================================
    // 2. PUBLIC COURSE DETAIL PAGE
    // ==========================================
    @GetMapping("/courses/{slug}")
    public String openCourseDetailPage(
            @PathVariable("slug") String slug,
            @RequestParam(value = "preview", defaultValue = "false") boolean preview,
            org.springframework.security.core.Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Course course = courseService.getCourseBySlug(slug).orElse(null);
        if (course == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Course not found.");
            return "redirect:/courses";
        }

        // Security check: If course is not PUBLISHED, only ADMIN can preview it!
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            if (!isAdmin) {
                redirectAttributes.addFlashAttribute("errorMsg", "This course is not available.");
                return "redirect:/courses";
            }
            model.addAttribute("isPreviewMode", true);
        }

        // Related courses in the same category
        List<Course> relatedCourses = courseService.getRelatedCourses(course.getCategory(), course.getId());
        model.addAttribute("relatedCourses", relatedCourses);

        // Check enrollment / purchase status for student
        boolean isPurchased = false;
        boolean isStudent = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            String email = authentication.getName();
            isPurchased = ordersRepository.existsByUserEmailAndCourseName(email, course.getName());
            User sessionUser = userRepository.findByEmail(email);
            model.addAttribute("sessionUser", sessionUser);
        }

        // Dynamic curriculum loading
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(String.valueOf(course.getId()));
        Map<String, List<Lesson>> curriculum = new java.util.LinkedHashMap<>();
        for (Lesson l : lessons) {
            curriculum.computeIfAbsent(l.getSectionName() != null ? l.getSectionName() : "Getting Started", k -> new ArrayList<>()).add(l);
        }
        model.addAttribute("curriculum", curriculum);
        model.addAttribute("totalLessons", lessons.size());

        model.addAttribute("course", course);
        model.addAttribute("isPurchased", isPurchased);
        model.addAttribute("razorpayKeyId", razorpayKeyId);

        return "public/course-detail";
    }

    // ==========================================
    // 3. FREE COURSE ENROLLMENT
    // ==========================================
    @PostMapping("/courses/free-enroll")
    public String handleFreeEnrollment(
            @RequestParam("courseId") Long courseId,
            org.springframework.security.core.Authentication authentication,
            RedirectAttributes redirectAttributes) {

        boolean isStudent = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (!isStudent) {
            redirectAttributes.addFlashAttribute("errorMsg", "Please log in as a student to enroll.");
            return "redirect:/login";
        }

        String email = authentication.getName();

        Course course = courseService.getCourseDetailsById(courseId);
        if (course == null || course.getStatus() != CourseStatus.PUBLISHED) {
            redirectAttributes.addFlashAttribute("errorMsg", "Course not available for enrollment.");
            return "redirect:/courses";
        }

        if (!course.isFree()) {
            redirectAttributes.addFlashAttribute("errorMsg", "This is a paid course. Please complete checkout.");
            return "redirect:/courses/" + course.getSlug();
        }

        if (ordersRepository.existsByUserEmailAndCourseName(email, course.getName())) {
            redirectAttributes.addFlashAttribute("successMsg", "You are already enrolled in this course!");
            return "redirect:/student/courses";
        }

        Orders freeOrder = new Orders();
        freeOrder.setCourseName(course.getName());
        freeOrder.setCourseAmount("0");
        freeOrder.setUserEmail(email);
        freeOrder.setDateOfPurchase(DateTimeUtil.getCurrentDateTimeFormatted());
        freeOrder.setOrderId("free_" + System.currentTimeMillis());
        freeOrder.setPaymentId("free_enrollment");
        freeOrder.setSignature("free_verified");
        freeOrder.setStatus("COMPLETED");

        orderService.storeUserOrders(freeOrder);

        // Create enrollment record for the student panel
        User user = userRepository.findByEmail(email);
        if (user != null && !enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            Enrollment enrollment = new Enrollment();
            enrollment.setUser(user);
            enrollment.setCourse(course);
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setPaymentStatus("FREE");
            enrollmentRepository.save(enrollment);

            // Trigger Notifications
            notificationService.sendToUser(
                    email,
                    NotificationType.COURSE_ENROLLED,
                    "Course Enrollment Activated",
                    "You have been enrolled in course '" + course.getName() + "' for free. Start learning now!",
                    "/student/courses/" + course.getId() + "/player",
                    "COURSE",
                    String.valueOf(course.getId())
            );

            notificationService.sendToAdmin(
                    NotificationType.COURSE_ENROLLED,
                    "New Free Enrollment",
                    (user.getName() != null ? user.getName() : email) + " enrolled in free course '" + course.getName() + "'.",
                    "/admin/students",
                    "COURSE",
                    String.valueOf(course.getId()),
                    user.getEmail(),
                    user.getName()
            );

            String instructorEmail = (course.getInstructorRef() != null) ? course.getInstructorRef().getEmail() : course.getInstructorEmail();
            if (instructorEmail != null && !instructorEmail.isBlank()) {
                notificationService.sendToInstructor(
                        instructorEmail,
                        NotificationType.COURSE_ENROLLED,
                        "New Student Enrollment",
                        (user.getName() != null ? user.getName() : email) + " enrolled in your course '" + course.getName() + "'.",
                        "/instructor/students",
                        "COURSE",
                        String.valueOf(course.getId())
                );
            }

            // Record Audit Event
            try {
                if (auditLogService != null) {
                    in.project.main.events.PlatformAuditEvent audit = in.project.main.events.PlatformAuditEvent.of(
                        email,
                        in.project.main.entities.enums.AuditEventType.ENROLLMENT_CREATED,
                        "COURSE_FREE_ENROLL",
                        (user.getName() != null ? user.getName() : email) + " enrolled in free course '" + course.getName() + "' (ID: " + course.getId() + ")."
                    )
                    .withActor(String.valueOf(user.getId()), email, user.getName(), "STUDENT")
                    .withEntity("ENROLLMENT", String.valueOf(enrollment.getId()), course.getName())
                    .withStatus(in.project.main.entities.enums.AuditStatus.SUCCESS)
                    .withSeverity(in.project.main.entities.enums.AuditSeverity.INFO);

                    auditLogService.record(audit);
                }
            } catch (Exception ignored) {}
        }

        redirectAttributes.addFlashAttribute("successMsg", "Successfully enrolled in " + course.getName() + "! Start learning below.");
        return "redirect:/student/courses";
    }

    // ==========================================
    // 4. STATIC INFORMATIONAL PAGES
    // ==========================================
    @GetMapping("/services")
    public String openServicesPage() {
        return "public/services";
    }

    @GetMapping("/about")
    public String openAboutPage() {
        return "public/about";
    }

    @GetMapping("/contact")
    public String openContactPage() {
        return "public/contact";
    }

    @GetMapping("/faq")
    public String openFaqPage() {
        return "public/faq";
    }

    /**
     * Target of SecurityConfig's accessDeniedPage. Without this mapping the forward that
     * Spring Security performs on an authorization failure lands on no handler at all.
     */
    @GetMapping("/403")
    public String openAccessDeniedPage() {
        return "403";
    }

    // ==========================================
    // 5. PUBLIC CERTIFICATE VERIFICATION
    // ==========================================
    @GetMapping("/verify/certificate/{code}")
    public String verifyCertificate(@PathVariable("code") String code, Model model) {
        Certificate certificate = certificateRepository.findByCertificateCode(code).orElse(null);
        if (certificate == null) {
            certificate = certificateRepository.findByEnrollmentId(code).orElse(null);
        }

        if (certificate != null) {
            try {
                Enrollment enrollment = enrollmentRepository.findById(Long.parseLong(certificate.getEnrollmentId())).orElse(null);
                if (enrollment != null) {
                    model.addAttribute("verified", true);
                    model.addAttribute("certificate", certificate);
                    model.addAttribute("enrollment", enrollment);
                    model.addAttribute("student", enrollment.getUser());
                    model.addAttribute("course", enrollment.getCourse());
                } else {
                    model.addAttribute("verified", false);
                }
            } catch (Exception e) {
                model.addAttribute("verified", false);
            }
        } else {
            model.addAttribute("verified", false);
        }

        model.addAttribute("certificateCode", code);
        return "public/verify-certificate";
    }
}
