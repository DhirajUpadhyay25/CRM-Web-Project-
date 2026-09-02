package in.project.main.services.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.criteria.Predicate;

import in.project.main.dto.InstructorDTO;
import in.project.main.dto.InstructorDetailDTO;
import in.project.main.dto.InstructorStatsDTO;
import in.project.main.entities.AuditLog;
import in.project.main.entities.Course;
import in.project.main.entities.Employee;
import in.project.main.entities.Enrollment;
import in.project.main.entities.Instructor;
import in.project.main.entities.Role;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.entities.enums.InstructorStatus;
import in.project.main.entities.enums.VerificationStatus;
import in.project.main.repositories.AssignmentRepository;
import in.project.main.repositories.AssignmentSubmissionRepository;
import in.project.main.repositories.AuditLogRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.InstructorRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.AuditLogService;
import in.project.main.services.InstructorService;

@Service
@Transactional
public class InstructorServiceImpl implements InstructorService {

    private static final Logger log = LoggerFactory.getLogger(InstructorServiceImpl.class);
    private static final String UPLOAD_DIR = "upload/instructors/";

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private in.project.main.services.NotificationService notificationService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<Instructor> searchAndFilterInstructors(
            String keyword,
            InstructorStatus status,
            VerificationStatus verificationStatus,
            String specialization,
            Pageable pageable) {

        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());
        boolean hasStatus = (status != null);
        boolean hasVerification = (verificationStatus != null);
        boolean hasSpec = (specialization != null && !specialization.trim().isEmpty());

        if (!hasKeyword && !hasStatus && !hasVerification && !hasSpec) {
            return instructorRepository.findAll(pageable);
        }

