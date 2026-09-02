package in.project.main.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Instructor;
import in.project.main.entities.enums.InstructorStatus;
import in.project.main.entities.enums.VerificationStatus;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long>, JpaSpecificationExecutor<Instructor> {

    Instructor findByEmail(String email);

    Optional<Instructor> findOptionalByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByStatus(InstructorStatus status);

    long countByVerificationStatus(VerificationStatus verificationStatus);

    List<Instructor> findByStatus(InstructorStatus status);

    List<Instructor> findByStatusOrderByNameAsc(InstructorStatus status);

    @Query("SELECT DISTINCT i.specialization FROM Instructor i WHERE i.specialization IS NOT NULL AND i.specialization != '' ORDER BY i.specialization ASC")
    List<String> findDistinctSpecializations();

    @Query("SELECT i FROM Instructor i WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(i.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(i.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(i.headline) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(i.specialization) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(i.skills) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:verificationStatus IS NULL OR i.verificationStatus = :verificationStatus) AND " +
           "(:specialization IS NULL OR :specialization = '' OR LOWER(i.specialization) LIKE LOWER(CONCAT('%', :specialization, '%')))")
    Page<Instructor> searchAndFilterInstructors(
            @Param("keyword") String keyword,
            @Param("status") InstructorStatus status,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("specialization") String specialization,
            Pageable pageable);
}
