package in.project.main.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import in.project.main.entities.Employee;
import in.project.main.entities.Role;
import in.project.main.entities.SystemRole;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.SystemRoleRepository;

/**
 * Ensures that on a fresh production deployment, an initial administrator account
 * (admin@edutake.com) is created securely if SEED_ADMIN_PASSWORD is provided.
 *
 * Runs after DatabaseSchemaFixRunner (Order 2) so that RBAC tables and schema
 * alterations are already completed.
 */
@Component
@Order(2)
public class ProductionAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionAdminInitializer.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SystemRoleRepository systemRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:}")
    private String seedAdminPassword;

    @Override
    public void run(String... args) {
        try {
            if (employeeRepository.findByEmail("admin@edutake.com") != null) {
                log.debug("Admin account (admin@edutake.com) already exists. Skipping initialization.");
                return;
            }

            if (seedAdminPassword == null || seedAdminPassword.trim().isEmpty()) {
                log.info("No SEED_ADMIN_PASSWORD provided. Skipping initial admin creation.");
                return;
            }

            Employee admin = new Employee();
            admin.setName("EduTake Administrator");
            admin.setEmail("admin@edutake.com");
            admin.setPassword(passwordEncoder.encode(seedAdminPassword.trim()));
            admin.setPhoneno("9999999999");
            admin.setCity("System");
            admin.setRole(Role.ADMIN);

            SystemRole adminSystemRole = systemRoleRepository.findByRoleName("ADMIN").orElse(null);
            if (adminSystemRole != null) {
                admin.setSystemRole(adminSystemRole);
            }

            employeeRepository.save(admin);
            log.info("Successfully created initial administrator account: admin@edutake.com");
        } catch (Exception e) {
            log.warn("Notice on ProductionAdminInitializer: {}", e.getMessage());
        }
    }
}
