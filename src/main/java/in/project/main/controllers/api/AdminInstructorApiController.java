package in.project.main.controllers.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.dto.ApiResponse;
import in.project.main.dto.InstructorCourseAssignDTO;
import in.project.main.dto.InstructorDetailDTO;
import in.project.main.dto.InstructorStatsDTO;
import in.project.main.dto.InstructorStatusUpdateDTO;
import in.project.main.entities.Course;
import in.project.main.entities.Instructor;
import in.project.main.entities.enums.InstructorStatus;
import in.project.main.entities.enums.VerificationStatus;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.InstructorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/instructors")
public class AdminInstructorApiController {

    @Autowired
    private InstructorService instructorService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Instructor>>> listInstructors(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) InstructorStatus status,
            @RequestParam(name = "verificationStatus", required = false) VerificationStatus verificationStatus,
            @RequestParam(name = "specialization", required = false) String specialization,
            @RequestParam(name = "sort", defaultValue = "newest") String sort) {

        Sort sortObj = "oldest".equalsIgnoreCase(sort) 
                ? Sort.by(Sort.Direction.ASC, "createdAt") 
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);

        Page<Instructor> result = instructorService.searchAndFilterInstructors(
                keyword, status, verificationStatus, specialization, pageable);

        return ResponseEntity.ok(ApiResponse.ok("Instructors retrieved successfully", result));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<InstructorStatsDTO>> getStats() {
        InstructorStatsDTO stats = instructorService.getInstructorStatistics();
        return ResponseEntity.ok(ApiResponse.ok("Statistics retrieved successfully", stats));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InstructorDetailDTO>> getInstructor(@PathVariable Long id) {
        try {
            InstructorDetailDTO detail = instructorService.getInstructorDetail(id);
            return ResponseEntity.ok(ApiResponse.ok("Instructor retrieved successfully", detail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Instructor>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody InstructorStatusUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
            Instructor updated = instructorService.updateInstructorStatus(id, dto.getStatus(), dto.getReason(), adminEmail);
            return ResponseEntity.ok(ApiResponse.ok("Instructor status updated successfully", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(
            @RequestParam("email") String email,
            @RequestParam(value = "excludeId", required = false) Long excludeId) {

        boolean available = instructorService.isEmailAvailable(email, excludeId);
        return ResponseEntity.ok(ApiResponse.ok(available ? "Email is available" : "Email is already taken", available));
    }

    @GetMapping("/{id}/courses")
    public ResponseEntity<ApiResponse<List<Course>>> getCourses(@PathVariable Long id) {
        try {
            List<Course> courses = instructorService.getInstructorCourses(id);
            return ResponseEntity.ok(ApiResponse.ok("Courses retrieved successfully", courses));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/courses/assign")
    public ResponseEntity<ApiResponse<String>> assignCourse(
            @PathVariable Long id,
            @Valid @RequestBody InstructorCourseAssignDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
            instructorService.assignCourse(id, dto.getCourseId(), adminEmail);
            return ResponseEntity.ok(ApiResponse.ok("Course assigned successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to assign course: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/courses/{courseId}")
    public ResponseEntity<ApiResponse<String>> unassignCourse(
            @PathVariable Long id,
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String adminEmail = (userDetails != null) ? userDetails.getUsername() : "admin@edutake.com";
            instructorService.unassignCourse(id, courseId, adminEmail);
            return ResponseEntity.ok(ApiResponse.ok("Course unassigned successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to unassign course: " + e.getMessage()));
        }
    }
}
