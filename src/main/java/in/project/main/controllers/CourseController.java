package in.project.main.controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.dto.CourseDTO;
import in.project.main.dto.CourseStatsDTO;
import in.project.main.entities.Course;
import in.project.main.entities.Employee;
import in.project.main.entities.Role;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.services.CategoryService;
import in.project.main.services.CourseService;
import jakarta.validation.Valid;

@Controller
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private in.project.main.services.InstructorService instructorService;

    private void populateDropdowns(Model model) {
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("levels", CourseLevel.values());
        model.addAttribute("statuses", CourseStatus.values());
        model.addAttribute("instructorsList", instructorService.getActiveInstructors());
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.trim().isEmpty() || "newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("oldest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("title_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "name");
        } else if ("title_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "name");
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "originalPrice");
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "originalPrice");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    // ==========================================
    // 1. ADMIN COURSE LIST & WORKSPACE
    // ==========================================
    @GetMapping({"/admin/courses", "/admin/course"})
    public String openCourseManagementPage(
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) CourseStatus status,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "featured", required = false) Boolean featured,
            @RequestParam(name = "pricingType", required = false) String pricingType,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "view", defaultValue = "table") String view) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), resolveSort(sort));
        Page<Course> coursesPage = courseService.searchAndFilterCourses(keyword, status, categoryId, featured, pricingType, pageable);
        CourseStatsDTO stats = courseService.getCourseStatistics();

        model.addAttribute("coursesPage", coursesPage);
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("featured", featured);
        model.addAttribute("pricingType", pricingType);
        model.addAttribute("sort", sort);
        model.addAttribute("view", view);

        return "admin/courses/list";
    }

    // ==========================================
    // 2. ADD NEW COURSE
    // ==========================================
    @GetMapping({"/admin/courses/new", "/admin/courses/add", "/admin/course/new", "/admin/course/add", "/admin/add-course"})
    public String openAddCoursePage(Model model) {
        model.addAttribute("courseDTO", new CourseDTO());
        populateDropdowns(model);
        return "admin/courses/add";
    }

    @PostMapping({"/admin/courses/new", "/admin/courses/add", "/admin/courses/create", "/admin/course/new", "/admin/course/add"})
    public String addCourseForm(
            @Valid @ModelAttribute("courseDTO") CourseDTO courseDTO,
            BindingResult bindingResult,
            @RequestParam(value = "courseImg", required = false) MultipartFile courseImg,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            populateDropdowns(model);
            return "admin/courses/add";
        }

        try {
            Course created = courseService.createCourse(courseDTO, courseImg);
            redirectAttributes.addFlashAttribute("successMsg", "Course '" + created.getName() + "' created successfully!");
            return "redirect:/admin/courses";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            populateDropdowns(model);
            return "admin/courses/add";
        }
    }

    // ==========================================
    // 3. ADMIN COURSE DETAIL / INSPECTION
    // ==========================================
    @GetMapping({"/admin/courses/{id}", "/admin/course/{id}"})
    public String openCourseDetailPage(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Course course = courseService.getCourseDetailsById(id);
        if (course == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Course not found.");
            return "redirect:/admin/courses";
        }

        model.addAttribute("course", course);
        model.addAttribute("canDelete", courseService.canSafelyDelete(id));
        return "admin/courses/detail";
    }

    // ==========================================
    // 4. EDIT COURSE
    // ==========================================
    @GetMapping({"/admin/courses/{id}/edit", "/admin/courses/edit"})
    public String openEditCoursePage(
            @PathVariable(value = "id", required = false) Long pathId,
            @RequestParam(value = "id", required = false) Long queryId,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        Long id = pathId != null ? pathId : queryId;
        if (id == null) {
            return "redirect:/admin/courses";
        }

        Course course = courseService.getCourseDetailsById(id);
        if (course == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Course not found.");
            return "redirect:/admin/courses";
        }

        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setName(course.getName());
        dto.setShortDescription(course.getShortDescription());
        dto.setDescription(course.getDescription());
        dto.setInstructor(course.getInstructor());
        dto.setInstructorEmail(course.getInstructorEmail());
        dto.setLevel(course.getLevel());
        dto.setLanguage(course.getLanguage());
        dto.setDuration(course.getDuration());
        dto.setOriginalPrice(course.getOriginalPrice());
        dto.setDiscountedPrice(course.getDiscountedPrice());
        dto.setStatus(course.getStatus());
        dto.setFeatured(course.isFeatured());
        dto.setSeoTitle(course.getSeoTitle());
        dto.setSeoDescription(course.getSeoDescription());
        if (course.getCategory() != null) {
            dto.setCategoryId(course.getCategory().getId());
        }
        dto.setImageUrl(course.getImageUrl());

        model.addAttribute("courseDTO", dto);
        model.addAttribute("course", course);
        populateDropdowns(model);
        
        return "admin/courses/edit";
    }

    @PostMapping({"/admin/courses/{id}/edit", "/admin/courses/edit"})
    public String updateCourseDetailsForm(
            @PathVariable(value = "id", required = false) Long pathId,
            @Valid @ModelAttribute("courseDTO") CourseDTO courseDTO,
            BindingResult bindingResult,
            @RequestParam(value = "courseImg", required = false) MultipartFile courseImg,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        Long id = pathId != null ? pathId : courseDTO.getId();
        if (bindingResult.hasErrors()) {
            return redisplayEditForm(id, model, redirectAttributes);
        }

        try {
            courseService.updateCourse(id, courseDTO, courseImg);
            redirectAttributes.addFlashAttribute("successMsg", "Course updated successfully!");
            return "redirect:/admin/courses";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Failed to update course: " + e.getMessage());
            return redisplayEditForm(id, model, redirectAttributes);
        }
    }

    /**
     * Re-renders the edit form after a validation or save failure. The template reads
     * ${course.name}, ${course.slug} and ${course.id}, so the entity has to go back into the
     * model or the "friendly" error page replaces the user's form and loses their input.
     * If the course cannot be resolved there is nothing to re-render, so fall back to the list.
     */
    private String redisplayEditForm(Long id, Model model, RedirectAttributes redirectAttributes) {
        Course course = (id != null) ? courseService.getCourseDetailsById(id) : null;
        if (course == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Course not found.");
            return "redirect:/admin/courses";
        }
        model.addAttribute("course", course);
        populateDropdowns(model);
        return "admin/courses/edit";
    }

    // ==========================================
    // 5. STATUS TRANSITIONS: PUBLISH / ARCHIVE / RESTORE
    // ==========================================
    @PostMapping("/admin/courses/{id}/publish")
    public String publishCourse(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.publishCourse(id);
            redirectAttributes.addFlashAttribute("successMsg", "Course '" + course.getName() + "' is now Published and live on the storefront!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @PostMapping("/admin/courses/{id}/archive")
    public String archiveCourse(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.archiveCourse(id);
            redirectAttributes.addFlashAttribute("successMsg", "Course '" + course.getName() + "' has been Archived.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @PostMapping("/admin/courses/{id}/restore")
    public String restoreCourse(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Course course = courseService.restoreCourse(id);
            redirectAttributes.addFlashAttribute("successMsg", "Course '" + course.getName() + "' restored to Draft status.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    // ==========================================
    // 6. SAFE DELETION
    // ==========================================
    @PostMapping("/admin/courses/{id}/delete")
    public String deleteCoursePost(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            courseService.deleteCourse(id);
            redirectAttributes.addFlashAttribute("successMsg", "Course deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    // ==========================================
    // 7. PREVIEW ACTION FOR ADMIN
    // ==========================================
    @GetMapping("/admin/courses/{id}/preview")
    public String previewCourse(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Course course = courseService.getCourseDetailsById(id);
        if (course == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Course not found.");
            return "redirect:/admin/courses";
        }
        return "redirect:/courses/" + course.getSlug() + "?preview=true";
    }
}