        Specification<Instructor> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasKeyword) {
                String term = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), term);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), term);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), term);
                Predicate headlineMatch = cb.like(cb.lower(root.get("headline")), term);
                Predicate specMatch = cb.like(cb.lower(root.get("specialization")), term);
                Predicate skillsMatch = cb.like(cb.lower(root.get("skills")), term);
                predicates.add(cb.or(nameMatch, emailMatch, phoneMatch, headlineMatch, specMatch, skillsMatch));
            }

            if (hasStatus) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (hasVerification) {
                predicates.add(cb.equal(root.get("verificationStatus"), verificationStatus));
            }

            if (hasSpec) {
                predicates.add(cb.like(cb.lower(root.get("specialization")), "%" + specialization.trim().toLowerCase() + "%"));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return instructorRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Instructor> getAllInstructors() {
        return instructorRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Instructor> getActiveInstructors() {
        return instructorRepository.findByStatusOrderByNameAsc(InstructorStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public Instructor getInstructorById(Long id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instructor not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Instructor getInstructorByEmail(String email) {
        return instructorRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorStatsDTO getInstructorStatistics() {
        long total = instructorRepository.count();
        long active = instructorRepository.countByStatus(InstructorStatus.ACTIVE);
        long inactive = instructorRepository.countByStatus(InstructorStatus.INACTIVE);
        long pending = instructorRepository.countByStatus(InstructorStatus.PENDING);
        long suspended = instructorRepository.countByStatus(InstructorStatus.SUSPENDED);
        long banned = instructorRepository.countByStatus(InstructorStatus.BANNED);

        List<Course> allCourses = courseRepository.findAll();
        long assignedCourses = allCourses.stream()
                .filter(c -> c.getInstructorRef() != null || (c.getInstructorEmail() != null && !c.getInstructorEmail().isBlank()))
                .count();

        long totalStudentsTaught = enrollmentRepository.count();

        return new InstructorStatsDTO(total, active, inactive, pending, suspended, banned, assignedCourses, totalStudentsTaught);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorDetailDTO getInstructorDetail(Long id) {
        Instructor instructor = getInstructorById(id);
        InstructorDetailDTO detail = new InstructorDetailDTO(instructor);

        // Fetch assigned courses
        List<Course> courses = courseRepository.findByInstructorIdOrEmail(id, instructor.getEmail());
        detail.setCourses(courses);
        detail.setTotalCourses(courses.size());

        long published = courses.stream().filter(c -> c.getStatus() == CourseStatus.PUBLISHED).count();
        long drafts = courses.stream().filter(c -> c.getStatus() == CourseStatus.DRAFT).count();
        detail.setPublishedCourses(published);
        detail.setDraftCourses(drafts);

        // Fetch enrollments across instructor's courses
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        List<Enrollment> enrollments = new ArrayList<>();
        long completedCount = 0;
        if (!courseIds.isEmpty()) {
            enrollments = enrollmentRepository.findAll().stream()
                    .filter(e -> e.getCourse() != null && courseIds.contains(e.getCourse().getId()))
                    .collect(Collectors.toList());
            completedCount = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();
        }
        detail.setEnrollments(enrollments);
        detail.setTotalStudents(enrollments.size());
        detail.setCompletedEnrollments(completedCount);

        // Fetch pending assignment submissions
        long pendingSubmissions = 0;
        if (!courseIds.isEmpty()) {
            List<Long> assignmentIds = assignmentRepository.findByCourseIdIn(courseIds).stream()
                    .map(a -> a.getId())
                    .collect(Collectors.toList());
            if (!assignmentIds.isEmpty()) {
                pendingSubmissions = assignmentSubmissionRepository.findByStatus("SUBMITTED").stream()
                        .filter(s -> assignmentIds.contains(s.getAssignmentId()))
                        .count();
            }
        }
        detail.setPendingSubmissions(pendingSubmissions);

        // Fetch audit logs for this instructor
        try {
            Page<AuditLog> auditPage = auditLogRepository.findByEntityTypeOrderByCreatedAtDesc("INSTRUCTOR", PageRequest.of(0, 20));
            List<AuditLog> instructorLogs = auditPage.getContent().stream()
                    .filter(l -> String.valueOf(id).equals(l.getEntityId()) || (l.getDetails() != null && l.getDetails().contains(instructor.getEmail())))
                    .collect(Collectors.toList());
            detail.setAuditLogs(instructorLogs);
        } catch (Exception e) {
            log.warn("Failed to retrieve audit logs for instructor id {}: {}", id, e.getMessage());
        }

        return detail;
    }

    @Override
    public Instructor createInstructor(InstructorDTO dto, String adminEmail) {
        String email = dto.getEmail().trim().toLowerCase();

        // 1. Check duplicate email in Instructor table
        if (instructorRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An instructor with email '" + email + "' already exists.");
        }

        // 2. Check duplicate email in Student (User) table
        if (userRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered as a student account.");
        }

        // 3. Create Instructor entity
        Instructor instructor = new Instructor();
        instructor.setFirstName(dto.getFirstName().trim());
        instructor.setLastName(dto.getLastName().trim());
        instructor.setName((dto.getFirstName().trim() + " " + dto.getLastName().trim()).trim());
        instructor.setEmail(email);
        instructor.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        instructor.setHeadline(dto.getHeadline() != null ? dto.getHeadline().trim() : null);
        instructor.setSpecialization(dto.getSpecialization() != null ? dto.getSpecialization().trim() : null);
        instructor.setBio(dto.getBio() != null ? dto.getBio().trim() : null);
        instructor.setSkills(dto.getSkills() != null ? dto.getSkills().trim() : null);
        instructor.setExperience(dto.getExperience() != null ? dto.getExperience().trim() : null);
        instructor.setEducation(dto.getEducation() != null ? dto.getEducation().trim() : null);
        instructor.setCertifications(dto.getCertifications() != null ? dto.getCertifications().trim() : null);
        instructor.setLanguages(dto.getLanguages() != null ? dto.getLanguages().trim() : null);
        instructor.setCity(dto.getCity() != null ? dto.getCity().trim() : null);
        instructor.setCountry(dto.getCountry() != null ? dto.getCountry().trim() : null);
        instructor.setWebsite(dto.getWebsite() != null ? dto.getWebsite().trim() : null);
        instructor.setLinkedinUrl(dto.getLinkedinUrl() != null ? dto.getLinkedinUrl().trim() : null);
        instructor.setGithubUrl(dto.getGithubUrl() != null ? dto.getGithubUrl().trim() : null);
        instructor.setStatus(dto.getStatus() != null ? dto.getStatus() : InstructorStatus.ACTIVE);
        instructor.setVerificationStatus(dto.getVerificationStatus() != null ? dto.getVerificationStatus() : VerificationStatus.VERIFIED);

        // Handle Profile Image
        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            String uploadedUrl = saveProfileImage(dto.getProfileImage());
            instructor.setImageUrl(uploadedUrl);
        }

        Instructor saved = instructorRepository.save(instructor);

        // 4. Synchronize credentials with Employee table for login
        Employee employee = employeeRepository.findByEmail(email);
        if (employee == null) {
            employee = new Employee();
            employee.setEmail(email);
        }
        employee.setName(saved.getName());
        employee.setPhoneno(saved.getPhone());
        employee.setCity(saved.getCity());
        employee.setRole(Role.INSTRUCTOR);

        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) 
                ? dto.getPassword().trim() : "instructor123";
        employee.setPassword(passwordEncoder.encode(rawPassword));
        employeeRepository.save(employee);

        // 5. Audit Log
        auditLogService.log(
                adminEmail != null ? adminEmail : "system",
                "INSTRUCTOR_CREATED",
                "INSTRUCTOR",
                String.valueOf(saved.getId()),
                "Created instructor '" + saved.getName() + "' (" + saved.getEmail() + ") with status " + saved.getStatus(),
                "SUCCESS"
        );

        // 6. Notifications
        try {
            notificationService.sendToAdmin(
                in.project.main.entities.enums.NotificationType.NEW_INSTRUCTOR_CREATED,
                "New Instructor Created",
                "Instructor '" + saved.getName() + "' (" + saved.getEmail() + ") was added by " + (adminEmail != null ? adminEmail : "Admin") + ".",
                "/admin/instructors/" + saved.getId(),
                "INSTRUCTOR",
                String.valueOf(saved.getId()),
                adminEmail,
                "Admin"
            );

            notificationService.sendToInstructor(
                saved.getEmail(),
                in.project.main.entities.enums.NotificationType.INSTRUCTOR_WELCOME,
                "Welcome to EduTake Teaching Portal",
                "Welcome " + saved.getName() + "! Your instructor account has been created. You can now manage your courses and students.",
                "/instructor/dashboard",
                "INSTRUCTOR",
                String.valueOf(saved.getId())
            );
        } catch (Exception ignored) {}

        return saved;
    }

    @Override
    public Instructor updateInstructor(Long id, InstructorDTO dto, String adminEmail) {
        Instructor instructor = getInstructorById(id);
        String oldEmail = instructor.getEmail();
        String oldName = instructor.getName();
        String newEmail = dto.getEmail().trim().toLowerCase();

        // Check duplicate email
        if (!oldEmail.equalsIgnoreCase(newEmail) && instructorRepository.existsByEmailAndIdNot(newEmail, id)) {
            throw new IllegalArgumentException("An instructor with email '" + newEmail + "' already exists.");
        }

        instructor.setFirstName(dto.getFirstName().trim());
        instructor.setLastName(dto.getLastName().trim());
        String newName = (dto.getFirstName().trim() + " " + dto.getLastName().trim()).trim();
        instructor.setName(newName);
        instructor.setEmail(newEmail);
        instructor.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        instructor.setHeadline(dto.getHeadline() != null ? dto.getHeadline().trim() : null);
        instructor.setSpecialization(dto.getSpecialization() != null ? dto.getSpecialization().trim() : null);
        instructor.setBio(dto.getBio() != null ? dto.getBio().trim() : null);
        instructor.setSkills(dto.getSkills() != null ? dto.getSkills().trim() : null);
        instructor.setExperience(dto.getExperience() != null ? dto.getExperience().trim() : null);
        instructor.setEducation(dto.getEducation() != null ? dto.getEducation().trim() : null);
        instructor.setCertifications(dto.getCertifications() != null ? dto.getCertifications().trim() : null);
        instructor.setLanguages(dto.getLanguages() != null ? dto.getLanguages().trim() : null);
        instructor.setCity(dto.getCity() != null ? dto.getCity().trim() : null);
        instructor.setCountry(dto.getCountry() != null ? dto.getCountry().trim() : null);
        instructor.setWebsite(dto.getWebsite() != null ? dto.getWebsite().trim() : null);
        instructor.setLinkedinUrl(dto.getLinkedinUrl() != null ? dto.getLinkedinUrl().trim() : null);
        instructor.setGithubUrl(dto.getGithubUrl() != null ? dto.getGithubUrl().trim() : null);
        if (dto.getStatus() != null) {
            instructor.setStatus(dto.getStatus());
        }
        if (dto.getVerificationStatus() != null) {
            instructor.setVerificationStatus(dto.getVerificationStatus());
        }

        // Handle Profile Image update
        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            String uploadedUrl = saveProfileImage(dto.getProfileImage());
            instructor.setImageUrl(uploadedUrl);
        }

        Instructor saved = instructorRepository.save(instructor);

        // Sync Employee table
        Employee employee = employeeRepository.findByEmail(oldEmail);
        if (employee == null && !oldEmail.equalsIgnoreCase(newEmail)) {
            employee = employeeRepository.findByEmail(newEmail);
        }
        if (employee == null) {
            employee = new Employee();
        }
        employee.setEmail(newEmail);
        employee.setName(newName);
        employee.setPhoneno(saved.getPhone());
        employee.setCity(saved.getCity());
        employee.setRole(Role.INSTRUCTOR);
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
        } else if (employee.getPassword() == null || employee.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode("instructor123"));
        }
        employeeRepository.save(employee);

        // Synchronize courses if email or name changed
        if (!oldEmail.equalsIgnoreCase(newEmail) || !oldName.equalsIgnoreCase(newName)) {
            List<Course> assignedCourses = courseRepository.findByInstructorIdOrEmail(id, oldEmail);
            for (Course c : assignedCourses) {
                c.setInstructorRef(saved);
                c.setInstructor(newName);
                c.setInstructorEmail(newEmail);
                courseRepository.save(c);
            }
        }

        // Audit Log
        auditLogService.log(
                adminEmail != null ? adminEmail : "system",
                "INSTRUCTOR_UPDATED",
                "INSTRUCTOR",
                String.valueOf(saved.getId()),
                "Updated instructor details for '" + saved.getName() + "' (" + saved.getEmail() + ")",
                "SUCCESS"
        );

        return saved;
    }

    @Override
    public Instructor updateInstructorStatus(Long id, InstructorStatus newStatus, String reason, String adminEmail) {
        Instructor instructor = getInstructorById(id);
        InstructorStatus oldStatus = instructor.getStatus();

        if (oldStatus == newStatus) {
            return instructor;
        }

        instructor.setStatus(newStatus);
        Instructor saved = instructorRepository.save(instructor);

        String details = "Status changed from " + oldStatus + " to " + newStatus;
        if (reason != null && !reason.trim().isEmpty()) {
            details += " (Reason: " + reason.trim() + ")";
        }

        auditLogService.log(
                adminEmail != null ? adminEmail : "system",
                "INSTRUCTOR_STATUS_CHANGED",
                "INSTRUCTOR",
                String.valueOf(saved.getId()),
                details,
                "SUCCESS"
        );

        try {
            notificationService.sendToInstructor(
                saved.getEmail(),
                in.project.main.entities.enums.NotificationType.INSTRUCTOR_STATUS_CHANGED,
                "Instructor Account Status Updated",
                "Your instructor account status has been updated to: " + newStatus + (reason != null && !reason.isBlank() ? " (" + reason + ")" : ""),
                "/instructor/profile",
                "INSTRUCTOR",
                String.valueOf(saved.getId())
            );
        } catch (Exception ignored) {}

        return saved;
    }

    @Override
    public void deleteInstructor(Long id, String adminEmail) {
        Instructor instructor = getInstructorById(id);

        // Check if instructor is assigned to any courses
        long courseCount = courseRepository.countByInstructorIdOrEmail(id, instructor.getEmail());
        if (courseCount > 0) {
            throw new IllegalStateException("Cannot delete instructor '" + instructor.getName() + 
                    "' because they are currently assigned to " + courseCount + 
                    " course(s). Please reassign or unassign their courses first, or set status to INACTIVE/BANNED.");
        }

        String email = instructor.getEmail();
        String name = instructor.getName();

        instructorRepository.delete(instructor);

        // Remove from Employee login credentials table
        Employee employee = employeeRepository.findByEmail(email);
        if (employee != null && employee.getRole() == Role.INSTRUCTOR) {
            employeeRepository.delete(employee);
        }

        auditLogService.log(
                adminEmail != null ? adminEmail : "system",
                "INSTRUCTOR_DELETED",
                "INSTRUCTOR",
                String.valueOf(id),
                "Deleted instructor '" + name + "' (" + email + ")",
                "SUCCESS"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> getInstructorCourses(Long instructorId) {
        Instructor instructor = getInstructorById(instructorId);
        return courseRepository.findByInstructorIdOrEmail(instructorId, instructor.getEmail());
    }

    @Override
    public void assignCourse(Long instructorId, Long courseId, String adminEmail) {
        Instructor instructor = getInstructorById(instructorId);

        if (instructor.getStatus() == InstructorStatus.BANNED || instructor.getStatus() == InstructorStatus.SUSPENDED) {
            throw new IllegalStateException("Cannot assign courses to an instructor with status: " + instructor.getStatus());
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));

        course.setInstructorRef(instructor);
        course.setInstructor(instructor.getName());
        course.setInstructorEmail(instructor.getEmail());
        courseRepository.save(course);

        auditLogService.log(
                adminEmail != null ? adminEmail : "system",
                "INSTRUCTOR_COURSE_ASSIGNED",
                "INSTRUCTOR",
                String.valueOf(instructorId),
                "Assigned course '" + course.getName() + "' (ID: " + courseId + ") to instructor '" + instructor.getName() + "'",
                "SUCCESS"
        );

        try {
            notificationService.sendToInstructor(
                instructor.getEmail(),
                in.project.main.entities.enums.NotificationType.INSTRUCTOR_COURSE_ASSIGNED,
                "New Course Assigned",
                "You have been assigned to lead course '" + course.getName() + "'.",
                "/instructor/courses",
                "COURSE",
                String.valueOf(courseId)
            );
        } catch (Exception ignored) {}
    }

    @Override
    public void unassignCourse(Long instructorId, Long courseId, String adminEmail) {
        Instructor instructor = getInstructorById(instructorId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));

        course.setInstructorRef(null);
        course.setInstructor(null);
        course.setInstructorEmail(null);
        courseRepository.save(course);

        auditLogService.log(
                adminEmail != null ? adminEmail : "system",
                "INSTRUCTOR_COURSE_UNASSIGNED",
                "INSTRUCTOR",
                String.valueOf(instructorId),
                "Unassigned course '" + course.getName() + "' (ID: " + courseId + ") from instructor '" + instructor.getName() + "'",
                "SUCCESS"
        );

        try {
            notificationService.sendToInstructor(
                instructor.getEmail(),
                in.project.main.entities.enums.NotificationType.INSTRUCTOR_COURSE_UNASSIGNED,
                "Course Unassigned",
                "Course '" + course.getName() + "' has been unassigned from your profile.",
                "/instructor/courses",
                "COURSE",
                String.valueOf(courseId)
            );
        } catch (Exception ignored) {}
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> getAvailableCoursesForAssignment(Long instructorId) {
        List<Course> allCourses = courseRepository.findAll();
        return allCourses.stream()
                .filter(c -> c.getInstructorRef() == null || !c.getInstructorRef().getId().equals(instructorId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllSpecializations() {
        return instructorRepository.findDistinctSpecializations();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email, Long excludeId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String cleanEmail = email.trim().toLowerCase();
        if (excludeId != null) {
            return !instructorRepository.existsByEmailAndIdNot(cleanEmail, excludeId);
        }
        return !instructorRepository.existsByEmail(cleanEmail);
    }

    private String saveProfileImage(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                extension = ".jpg";
            }

            String newFilename = "instructor_" + UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/" + UPLOAD_DIR + newFilename;
        } catch (IOException e) {
            log.error("Failed to store instructor profile photo: {}", e.getMessage(), e);
            throw new RuntimeException("Could not upload profile image: " + e.getMessage());
        }
    }
}
