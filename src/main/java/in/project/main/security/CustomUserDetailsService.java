package in.project.main.security;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1. Check Employee table (includes ADMIN and EMPLOYEE roles)
        Employee employee = employeeRepository.findByEmail(email);
        if (employee != null) {
            return new CustomUserDetails(
                employee.getEmail(),
                employee.getPassword(),
                employee.getRole(),
                employee.getName(),
                true
            );
        }

        // 2. Check Student (User) table
        User user = userRepository.findByEmail(email);
        if (user != null) {
            return new CustomUserDetails(
                user.getEmail(),
                user.getPassword(),
                Role.STUDENT,
                user.getName(),
                !user.isBanStatus()
            );
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
