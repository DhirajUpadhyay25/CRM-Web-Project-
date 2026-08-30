package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Employee;

import java.util.List;
import in.project.main.entities.Role;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>
{
	Employee findByEmail(String email);
	List<Employee> findByRole(Role role);
}
