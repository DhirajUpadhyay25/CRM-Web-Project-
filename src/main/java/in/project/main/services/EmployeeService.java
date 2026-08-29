package in.project.main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Employee;
import in.project.main.repositories.EmployeeRepository;

@Service
public class EmployeeService 
{
	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	public boolean loginEmpService(String email, String password)
	{
		Employee employee = employeeRepository.findByEmail(email);
		if(employee != null)
		{
			return passwordEncoder.matches(password, employee.getPassword());
		}
		return false;
	}
	
	@Transactional
	public void addEmployee(Employee employee)
	{
		employee.setPassword(passwordEncoder.encode(employee.getPassword()));
		employeeRepository.save(employee);
	}
	
	public Employee getEmployeeDetails(String employeeEmail)
	{
		return employeeRepository.findByEmail(employeeEmail);
	}
	
	public Page<Employee> getAllEmployeeDetailsByPagination(Pageable pageable)
	{
		return employeeRepository.findAll(pageable);
	}
	
	@Transactional
	public void updateEmployeeDetails(Employee employee)
	{
		Employee existing = employeeRepository.findByEmail(employee.getEmail());
		if (existing != null && employee.getPassword() != null && !employee.getPassword().isEmpty()) {
			employee.setPassword(passwordEncoder.encode(employee.getPassword()));
		} else if (existing != null && (employee.getPassword() == null || employee.getPassword().isEmpty())) {
			employee.setPassword(existing.getPassword());
		}
		employeeRepository.save(employee);
	}
	
	@Transactional
	public void deleteEmployeeDetails(String employeeEmail)
	{
		Employee employee = employeeRepository.findByEmail(employeeEmail);
		if(employee != null)
		{
			employeeRepository.delete(employee);
		}
		else
		{
			throw new RuntimeException("Employee not found with email : "+employeeEmail);
		}
	}
	
	@Transactional
	public void migratePlaintextPasswords() {
		Iterable<Employee> employees = employeeRepository.findAll();
		for (Employee emp : employees) {
			if (emp.getPassword() != null && !emp.getPassword().startsWith("$2a$")) {
				emp.setPassword(passwordEncoder.encode(emp.getPassword()));
				employeeRepository.save(emp);
			}
		}
	}
}
