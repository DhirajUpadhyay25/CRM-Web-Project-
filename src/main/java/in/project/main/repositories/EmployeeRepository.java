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
}
