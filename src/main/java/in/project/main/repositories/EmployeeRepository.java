package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Employee;
import in.project.main.entities.Role;
import in.project.main.entities.SystemRole;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>
{
	Employee findByEmail(String email);
	List<Employee> findByRole(Role role);
	long countBySystemRole(SystemRole systemRole);
	List<Employee> findBySystemRole(SystemRole systemRole);

	@org.springframework.data.jpa.repository.Query("SELECT e FROM Employee e WHERE " +
		"(:q IS NULL OR :q = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.email) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.phoneno) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.city) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
		"(:role IS NULL OR e.role = :role)")
	org.springframework.data.domain.Page<Employee> searchEmployees(
		@org.springframework.data.repository.query.Param("q") String query,
		@org.springframework.data.repository.query.Param("role") Role role,
		org.springframework.data.domain.Pageable pageable
	);
}
