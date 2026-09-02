package in.project.main.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import in.project.main.dto.InstructorDTO;
import in.project.main.dto.InstructorDetailDTO;
import in.project.main.dto.InstructorStatsDTO;
import in.project.main.entities.Course;
import in.project.main.entities.Instructor;
import in.project.main.entities.enums.InstructorStatus;
import in.project.main.entities.enums.VerificationStatus;

public interface InstructorService {

    Page<Instructor> searchAndFilterInstructors(
            String keyword,
            InstructorStatus status,
            VerificationStatus verificationStatus,
            String specialization,
            Pageable pageable);

    List<Instructor> getAllInstructors();

    List<Instructor> getActiveInstructors();

    Instructor getInstructorById(Long id);

    Instructor getInstructorByEmail(String email);

    InstructorStatsDTO getInstructorStatistics();

    InstructorDetailDTO getInstructorDetail(Long id);

    Instructor createInstructor(InstructorDTO dto, String adminEmail);

    Instructor updateInstructor(Long id, InstructorDTO dto, String adminEmail);

    Instructor updateInstructorStatus(Long id, InstructorStatus status, String reason, String adminEmail);

    void deleteInstructor(Long id, String adminEmail);

    List<Course> getInstructorCourses(Long instructorId);

    void assignCourse(Long instructorId, Long courseId, String adminEmail);

    void unassignCourse(Long instructorId, Long courseId, String adminEmail);

    List<Course> getAvailableCoursesForAssignment(Long instructorId);

    List<String> getAllSpecializations();

    boolean isEmailAvailable(String email, Long excludeId);
}
