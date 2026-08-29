package in.project.main.repositories;

import java.util.List;

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

	long countByBanStatusFalse();

	List<User> findTop10ByOrderByIdDesc();

	@Query("SELECT u FROM User u WHERE " +
	       "(:keyword IS NULL OR :keyword = '' OR " +
	       " LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.phoneno) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       " LOWER(u.city) LIKE LOWER(CONCAT('%', :keyword, '%')))")
	Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
}