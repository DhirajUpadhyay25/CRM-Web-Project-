package in.project.main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Employee;
import in.project.main.repositories.EmployeeRepository;

import java.util.Optional;

import in.project.main.entities.Role;
import in.project.main.entities.SystemRole;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.SystemRoleRepository;

@Service
public class EmployeeService 
{
	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private SystemRoleRepository systemRoleRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired(required = false)
	private AuditLogService auditLogService;
	
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
		if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
			employee.setPassword(passwordEncoder.encode(employee.getPassword()));
		}
		employeeRepository.save(employee);
	}

	@Transactional
	public Employee createEmployeeWithRole(Employee employee, Long systemRoleId, String actorEmail)
	{
		if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
			throw new IllegalArgumentException("Email address is required.");
		}
		String cleanEmail = employee.getEmail().trim().toLowerCase();
		if (employeeRepository.findByEmail(cleanEmail) != null) {
			throw new IllegalArgumentException("A user with email '" + cleanEmail + "' already exists.");
		}
		employee.setEmail(cleanEmail);

		if (employee.getPassword() != null && !employee.getPassword().trim().isEmpty()) {
			employee.setPassword(passwordEncoder.encode(employee.getPassword().trim()));
		} else {
			employee.setPassword(passwordEncoder.encode("EduTake@123")); // Default initial password
		}

		if (systemRoleId != null && systemRoleId > 0) {
			SystemRole role = systemRoleRepository.findById(systemRoleId).orElse(null);
			if (role != null) {
				employee.setSystemRole(role);
				try {
					employee.setRole(Role.valueOf(role.getRoleName()));
				} catch (Exception ignored) {
					employee.setRole(Role.EMPLOYEE);
				}
			}
		} else if (employee.getRole() == null) {
			employee.setRole(Role.EMPLOYEE);
		}

		Employee saved = employeeRepository.save(employee);

		if (auditLogService != null) {
			try {
				auditLogService.record(
					PlatformAuditEvent.of(
						actorEmail,
						AuditEventType.USER_CREATED,
						"CREATE_ADMIN_USER",
						"Created administrative user '" + saved.getName() + "' (" + saved.getEmail() + ") with role " + (saved.getSystemRole() != null ? saved.getSystemRole().getDisplayName() : saved.getRole().name())
					)
					.withEntity("Employee", String.valueOf(saved.getId()), saved.getName())
					.withSeverity(AuditSeverity.MEDIUM)
					.withStatus(AuditStatus.SUCCESS)
				);
			} catch (Exception ignored) {}
		}

		return saved;
	}

	@Transactional
	public Employee updateEmployeeProfile(Long id, String name, String phoneno, String city, Long systemRoleId, String actorEmail)
	{
		Employee emp = employeeRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

		String oldRole = emp.getSystemRole() != null ? emp.getSystemRole().getDisplayName() : emp.getRole().name();
		emp.setName(name);
		emp.setPhoneno(phoneno);
		emp.setCity(city);

		if (systemRoleId != null && systemRoleId > 0) {
			SystemRole role = systemRoleRepository.findById(systemRoleId).orElse(null);
			if (role != null) {
				emp.setSystemRole(role);
				try {
					emp.setRole(Role.valueOf(role.getRoleName()));
				} catch (Exception ignored) {
					emp.setRole(Role.EMPLOYEE);
				}
			}
		} else if (systemRoleId != null && systemRoleId == 0) {
			emp.setSystemRole(null);
			emp.setRole(Role.EMPLOYEE);
		}

		Employee saved = employeeRepository.save(emp);

		if (auditLogService != null) {
			try {
				auditLogService.record(
					PlatformAuditEvent.of(
						actorEmail,
						AuditEventType.ROLE_ASSIGNED,
						"UPDATE_ADMIN_USER",
						"Updated user '" + saved.getName() + "' (" + saved.getEmail() + ") profile and assigned role to " + (saved.getSystemRole() != null ? saved.getSystemRole().getDisplayName() : saved.getRole().name())
					)
					.withEntity("Employee", String.valueOf(saved.getId()), saved.getName())
					.withSeverity(AuditSeverity.LOW)
					.withStatus(AuditStatus.SUCCESS)
				);
			} catch (Exception ignored) {}
		}

		return saved;
	}

	@Transactional
	public void resetEmployeePassword(Long id, String newPassword, String actorEmail)
	{
		Employee emp = employeeRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

		if (newPassword == null || newPassword.trim().length() < 6) {
			throw new IllegalArgumentException("Password must be at least 6 characters long.");
		}

		emp.setPassword(passwordEncoder.encode(newPassword.trim()));
		employeeRepository.save(emp);

		if (auditLogService != null) {
			try {
				auditLogService.record(
					PlatformAuditEvent.of(
						actorEmail,
						AuditEventType.PASSWORD_CHANGED,
						"RESET_USER_PASSWORD",
						"Admin reset password for user '" + emp.getName() + "' (" + emp.getEmail() + ")"
					)
					.withEntity("Employee", String.valueOf(emp.getId()), emp.getEmail())
					.withSeverity(AuditSeverity.HIGH)
					.withStatus(AuditStatus.SUCCESS)
				);
			} catch (Exception ignored) {}
		}
	}
	
	public Employee getEmployeeDetails(String employeeEmail)
	{
		return employeeRepository.findByEmail(employeeEmail);
	}

	public Optional<Employee> getEmployeeById(Long id)
	{
		return employeeRepository.findById(id);
	}
	
	public Page<Employee> getAllEmployeeDetailsByPagination(Pageable pageable)
	{
		return employeeRepository.findAll(pageable);
	}

	public Page<Employee> searchEmployees(String query, String roleStr, Pageable pageable)
	{
		Role roleEnum = null;
		if (roleStr != null && !roleStr.isBlank() && !"ALL".equalsIgnoreCase(roleStr)) {
			try {
				roleEnum = Role.valueOf(roleStr.toUpperCase().trim());
			} catch (Exception ignored) {}
		}
		String cleanQ = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
		return employeeRepository.searchEmployees(cleanQ, roleEnum, pageable);
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
			if ("admin@edutake.com".equalsIgnoreCase(employee.getEmail())) {
				throw new IllegalStateException("Primary Super Administrator account cannot be deleted.");
			}
			employeeRepository.delete(employee);
		}
		else
		{
			throw new RuntimeException("Employee not found with email : "+employeeEmail);
		}
	}

	@Transactional
	public void deleteEmployeeById(Long id, String actorEmail)
	{
		Employee emp = employeeRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + id));

		if ("admin@edutake.com".equalsIgnoreCase(emp.getEmail())) {
			throw new IllegalStateException("Primary Super Administrator account cannot be deleted.");
		}

		if (actorEmail != null && actorEmail.equalsIgnoreCase(emp.getEmail())) {
			throw new IllegalStateException("You cannot delete your own administrative account while logged in.");
		}

		employeeRepository.delete(emp);

		if (auditLogService != null) {
			try {
				auditLogService.record(
					PlatformAuditEvent.of(
						actorEmail,
						AuditEventType.USER_DELETED,
						"DELETE_ADMIN_USER",
						"Deleted administrative user '" + emp.getName() + "' (" + emp.getEmail() + ")"
					)
					.withEntity("Employee", String.valueOf(id), emp.getName())
					.withSeverity(AuditSeverity.HIGH)
					.withStatus(AuditStatus.SUCCESS)
				);
			} catch (Exception ignored) {}
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
