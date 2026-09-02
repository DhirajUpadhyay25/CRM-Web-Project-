package in.project.main.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import in.project.main.services.AppSettingService;
import in.project.main.services.RbacService;

/**
 * Runs automatically on application startup to ensure database columns
 * (e.g. employee.role, instructor.status, notification/audit columns, app_setting, RBAC tables)
 * are sufficiently sized and legacy values are safely normalized.
 */
@Component
@Order(1)
public class DatabaseSchemaFixRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaFixRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private RbacService rbacService;

    @Autowired(required = false)
    private AppSettingService appSettingService;

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

            // 3. Expand notification table type, category, and priority columns
            try {
                jdbcTemplate.execute("ALTER TABLE notification MODIFY COLUMN type VARCHAR(64) NOT NULL");
                jdbcTemplate.execute("ALTER TABLE notification MODIFY COLUMN category VARCHAR(64) NOT NULL");
                jdbcTemplate.execute("ALTER TABLE notification MODIFY COLUMN priority VARCHAR(64) NOT NULL");
                log.info("Successfully ensured notification table columns are VARCHAR(64)");
            } catch (Exception e) {
                log.debug("Notice on altering notification columns: {}", e.getMessage());
            }

            // 4. Expand audit_log columns & create system_error_log table
            try {
                jdbcTemplate.execute("ALTER TABLE audit_log MODIFY COLUMN event_type VARCHAR(64) NULL");
                jdbcTemplate.execute("ALTER TABLE audit_log MODIFY COLUMN category VARCHAR(64) NULL");
                jdbcTemplate.execute("ALTER TABLE audit_log MODIFY COLUMN severity VARCHAR(32) NULL");
                jdbcTemplate.execute("ALTER TABLE audit_log MODIFY COLUMN status VARCHAR(32) NULL");
                jdbcTemplate.execute("ALTER TABLE audit_log MODIFY COLUMN admin_email VARCHAR(255) NULL");
                
                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS system_error_log (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    error_type VARCHAR(255) NOT NULL," +
                    "    error_message TEXT," +
                    "    error_signature VARCHAR(64) NOT NULL," +
                    "    service_module VARCHAR(64) DEFAULT 'CORE'," +
                    "    endpoint VARCHAR(255)," +
                    "    http_method VARCHAR(16)," +
                    "    status_code INT DEFAULT 500," +
                    "    request_id VARCHAR(64)," +
                    "    actor_email VARCHAR(255)," +
                    "    ip_address VARCHAR(64)," +
                    "    stack_trace TEXT," +
                    "    occurrence_count INT DEFAULT 1," +
                    "    status VARCHAR(32) DEFAULT 'UNRESOLVED'," +
                    "    last_occurred_at DATETIME(6)," +
                    "    created_at DATETIME(6)," +
                    "    INDEX idx_err_created (created_at)," +
                    "    INDEX idx_err_signature (error_signature)," +
                    "    INDEX idx_err_type (error_type)," +
                    "    INDEX idx_err_status (status)," +
                    "    INDEX idx_err_endpoint (endpoint)" +
                    ")"
                );
                log.info("Successfully ensured audit_log & system_error_log tables are configured.");
            } catch (Exception e) {
                log.debug("Notice on altering audit_log schema: {}", e.getMessage());
            }

            // 5. Ensure app_setting, app_permission, system_role, role_permissions tables exist
            try {
                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS app_setting (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    setting_key VARCHAR(120) NOT NULL UNIQUE," +
                    "    setting_value TEXT," +
                    "    setting_category VARCHAR(50) NOT NULL," +
                    "    setting_type VARCHAR(30) DEFAULT 'STRING'," +
                    "    display_name VARCHAR(150)," +
                    "    description TEXT," +
                    "    is_encrypted BOOLEAN DEFAULT FALSE," +
                    "    updated_at DATETIME(6)," +
                    "    updated_by VARCHAR(120)," +
                    "    INDEX idx_setting_key (setting_key)," +
                    "    INDEX idx_setting_category (setting_category)" +
                    ")"
                );

                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS app_permission (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    code VARCHAR(100) NOT NULL UNIQUE," +
                    "    module VARCHAR(50) NOT NULL," +
                    "    name VARCHAR(150) NOT NULL," +
                    "    description TEXT," +
                    "    is_sensitive BOOLEAN DEFAULT FALSE," +
                    "    created_at DATETIME(6)," +
                    "    INDEX idx_perm_code (code)," +
                    "    INDEX idx_perm_module (module)" +
                    ")"
                );

                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS system_role (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    role_name VARCHAR(60) NOT NULL UNIQUE," +
                    "    display_name VARCHAR(120)," +
                    "    description TEXT," +
                    "    is_system_role BOOLEAN DEFAULT FALSE," +
                    "    is_active BOOLEAN DEFAULT TRUE," +
                    "    created_at DATETIME(6)," +
                    "    updated_at DATETIME(6)," +
                    "    INDEX idx_role_name (role_name)" +
                    ")"
                );

                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS role_permissions (" +
                    "    role_id BIGINT NOT NULL," +
                    "    permission_id BIGINT NOT NULL," +
                    "    PRIMARY KEY (role_id, permission_id)," +
                    "    INDEX idx_rp_role (role_id)," +
                    "    INDEX idx_rp_perm (permission_id)" +
                    ")"
                );

                jdbcTemplate.execute("UPDATE system_role SET is_active = TRUE WHERE is_active IS NULL");
                jdbcTemplate.execute("UPDATE system_role SET is_system_role = FALSE WHERE is_system_role IS NULL");
                jdbcTemplate.execute("UPDATE system_role SET display_name = role_name WHERE display_name IS NULL OR display_name = ''");

                log.info("Successfully ensured app_setting, app_permission, system_role & role_permissions tables exist and normalized.");
            } catch (Exception e) {
                log.debug("Notice on creating settings/RBAC tables: {}", e.getMessage());
            }

            // 6. Seed default settings and RBAC roles
            try {
                if (appSettingService != null) {
                    appSettingService.seedDefaultSettings();
                    appSettingService.refreshCache();
                }
                if (rbacService != null) {
                    rbacService.seedDefaultRolesAndPermissions();
                }
                log.info("Successfully completed settings and RBAC roles startup synchronization.");
            } catch (Exception e) {
                log.warn("Notice on seeding settings and RBAC roles: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.warn("Database schema fix runner completed with note: {}", e.getMessage());
        }
    }
}
