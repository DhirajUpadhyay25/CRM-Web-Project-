package in.project.main.controllers;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Batch;
import in.project.main.entities.Course;
import in.project.main.entities.Enrollment;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.BatchRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.security.CustomUserDetails;
import in.project.main.services.AuditLogService;

@Controller
@RequestMapping("/instructor/batches")
public class InstructorBatchController {

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    // Helper: Verify that the course belongs to the instructor
    private Course checkCourseOwnership(Long courseId, String email) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (!email.equalsIgnoreCase(course.getInstructorEmail())) {
            throw new SecurityException("Access Denied: You do not own this course");
        }
        return course;
    }

    // 1. LIST INSTRUCTOR BATCHES
    @GetMapping
    public String listBatches(
            @RequestParam(name = "status", required = false) String statusFilter,
            @RequestParam(name = "courseId", required = false) Long courseIdFilter,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        String instructorEmail = userDetails.getUsername();
        List<Course> instructorCourses = courseRepository.findByInstructorEmail(instructorEmail);
        List<String> courseIdStrings = instructorCourses.stream()
                .map(c -> String.valueOf(c.getId()))
                .collect(Collectors.toList());

        List<Batch> batches = new ArrayList<>();
        if (!courseIdStrings.isEmpty()) {
            if (courseIdFilter != null) {
                batches = batchRepository.findByCourseId(String.valueOf(courseIdFilter));
            } else {
                batches = batchRepository.findByCourseIdIn(courseIdStrings);
            }
        }

        // Apply status filter if provided
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter.trim())) {
            batches = batches.stream()
                    .filter(b -> statusFilter.equalsIgnoreCase(b.getStatus()))
                    .collect(Collectors.toList());
        }

        // Build enriched batch items
        Map<Long, Course> courseMap = instructorCourses.stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));

        List<Map<String, Object>> displayBatches = new ArrayList<>();
        long upcomingCount = 0;
        long ongoingCount = 0;
        long completedCount = 0;

        for (Batch b : batches) {
            Map<String, Object> item = new HashMap<>();
            item.put("batch", b);

            Long cId = null;
            try {
                cId = Long.parseLong(b.getCourseId());
            } catch (Exception ignored) {}

            Course c = (cId != null) ? courseMap.get(cId) : null;
            if (c == null && cId != null) {
                c = courseRepository.findById(cId).orElse(null);
            }
            item.put("course", c);
            item.put("courseName", c != null ? c.getName() : "Course #" + b.getCourseId());

            // Count enrolled students
            long studentCount = 0;
            if (cId != null) {
                studentCount = enrollmentRepository.countByCourseIdAndStatus(cId, EnrollmentStatus.ACTIVE);
            }
            item.put("studentCount", studentCount);

            // Tally status counts
            String st = b.getStatus() != null ? b.getStatus().toUpperCase() : "UPCOMING";
            if ("ONGOING".equals(st) || "ACTIVE".equals(st)) {
                ongoingCount++;
            } else if ("COMPLETED".equals(st)) {
                completedCount++;
            } else {
                upcomingCount++;
            }

            displayBatches.add(item);
        }

        model.addAttribute("batches", displayBatches);
        model.addAttribute("courses", instructorCourses);
        model.addAttribute("totalBatches", displayBatches.size());
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("ongoingCount", ongoingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("courseIdFilter", courseIdFilter);

        return "instructor/batches/list";
    }

    // 2. CREATE NEW BATCH
    @PostMapping("/add")
    public String addBatch(
            @RequestParam String name,
            @RequestParam Long courseId,
            @RequestParam String startDate,
            @RequestParam(defaultValue = "UPCOMING") String status,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        try {
            Course course = checkCourseOwnership(courseId, userDetails.getUsername());

            Batch batch = new Batch();
            batch.setName(name.trim());
            batch.setCourseId(String.valueOf(course.getId()));
            batch.setStartDate(startDate);
            batch.setStatus(status.toUpperCase());
            Batch saved = batchRepository.save(batch);

            if (auditLogService != null) {
                in.project.main.events.PlatformAuditEvent audit = in.project.main.events.PlatformAuditEvent.of(
                        userDetails.getUsername(),
                        in.project.main.entities.enums.AuditEventType.SETTINGS_CHANGED,
                        "BATCH_CREATED",
                        "Trainer created batch '" + saved.getName() + "' for course '" + course.getName() + "'."
                )
                .withActor(null, userDetails.getUsername(), userDetails.getName(), "INSTRUCTOR")
                .withEntity("BATCH", String.valueOf(saved.getId()), saved.getName())
                .withStatus(in.project.main.entities.enums.AuditStatus.SUCCESS)
                .withSeverity(in.project.main.entities.enums.AuditSeverity.INFO);

                auditLogService.record(audit);
            }

            ra.addFlashAttribute("successMsg", "Batch '" + saved.getName() + "' created successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to create batch: " + e.getMessage());
        }

        return "redirect:/instructor/batches";
    }

    // 3. UPDATE BATCH
    @PostMapping("/{id}/update")
    public String updateBatch(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam Long courseId,
            @RequestParam String startDate,
            @RequestParam String status,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        try {
            Course course = checkCourseOwnership(courseId, userDetails.getUsername());
            Batch batch = batchRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

            batch.setName(name.trim());
            batch.setCourseId(String.valueOf(course.getId()));
            batch.setStartDate(startDate);
            batch.setStatus(status.toUpperCase());
            batchRepository.save(batch);

            ra.addFlashAttribute("successMsg", "Batch '" + batch.getName() + "' updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to update batch: " + e.getMessage());
        }

        return "redirect:/instructor/batches";
    }

    // 4. DELETE BATCH
    @PostMapping("/{id}/delete")
    public String deleteBatch(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes ra) {

        try {
            Batch batch = batchRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

            try {
                Long cId = Long.parseLong(batch.getCourseId());
                checkCourseOwnership(cId, userDetails.getUsername());
            } catch (NumberFormatException ignored) {}

            batchRepository.delete(batch);
            ra.addFlashAttribute("successMsg", "Batch deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to delete batch: " + e.getMessage());
        }

        return "redirect:/instructor/batches";
    }
}
