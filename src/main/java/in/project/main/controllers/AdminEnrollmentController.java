package in.project.main.controllers;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.dto.BulkEnrollmentDTO;
import in.project.main.dto.BulkEnrollmentResultDTO;
import in.project.main.dto.BulkEnrollmentStatusUpdateDTO;
import in.project.main.dto.EnrollmentAnalyticsDTO;
import in.project.main.dto.EnrollmentDTO;
import in.project.main.dto.EnrollmentDetailDTO;
import in.project.main.dto.EnrollmentStatsDTO;
import in.project.main.dto.EnrollmentStatusUpdateDTO;
import in.project.main.dto.ManualEnrollmentDTO;
import in.project.main.entities.Course;
import in.project.main.entities.User;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.EnrollmentService;
import in.project.main.services.RbacService;

@Controller
@RequestMapping("/admin/enrollments")
public class AdminEnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RbacService rbacService;

    // =========================================================================
    // 1. DASHBOARD & LISTING
    // =========================================================================

    @GetMapping
    public String listEnrollments(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "courseId", required = false) Long courseId,
            @RequestParam(name = "paymentStatus", required = false) String paymentStatus,
            @RequestParam(name = "enrollmentType", required = false) String enrollmentType,
            @RequestParam(name = "enrollmentSource", required = false) String enrollmentSource,
            @RequestParam(name = "sortBy", defaultValue = "enrolledAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        String validSortBy = "enrolledAt";
        if (sortBy != null) {
            String s = sortBy.trim();
            if (s.equals("id") || s.equals("status") || s.equals("completedAt") || s.equals("startDate") || s.equals("expiryDate") || s.equals("lastAccessedAt") || s.equals("paymentStatus") || s.equals("enrollmentType") || s.equals("enrolledAt")) {
                validSortBy = s;
            }
        }
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(validSortBy).ascending() : Sort.by(validSortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        EnrollmentStatus enrollmentStatus = null;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                enrollmentStatus = EnrollmentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<EnrollmentDTO> enrollmentPage = enrollmentService.getEnrollmentsPage(
                search,
                enrollmentStatus,
                courseId,
                paymentStatus,
                enrollmentType,
                enrollmentSource,
                null,
                null,
                pageable);

        EnrollmentStatsDTO stats = enrollmentService.getEnrollmentStats();

        // Dropdowns for filters and modals
        List<Course> courses = courseRepository.findAll();
        List<User> students = userRepository.findAll();

        model.addAttribute("enrollmentPage", enrollmentPage);
        model.addAttribute("stats", stats);
        model.addAttribute("courses", courses);
        model.addAttribute("students", students);
        model.addAttribute("enrollmentStatuses", EnrollmentStatus.values());

        // Retain active filter values
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", status);
        model.addAttribute("courseIdFilter", courseId);
        model.addAttribute("paymentStatusFilter", paymentStatus);
        model.addAttribute("enrollmentTypeFilter", enrollmentType);
        model.addAttribute("enrollmentSourceFilter", enrollmentSource);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("pageSize", size);

        return "admin/learning/enrollments/list";
    }

    // =========================================================================
    // 2. ENROLLMENT DETAILS
    // =========================================================================

    @GetMapping({"/{id}", "/detail"})
    public String viewEnrollmentDetails(
            @PathVariable(required = false) Long id,
            @RequestParam(name = "id", required = false) Long paramId,
            Model model,
            RedirectAttributes ra) {

        Long targetId = id != null ? id : paramId;
        if (targetId == null) {
            ra.addFlashAttribute("errorMsg", "Invalid enrollment identifier.");
            return "redirect:/admin/enrollments";
        }

        try {
            EnrollmentDetailDTO detail = enrollmentService.getEnrollmentDetails(targetId);
            model.addAttribute("enrollment", detail);
            model.addAttribute("enrollmentStatuses", EnrollmentStatus.values());
            return "admin/learning/enrollments/detail";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/admin/enrollments";
        }
    }

    // =========================================================================
    // 3. MANUAL ENROLLMENT
    // =========================================================================

    @PostMapping
    public String handleManualEnrollment(
            @ModelAttribute ManualEnrollmentDTO dto,
            Principal principal,
            RedirectAttributes ra) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";

        try {
            EnrollmentDTO created = enrollmentService.manualEnrollStudent(dto, actorEmail);
            ra.addFlashAttribute("successMsg", "Successfully enrolled student '" + created.getStudentName() + "' in course '" + created.getCourseName() + "' (Enrollment #" + created.getId() + ").");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to enroll student: " + e.getMessage());
        }

        return "redirect:/admin/enrollments";
    }

    // =========================================================================
    // 4. STATUS TRANSITIONS
    // =========================================================================

    @PostMapping("/{id}/status")
    public String handleStatusUpdate(
            @PathVariable Long id,
            @RequestParam("status") String statusStr,
            @RequestParam(name = "reason", required = false) String reason,
            @RequestParam(name = "expiryDate", required = false) String expiryDateStr,
            @RequestParam(name = "notifyStudent", defaultValue = "true") boolean notifyStudent,
            Principal principal,
            RedirectAttributes ra) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";

        try {
            EnrollmentStatus status = EnrollmentStatus.valueOf(statusStr.trim().toUpperCase());
            EnrollmentStatusUpdateDTO dto = new EnrollmentStatusUpdateDTO(status, reason);
            dto.setNotifyStudent(notifyStudent);
            if (expiryDateStr != null && !expiryDateStr.isBlank()) {
                dto.setExpiryDate(LocalDateTime.parse(expiryDateStr.trim()));
            }

            EnrollmentDTO updated = enrollmentService.updateEnrollmentStatus(id, dto, actorEmail);
            ra.addFlashAttribute("successMsg", "Enrollment #" + id + " status changed to " + updated.getStatus().getDisplayName() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update enrollment status: " + e.getMessage());
        }

        return "redirect:/admin/enrollments/" + id;
    }

    @PostMapping("/{id}/activate")
    public String activateEnrollment(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return handleStatusUpdate(id, "ACTIVE", "Activated by Administrator", null, true, principal, ra);
    }

    @PostMapping("/{id}/suspend")
    public String suspendEnrollment(
            @PathVariable Long id,
            @RequestParam(name = "reason", defaultValue = "Administrative review") String reason,
            Principal principal,
            RedirectAttributes ra) {
        return handleStatusUpdate(id, "SUSPENDED", reason, null, true, principal, ra);
    }

    @PostMapping("/{id}/resume")
    public String resumeEnrollment(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return handleStatusUpdate(id, "ACTIVE", "Resumed by Administrator", null, true, principal, ra);
    }

    @PostMapping("/{id}/cancel")
    public String cancelEnrollment(
            @PathVariable Long id,
            @RequestParam(name = "reason", defaultValue = "Cancelled by Administrator") String reason,
            Principal principal,
            RedirectAttributes ra) {
        return handleStatusUpdate(id, "CANCELLED", reason, null, true, principal, ra);
    }

    @PostMapping("/{id}/revoke")
    public String revokeEnrollment(
            @PathVariable Long id,
            @RequestParam(name = "reason", defaultValue = "Access revoked by Administrator") String reason,
            Principal principal,
            RedirectAttributes ra) {
        return handleStatusUpdate(id, "REVOKED", reason, null, true, principal, ra);
    }

    @PostMapping("/{id}/complete")
    public String completeEnrollment(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return handleStatusUpdate(id, "COMPLETED", "Marked as completed by Administrator", null, true, principal, ra);
    }

    // =========================================================================
    // 5. BULK ENROLLMENT & BULK STATUS ACTIONS
    // =========================================================================

    @PostMapping("/bulk")
    public String handleBulkEnrollment(
            @ModelAttribute BulkEnrollmentDTO dto,
            Principal principal,
            RedirectAttributes ra) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";

        try {
            BulkEnrollmentResultDTO result = enrollmentService.bulkEnrollStudents(dto, actorEmail);
            ra.addFlashAttribute("successMsg", "Bulk enrollment completed: " + result.getSuccessCount() + " enrolled, " + result.getSkippedAlreadyEnrolledCount() + " already enrolled, " + result.getFailedCount() + " failed.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Bulk enrollment failed: " + e.getMessage());
        }

        return "redirect:/admin/enrollments";
    }

    @PostMapping("/bulk/status")
    public String handleBulkStatusChange(
            @ModelAttribute BulkEnrollmentStatusUpdateDTO dto,
            Principal principal,
            RedirectAttributes ra) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";

        try {
            BulkEnrollmentResultDTO result = enrollmentService.bulkUpdateStatus(dto, actorEmail);
            ra.addFlashAttribute("successMsg", "Bulk status update completed: " + result.getSuccessCount() + " updated, " + result.getFailedCount() + " failed.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Bulk status update failed: " + e.getMessage());
        }

        return "redirect:/admin/enrollments";
    }

    // =========================================================================
    // 6. CSV EXPORT
    // =========================================================================

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEnrollments(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "courseId", required = false) Long courseId,
            @RequestParam(name = "paymentStatus", required = false) String paymentStatus,
            @RequestParam(name = "enrollmentType", required = false) String enrollmentType) {

        EnrollmentStatus enrollmentStatus = null;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                enrollmentStatus = EnrollmentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        byte[] csvData = enrollmentService.exportEnrollmentsToCsv(search, enrollmentStatus, courseId, paymentStatus, enrollmentType);
        String filename = "enrollments_export_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvData);
    }

    // =========================================================================
    // 7. ANALYTICS VIEW
    // =========================================================================

    @GetMapping("/analytics")
    public String viewAnalytics(Model model) {
        EnrollmentAnalyticsDTO analytics = enrollmentService.getEnrollmentAnalytics();
        model.addAttribute("analytics", analytics);
        return "admin/learning/enrollments/analytics";
    }
}

