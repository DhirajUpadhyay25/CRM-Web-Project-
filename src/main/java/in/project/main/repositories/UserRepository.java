package in.project.main.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import in.project.main.entities.User;


@Repository
public interface UserRepository extends JpaRepository<User, Long>
{
	User findByEmail(String email);

	Optional<User> findById(Long id);

	boolean existsByEmail(String email);

	long countByBanStatusFalse();

	long countByBanStatusTrue();

	List<User> findTop10ByOrderByIdDesc();

	@Query("SELECT u FROM User u WHERE " +
	       "(:keyword IS NULL OR :keyword = '' OR " +
	       " LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.phoneno) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.city) LIKE LOWER(CONCAT('%', :keyword, '%')))")
	Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

	@Query("SELECT u FROM User u WHERE " +
	       "(:keyword IS NULL OR :keyword = '' OR " +
	       " LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.phoneno) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.city) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
	       "(:banStatus IS NULL OR u.banStatus = :banStatus)")
	Page<User> searchAndFilterUsers(@Param("keyword") String keyword, @Param("banStatus") Boolean banStatus, Pageable pageable);

	@Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
	long countNewStudentsSince(@Param("startDate") LocalDateTime startDate);

	@Query("SELECT u FROM User u ORDER BY u.id DESC")
	Page<User> findAllOrderByCreatedAtDesc(Pageable pageable);

	@Query("SELECT u FROM User u WHERE " +
	       "(:keyword IS NULL OR :keyword = '' OR " +
	       " LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.phoneno) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.city) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
	       "(:banStatus IS NULL OR u.banStatus = :banStatus) " +
	       "ORDER BY u.id DESC")
	Page<User> searchFilterAndSortUsers(@Param("keyword") String keyword, @Param("banStatus") Boolean banStatus, Pageable pageable);
}