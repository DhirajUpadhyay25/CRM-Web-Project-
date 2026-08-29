package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.EnrollmentRepository;

@Controller
@RequestMapping("/admin/enrollments")
public class AdminEnrollmentController {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @GetMapping
    public String listEnrollments(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("enrolledAt").descending());
        Page<in.project.main.entities.Enrollment> enrollments;

        EnrollmentStatus enrollmentStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                enrollmentStatus = EnrollmentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        enrollments = enrollmentRepository.searchAndFilter(
                (search != null && !search.trim().isEmpty()) ? search.trim() : null,
                enrollmentStatus,
                pageable);

        model.addAttribute("enrollments", enrollments);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("enrollmentStatuses", java.util.Arrays.asList(EnrollmentStatus.values()));
        model.addAttribute("totalEnrollments", enrollmentRepository.count());
        model.addAttribute("activeEnrollments", enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE));
        model.addAttribute("completedEnrollments", enrollmentRepository.countByStatus(EnrollmentStatus.COMPLETED));

        return "admin/learning/enrollments/list";
    }
}
