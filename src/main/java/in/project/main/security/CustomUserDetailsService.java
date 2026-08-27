package in.project.main.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import in.project.main.entities.Employee;
import in.project.main.entities.Role;
import in.project.main.entities.User;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        // 1. Check if it's the Admin
        if (email.equals(adminEmail)) {
            return new CustomUserDetails(
                adminEmail,
                adminPassword,
                Role.ADMIN,
                "Administrator",
                true
            );
        }

        // 2. Check if it's an Employee
        Employee employee = employeeRepository.findByEmail(email);
        if (employee != null) {
            return new CustomUserDetails(
                employee.getEmail(),
                employee.getPassword(),
                Role.EMPLOYEE,
                employee.getName(),
                true
            );
        }

        // 3. Check if it's a Student (User)
        User user = userRepository.findByEmail(email);
        if (user != null) {
            return new CustomUserDetails(
                user.getEmail(),
                user.getPassword(),
                Role.STUDENT,
                user.getName(),
                !user.isBanStatus() // If banStatus is true, enabled should be false
            );
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
