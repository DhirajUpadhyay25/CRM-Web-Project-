package in.project.main.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.project.main.entities.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>
{
	Employee findByEmail(String email);
}
