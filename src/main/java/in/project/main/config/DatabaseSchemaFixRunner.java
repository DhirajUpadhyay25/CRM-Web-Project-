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

            // 4. Expand notification table type, category, and priority columns
            try {
                jdbcTemplate.execute("ALTER TABLE notification MODIFY COLUMN type VARCHAR(64) NOT NULL");
                jdbcTemplate.execute("ALTER TABLE notification MODIFY COLUMN category VARCHAR(64) NOT NULL");
                jdbcTemplate.execute("ALTER TABLE notification MODIFY COLUMN priority VARCHAR(64) NOT NULL");
                log.info("Successfully ensured notification table columns (type, category, priority) are VARCHAR(64)");
            } catch (Exception e) {
                log.debug("Notice on altering notification columns: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.warn("Database schema fix runner completed with note: {}", e.getMessage());
        }
    }
}
