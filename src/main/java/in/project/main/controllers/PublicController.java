package in.project.main.controllers;

import java.util.List;

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
import in.project.main.entities.Orders;
import in.project.main.entities.Role;
import in.project.main.entities.User;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.CategoryService;
import in.project.main.services.CourseService;
import in.project.main.services.OrderService;
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
            @RequestParam(name = "sort", defaultValue = "newest") String sort) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), resolvePublicSort(sort));
        Page<Course> coursesPage = courseService.getPublicStorefrontCourses(keyword, categoryId, level, pricingType, pageable);

        model.addAttribute("coursesPage", coursesPage);
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
            return "redirect:/myCourses";
        }

        Orders freeOrder = new Orders();
        freeOrder.setCourseName(course.getName());
        freeOrder.setCourseAmount("0");
        freeOrder.setUserEmail(email);
        freeOrder.setDateOfPurchase(DateTimeUtil.getCurrentDateTimeFormatted());
        freeOrder.setOrderId("free_" + System.currentTimeMillis());
        freeOrder.setPaymentId("free_enrollment");
        freeOrder.setSignature("free_verified");

        orderService.storeUserOrders(freeOrder);

        redirectAttributes.addFlashAttribute("successMsg", "🎉 Successfully enrolled in " + course.getName() + "! Start learning below.");
        return "redirect:/myCourses";
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
}
