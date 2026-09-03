package in.project.main.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

public interface EnrollmentService {

    Page<EnrollmentDTO> getEnrollmentsPage(
            String search,
            EnrollmentStatus status,
            Long courseId,
            String paymentStatus,
            String enrollmentType,
            String enrollmentSource,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    EnrollmentDetailDTO getEnrollmentDetails(Long id);

    EnrollmentStatsDTO getEnrollmentStats();

    EnrollmentAnalyticsDTO getEnrollmentAnalytics();

    EnrollmentDTO manualEnrollStudent(ManualEnrollmentDTO dto, String actorEmail);

    EnrollmentDTO updateEnrollmentStatus(Long id, EnrollmentStatusUpdateDTO dto, String actorEmail);

    BulkEnrollmentResultDTO bulkEnrollStudents(BulkEnrollmentDTO dto, String actorEmail);

    BulkEnrollmentResultDTO bulkUpdateStatus(BulkEnrollmentStatusUpdateDTO dto, String actorEmail);

    byte[] exportEnrollmentsToCsv(
            String search,
            EnrollmentStatus status,
            Long courseId,
            String paymentStatus,
            String enrollmentType);

    boolean canAccessCourse(String email, Long courseId);

    boolean canAccessCourse(Long userId, Long courseId);

    List<EnrollmentDetailDTO.AuditTimelineItemDTO> getEnrollmentHistory(Long enrollmentId);
}
