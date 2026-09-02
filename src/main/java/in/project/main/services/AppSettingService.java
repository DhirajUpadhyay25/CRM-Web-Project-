package in.project.main.services;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.AppSetting;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AppSettingRepository;
import jakarta.annotation.PostConstruct;

@Service
public class AppSettingService {

    private static final Logger logger = LoggerFactory.getLogger(AppSettingService.class);

    @Autowired
    private AppSettingRepository settingRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    // Fast in-memory cache
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            seedDefaultSettings();
            refreshCache();
        } catch (Exception e) {
            logger.warn("Could not initialize settings during startup (table may not exist yet): {}", e.getMessage());
        }
    }

    public synchronized void refreshCache() {
        try {
            cache.clear();
            List<AppSetting> settings = settingRepository.findAll();
            for (AppSetting s : settings) {
                if (s.getSettingKey() != null && s.getSettingValue() != null) {
                    cache.put(s.getSettingKey(), s.getSettingValue());
                }
            }
            logger.info("Loaded {} application settings into memory cache.", cache.size());
        } catch (Exception e) {
            logger.warn("Error refreshing settings cache: {}", e.getMessage());
        }
    }

    public void invalidateCache() {
        refreshCache();
    }

    public String get(String key, String defaultValue) {
        if (key == null) return defaultValue;
        String val = cache.get(key);
        if (val != null) return val;

        try {
            Optional<AppSetting> opt = settingRepository.findBySettingKey(key);
            if (opt.isPresent() && opt.get().getSettingValue() != null) {
                String fetched = opt.get().getSettingValue();
                cache.put(key, fetched);
                return fetched;
            }
        } catch (Exception ignored) {}

        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val) || "1".equals(val) || "yes".equalsIgnoreCase(val);
    }

    public int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public List<AppSetting> getSettingsByCategory(String category) {
        try {
            return settingRepository.findBySettingCategoryOrderBySettingKeyAsc(category);
        } catch (Exception e) {
            logger.error("Error fetching settings for category {}: {}", category, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getCategorySettingsMap(String category) {
        Map<String, Object> map = new LinkedHashMap<>();
        List<AppSetting> list = getSettingsByCategory(category);
        for (AppSetting s : list) {
            if (s.isEncrypted()) {
                // Mask sensitive values
                map.put(s.getSettingKey(), (s.getSettingValue() != null && !s.getSettingValue().isBlank()) ? "••••••••" : "");
            } else {
                map.put(s.getSettingKey(), s.getSettingValue() != null ? s.getSettingValue() : "");
            }
        }
        return map;
    }

    @Transactional
    public AppSetting updateSetting(String key, String value, String userEmail) {
        if (key == null) throw new IllegalArgumentException("Setting key cannot be null");

        AppSetting setting = settingRepository.findBySettingKey(key)
            .orElseGet(() -> {
                AppSetting s = new AppSetting();
                s.setSettingKey(key);
                s.setSettingCategory("GENERAL");
                s.setDisplayName(key);
                return s;
            });

        String oldValue = setting.getSettingValue();
        
        // If encrypted and new value is the mask, do not overwrite existing value
        if (setting.isEncrypted() && "••••••••".equals(value)) {
            return setting;
        }

        setting.setSettingValue(value != null ? value.trim() : "");
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy(userEmail != null ? userEmail : "SYSTEM");

        AppSetting saved = settingRepository.save(setting);
        cache.put(key, saved.getSettingValue());

        // Audit Logging
        if (auditLogService != null) {
            try {
                String safeOld = setting.isEncrypted() ? "[PROTECTED]" : oldValue;
                String safeNew = setting.isEncrypted() ? "[PROTECTED]" : value;
                
                AuditEventType eventType = AuditEventType.SETTING_UPDATED;
                if ("maintenance.enabled".equals(key)) {
                    eventType = "true".equalsIgnoreCase(value) ? AuditEventType.MAINTENANCE_MODE_ENABLED : AuditEventType.MAINTENANCE_MODE_DISABLED;
                }

                auditLogService.record(
                    PlatformAuditEvent.of(
                        userEmail,
                        eventType,
                        "UPDATE_SETTING",
                        "Updated setting '" + setting.getDisplayName() + "' (" + key + ")"
                    )
                    .withEntity("AppSetting", String.valueOf(setting.getId()), setting.getDisplayName())
                    .withSeverity(AuditSeverity.MEDIUM)
                    .withStatus(AuditStatus.SUCCESS)
                    .withChanges(safeOld, safeNew, key)
                );
            } catch (Exception e) {
                logger.warn("Failed to audit setting update: {}", e.getMessage());
            }
        }

        return saved;
    }

    @Transactional
    public void updateCategorySettings(String category, Map<String, String> values, String userEmail) {
        if (values == null || values.isEmpty()) return;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (key != null && !key.startsWith("_csrf")) {
                updateSetting(key, val, userEmail);
            }
        }
        refreshCache();
    }

    public void sendTestEmail(String recipientEmail, String userEmail) {
        if (recipientEmail == null || !recipientEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid recipient email address");
        }

        String host = get("email.smtp_host", "smtp.gmail.com");
        String port = get("email.smtp_port", "587");
        String sender = get("email.sender_email", "no-reply@edutake.com");

        logger.info("Simulating test email dispatch to {} via {}:{} (Sender: {})", recipientEmail, host, port, sender);

        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        userEmail,
                        AuditEventType.TEST_EMAIL_SENT,
                        "SEND_TEST_EMAIL",
                        "Sent test email to '" + recipientEmail + "' via SMTP host " + host + ":" + port
                    )
                    .withEntity("EmailService", "0", recipientEmail)
                    .withSeverity(AuditSeverity.LOW)
                    .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception e) {
                logger.warn("Failed to audit test email dispatch: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public void seedDefaultSettings() {
        registerDefault("general.app_name", "EduTake", "GENERAL", "STRING", "Application Name", "The official name of the LMS platform.", false);
        registerDefault("general.app_logo", "/images/logo.png", "GENERAL", "STRING", "Application Logo URL", "URL or path to application logo.", false);
        registerDefault("general.app_favicon", "/images/favicon.ico", "GENERAL", "STRING", "Application Favicon URL", "URL or path to favicon icon.", false);
        registerDefault("general.app_description", "Enterprise Learning & Student Management System", "GENERAL", "STRING", "Application Description", "Short tagline or platform description.", false);
        registerDefault("general.contact_email", "contact@edutake.com", "GENERAL", "STRING", "Contact Email", "Public contact email address.", false);
        registerDefault("general.support_email", "support@edutake.com", "GENERAL", "STRING", "Support Email", "Customer and student support email.", false);
        registerDefault("general.support_phone", "+91 98765 43210", "GENERAL", "STRING", "Support Phone", "Public contact/support phone number.", false);
        registerDefault("general.website_url", "http://localhost:8080", "GENERAL", "STRING", "Website URL", "Base URL for the application.", false);
        registerDefault("general.default_language", "en", "GENERAL", "STRING", "Default Language", "Primary platform language (e.g. en, hi).", false);
        registerDefault("general.default_timezone", "Asia/Kolkata", "GENERAL", "STRING", "Default Timezone", "System time standard timezone.", false);
        registerDefault("general.date_format", "YYYY-MM-DD", "GENERAL", "STRING", "Date Format", "Display date format standard.", false);
        registerDefault("general.time_format", "24h", "GENERAL", "STRING", "Time Format", "12h or 24h format standard.", false);
        registerDefault("general.currency", "INR", "GENERAL", "STRING", "Currency Code", "Base currency code (e.g. INR, USD).", false);
        registerDefault("general.currency_symbol", "₹", "GENERAL", "STRING", "Currency Symbol", "Display symbol (e.g. ₹, $).", false);
        registerDefault("general.country_region", "India", "GENERAL", "STRING", "Country / Region", "Platform base country.", false);
        registerDefault("general.default_pagination_size", "15", "GENERAL", "NUMBER", "Default Pagination Size", "Default page size for data tables.", false);

        // Platform / LMS Settings
        registerDefault("platform.student_registration_enabled", "true", "PLATFORM", "BOOLEAN", "Allow Student Registration", "Allow new students to sign up publicly.", false);
        registerDefault("platform.student_email_verification_required", "false", "PLATFORM", "BOOLEAN", "Require Student Email Verification", "Require email confirmation before login.", false);
        registerDefault("platform.student_admin_approval_required", "false", "PLATFORM", "BOOLEAN", "Require Admin Approval for Students", "Require manual approval before student activation.", false);
        registerDefault("platform.student_profile_edit_allowed", "true", "PLATFORM", "BOOLEAN", "Allow Student Profile Editing", "Allow students to edit their profiles.", false);
        registerDefault("platform.student_account_deletion_allowed", "false", "PLATFORM", "BOOLEAN", "Allow Student Self-Deletion", "Allow students to delete their accounts.", false);
        registerDefault("platform.student_default_status", "ACTIVE", "PLATFORM", "STRING", "Default Student Status", "Initial status for new students.", false);
        registerDefault("platform.instructor_registration_enabled", "true", "PLATFORM", "BOOLEAN", "Allow Instructor Applications", "Allow instructors to apply or register.", false);
        registerDefault("platform.instructor_approval_required", "true", "PLATFORM", "BOOLEAN", "Require Instructor Approval", "Require admin verification before teaching.", false);
        registerDefault("platform.instructor_verification_required", "true", "PLATFORM", "BOOLEAN", "Require Document Verification", "Require KYC/ID verification for instructors.", false);
        registerDefault("platform.instructor_profile_visibility", "PUBLIC", "PLATFORM", "STRING", "Instructor Profile Visibility", "Public or student-only instructor profiles.", false);
        registerDefault("platform.course_approval_required", "false", "PLATFORM", "BOOLEAN", "Require Admin Review for Courses", "Require admin approval before publishing.", false);
        registerDefault("platform.instructor_publish_allowed", "true", "PLATFORM", "BOOLEAN", "Allow Instructors to Publish", "Allow instructors to publish courses directly.", false);
        registerDefault("platform.course_edit_after_publish", "true", "PLATFORM", "BOOLEAN", "Allow Editing Published Courses", "Allow modifying curriculum after publishing.", false);
        registerDefault("platform.free_courses_allowed", "true", "PLATFORM", "BOOLEAN", "Allow Free Courses", "Allow courses with zero price.", false);
        registerDefault("platform.paid_courses_allowed", "true", "PLATFORM", "BOOLEAN", "Allow Paid Courses", "Allow commercial paid courses.", false);
        registerDefault("platform.enrollment_self_allowed", "true", "PLATFORM", "BOOLEAN", "Allow Student Self-Enrollment", "Allow students to enroll via checkout.", false);
        registerDefault("platform.enrollment_cancellation_allowed", "true", "PLATFORM", "BOOLEAN", "Allow Enrollment Cancellation", "Allow cancellation / refund requests.", false);
        registerDefault("platform.enrollment_expiration_days", "365", "PLATFORM", "NUMBER", "Enrollment Validity (Days)", "Number of days enrollment remains valid.", false);

        // Security Settings
        registerDefault("security.min_password_length", "6", "SECURITY", "NUMBER", "Minimum Password Length", "Minimum characters required for passwords.", false);
        registerDefault("security.password_complexity_required", "false", "SECURITY", "BOOLEAN", "Require Password Complexity", "Require mix of uppercase, lowercase, numbers, symbols.", false);
        registerDefault("security.max_login_attempts", "5", "SECURITY", "NUMBER", "Max Failed Login Attempts", "Lock account after consecutive failed logins.", false);
        registerDefault("security.lockout_duration_minutes", "15", "SECURITY", "NUMBER", "Lockout Duration (Minutes)", "Account lockout time after failed attempts.", false);
        registerDefault("security.session_timeout_minutes", "60", "SECURITY", "NUMBER", "Session Timeout (Minutes)", "Inactivity duration before automatic logout.", false);
        registerDefault("security.allow_multiple_sessions", "true", "SECURITY", "BOOLEAN", "Allow Concurrent Sessions", "Allow logins from multiple devices simultaneously.", false);
        registerDefault("security.login_notification_enabled", "true", "SECURITY", "BOOLEAN", "Send Login Notifications", "Notify users on successful login.", false);
        registerDefault("security.suspicious_login_detection", "true", "SECURITY", "BOOLEAN", "Detect Suspicious Logins", "Flag unknown IP addresses or rapid location jumps.", false);
        registerDefault("security.password_reset_expiry_hours", "24", "SECURITY", "NUMBER", "Password Reset Expiration (Hours)", "Reset link validity duration.", false);

        // Email / SMTP Settings
        registerDefault("email.enabled", "true", "EMAIL", "BOOLEAN", "Email System Enabled", "Global toggle for outgoing emails.", false);
        registerDefault("email.smtp_host", "smtp.gmail.com", "EMAIL", "STRING", "SMTP Host", "Outgoing mail server hostname.", false);
        registerDefault("email.smtp_port", "587", "EMAIL", "NUMBER", "SMTP Port", "Server communication port (587 or 465).", false);
        registerDefault("email.smtp_username", "notifications@edutake.com", "EMAIL", "STRING", "SMTP Username", "Authentication username for SMTP.", false);
        registerDefault("email.smtp_password", "smtpSecretPassword123", "EMAIL", "ENCRYPTED", "SMTP Password", "Authentication password for SMTP.", true);
        registerDefault("email.encryption", "TLS", "EMAIL", "STRING", "Encryption Type", "TLS, SSL, or NONE.", false);
        registerDefault("email.sender_email", "no-reply@edutake.com", "EMAIL", "STRING", "Default Sender Email", "Email address shown in 'From' header.", false);
        registerDefault("email.sender_name", "EduTake Platform", "EMAIL", "STRING", "Default Sender Name", "Display name in 'From' header.", false);

        // File / Media Settings
        registerDefault("media.max_upload_size_mb", "10", "FILE", "NUMBER", "Maximum Upload Size (MB)", "Global file upload limit.", false);
        registerDefault("media.allowed_image_formats", "jpg,jpeg,png,webp,gif", "FILE", "STRING", "Allowed Image Formats", "Comma-separated list of permitted image extensions.", false);
        registerDefault("media.allowed_doc_formats", "pdf,doc,docx,ppt,pptx,zip", "FILE", "STRING", "Allowed Document Formats", "Comma-separated list of permitted document extensions.", false);
        registerDefault("media.allowed_video_formats", "mp4,webm,mov", "FILE", "STRING", "Allowed Video Formats", "Comma-separated list of permitted video extensions.", false);
        registerDefault("media.max_profile_image_kb", "2048", "FILE", "NUMBER", "Max Profile Image (KB)", "Maximum avatar file size.", false);
        registerDefault("media.max_thumbnail_kb", "4096", "FILE", "NUMBER", "Max Thumbnail Size (KB)", "Maximum course thumbnail file size.", false);

        // Maintenance Settings
        registerDefault("maintenance.enabled", "false", "MAINTENANCE", "BOOLEAN", "Maintenance Mode Enabled", "Put platform into maintenance mode for non-admins.", false);
        registerDefault("maintenance.message", "EduTake is currently undergoing scheduled platform upgrades and performance optimization. We will be back online shortly.", "MAINTENANCE", "STRING", "Maintenance Broadcast Notice", "Public announcement message displayed during maintenance.", false);
        registerDefault("maintenance.expected_time", "2 hours", "MAINTENANCE", "STRING", "Expected Availability", "Estimated completion timeframe.", false);
        registerDefault("maintenance.admin_bypass", "true", "MAINTENANCE", "BOOLEAN", "Allow Admin Bypass", "Allow administrators to access control panel during maintenance.", false);
    }

    private void registerDefault(String key, String defaultValue, String category, String type, String displayName, String description, boolean encrypted) {
        try {
            if (!settingRepository.existsBySettingKey(key)) {
                AppSetting setting = new AppSetting(key, defaultValue, category, type, displayName, description, encrypted);
                setting.setUpdatedAt(LocalDateTime.now());
                setting.setUpdatedBy("SYSTEM_SEEDER");
                settingRepository.save(setting);
            }
        } catch (Exception e) {
            logger.warn("Could not register default setting {}: {}", key, e.getMessage());
        }
    }
}
