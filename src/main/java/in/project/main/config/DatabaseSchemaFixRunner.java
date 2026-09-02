package in.project.main.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs automatically on application startup to ensure database columns
 * (e.g. employee.role, instructor.status) are sufficiently sized and
 * legacy values are safely normalized.
 */
@Component
@Order(1)
public class DatabaseSchemaFixRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaFixRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Executing database schema column size verification...");

            // 1. Expand employee.role column so 'INSTRUCTOR' (10 chars) is never truncated in MySQL
            try {
                jdbcTemplate.execute("ALTER TABLE employee MODIFY COLUMN role VARCHAR(50) NOT NULL DEFAULT 'EMPLOYEE'");
                log.info("Successfully ensured employee.role is VARCHAR(50)");
            } catch (Exception e) {
                log.debug("Notice on altering employee.role: {}", e.getMessage());
            }

            // 2. Expand instructor status & verification_status columns
            try {
                jdbcTemplate.execute("ALTER TABLE instructor MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'");
                jdbcTemplate.execute("ALTER TABLE instructor MODIFY COLUMN verification_status VARCHAR(50) NOT NULL DEFAULT 'VERIFIED'");
                log.info("Successfully ensured instructor status columns are VARCHAR(50)");
            } catch (Exception e) {
                log.debug("Notice on altering instructor columns: {}", e.getMessage());
            }

            // 3. Normalize legacy mixed-case status rows in instructor table
            try {
                jdbcTemplate.execute("UPDATE instructor SET status = 'ACTIVE' WHERE UPPER(status) = 'ACTIVE' OR status = 'Active'");
                jdbcTemplate.execute("UPDATE instructor SET status = 'INACTIVE' WHERE UPPER(status) = 'INACTIVE' OR status = 'Inactive'");
                jdbcTemplate.execute("UPDATE instructor SET verification_status = 'VERIFIED' WHERE UPPER(verification_status) = 'VERIFIED' OR verification_status = 'Verified' OR verification_status IS NULL");
                log.info("Normalized legacy instructor status records in database.");
            } catch (Exception e) {
                log.debug("Notice on normalizing instructor status: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.warn("Database schema fix runner completed with note: {}", e.getMessage());
        }
    }
}
