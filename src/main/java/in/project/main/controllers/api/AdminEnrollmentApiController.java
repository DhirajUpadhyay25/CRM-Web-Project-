package in.project.main.controllers.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.dto.ApiResponse;
import in.project.main.dto.BulkEnrollmentDTO;
import in.project.main.dto.BulkEnrollmentResultDTO;
import in.project.main.dto.BulkEnrollmentStatusUpdateDTO;
import in.project.main.dto.EnrollmentAnalyticsDTO;
import in.project.main.dto.EnrollmentDTO;
import in.project.main.dto.EnrollmentDetailDTO;
import in.project.main.dto.EnrollmentStatsDTO;
import in.project.main.dto.EnrollmentStatusUpdateDTO;
import in.project.main.dto.ManualEnrollmentDTO;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.services.EnrollmentService;

@RestController
@RequestMapping("/api/admin/enrollments")
public class AdminEnrollmentApiController {

    @Autowired
    private EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EnrollmentDTO>>> getEnrollments(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "courseId", required = false) Long courseId,
            @RequestParam(name = "paymentStatus", required = false) String paymentStatus,
            @RequestParam(name = "enrollmentType", required = false) String enrollmentType,
            @RequestParam(name = "enrollmentSource", required = false) String enrollmentSource,
            @RequestParam(name = "sortBy", defaultValue = "enrolledAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        EnrollmentStatus enrollmentStatus = null;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                enrollmentStatus = EnrollmentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<EnrollmentDTO> result = enrollmentService.getEnrollmentsPage(
                search, enrollmentStatus, courseId, paymentStatus, enrollmentType, enrollmentSource, null, null, pageable);

        return ResponseEntity.ok(ApiResponse.success("Enrollments fetched successfully", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EnrollmentDetailDTO>> getEnrollmentDetail(@PathVariable Long id) {
        try {
            EnrollmentDetailDTO detail = enrollmentService.getEnrollmentDetails(id);
            return ResponseEntity.ok(ApiResponse.success("Enrollment details fetched successfully", detail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EnrollmentDTO>> manualEnroll(
            @RequestBody ManualEnrollmentDTO dto,
            Principal principal) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            EnrollmentDTO created = enrollmentService.manualEnrollStudent(dto, actorEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Student enrolled successfully", created));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to enroll student: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> updateStatus(
            @PathVariable Long id,
            @RequestBody EnrollmentStatusUpdateDTO dto,
            Principal principal) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            EnrollmentDTO updated = enrollmentService.updateEnrollmentStatus(id, dto, actorEmail);
            return ResponseEntity.ok(ApiResponse.success("Enrollment status updated successfully", updated));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkEnrollmentResultDTO>> bulkEnroll(
            @RequestBody BulkEnrollmentDTO dto,
            Principal principal) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            BulkEnrollmentResultDTO result = enrollmentService.bulkEnrollStudents(dto, actorEmail);
            return ResponseEntity.ok(ApiResponse.success("Bulk enrollment processed", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Bulk enrollment failed: " + e.getMessage()));
        }
    }

    @PostMapping("/bulk/status")
    public ResponseEntity<ApiResponse<BulkEnrollmentResultDTO>> bulkUpdateStatus(
            @RequestBody BulkEnrollmentStatusUpdateDTO dto,
            Principal principal) {

        String actorEmail = principal != null ? principal.getName() : "admin@edutake.com";
        try {
            BulkEnrollmentResultDTO result = enrollmentService.bulkUpdateStatus(dto, actorEmail);
            return ResponseEntity.ok(ApiResponse.success("Bulk status update processed", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Bulk status update failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<EnrollmentStatsDTO>> getStats() {
        EnrollmentStatsDTO stats = enrollmentService.getEnrollmentStats();
        return ResponseEntity.ok(ApiResponse.success("Enrollment stats fetched successfully", stats));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<EnrollmentAnalyticsDTO>> getAnalytics() {
        EnrollmentAnalyticsDTO analytics = enrollmentService.getEnrollmentAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Enrollment analytics fetched successfully", analytics));
    }
}
