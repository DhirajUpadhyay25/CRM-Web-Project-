package in.project.main.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.AppPermission;
import in.project.main.entities.Employee;
import in.project.main.entities.Role;
import in.project.main.entities.SystemRole;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AppPermissionRepository;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.SystemRoleRepository;
import jakarta.annotation.PostConstruct;

@Service("rbac")
public class RbacService {

    private static final Logger logger = LoggerFactory.getLogger(RbacService.class);

    @Autowired
    private SystemRoleRepository roleRepository;

    @Autowired
    private AppPermissionRepository permissionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @PostConstruct
    public void init() {
        try {
            seedDefaultRolesAndPermissions();
        } catch (Exception e) {
            logger.warn("Could not seed default roles and permissions on startup: {}", e.getMessage());
        }
    }

    // =========================================================================
    // AUTHORIZATION EVALUATION
    // =========================================================================

    public boolean can(String permissionCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return hasPermission(auth, permissionCode);
    }

    public boolean hasPermission(Authentication auth, String permissionCode) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }

        if (permissionCode == null || permissionCode.isBlank()) {
            return true;
        }

        // 1. Super Admin bypasses all checks
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String authority = ga.getAuthority();
            if ("ROLE_SUPER_ADMIN".equals(authority) || "ROLE_ADMIN".equals(authority)) {
                // If SUPER_ADMIN or standard ADMIN, grant full access unless it's a specific custom staff role
                return true;
            }
            // Direct authority match (PERM_students.view or students.view)
            if (authority.equalsIgnoreCase("PERM_" + permissionCode) || authority.equalsIgnoreCase(permissionCode)) {
                return true;
            }
        }

        // 2. Check employee assigned role in database
        String email = auth.getName();
        try {
            Employee emp = employeeRepository.findByEmail(email);
            if (emp != null && emp.getSystemRole() != null) {
                SystemRole sr = emp.getSystemRole();
                if ("SUPER_ADMIN".equalsIgnoreCase(sr.getRoleName()) || "ADMIN".equalsIgnoreCase(sr.getRoleName())) {
                    return true;
                }
                if (sr.getPermissions() != null) {
                    for (AppPermission p : sr.getPermissions()) {
                        if (permissionCode.equalsIgnoreCase(p.getCode())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    public boolean hasAnyPermission(Authentication auth, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) return true;
        for (String code : permissionCodes) {
            if (hasPermission(auth, code)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(Authentication auth, String roleName) {
        if (auth == null || !auth.isAuthenticated()) return false;
        String target = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga.getAuthority().equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // ROLE & PERMISSION MANAGEMENT
    // =========================================================================

    public List<SystemRole> getAllRoles() {
        try {
            return roleRepository.findAll();
        } catch (Exception e) {
            logger.error("Error fetching all roles: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Optional<SystemRole> getRoleById(Long id) {
        if (id == null) return Optional.empty();
        return roleRepository.findById(id);
    }

    public Optional<SystemRole> getRoleByName(String roleName) {
        if (roleName == null) return Optional.empty();
        return roleRepository.findByRoleName(roleName.toUpperCase().trim());
    }

    public List<AppPermission> getAllPermissions() {
        try {
            return permissionRepository.findAllByOrderByModuleAscCodeAsc();
        } catch (Exception e) {
            logger.error("Error fetching all permissions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, List<AppPermission>> getPermissionsGroupedByModule() {
        Map<String, List<AppPermission>> map = new LinkedHashMap<>();
        List<AppPermission> list = getAllPermissions();
        for (AppPermission p : list) {
            String module = p.getModule() != null ? p.getModule() : "OTHER";
            map.computeIfAbsent(module, k -> new ArrayList<>()).add(p);
        }
        return map;
    }

    public long getTotalPermissionsCount() {
        try {
            return permissionRepository.count();
        } catch (Exception e) {
            return 0;
        }
    }

    public long getUserCountForRole(SystemRole role) {
        if (role == null) return 0;
        try {
            return employeeRepository.countBySystemRole(role);
        } catch (Exception e) {
            return 0;
        }
    }

    public Map<Long, Long> getRoleUserCounts() {
        Map<Long, Long> counts = new HashMap<>();
        try {
            List<SystemRole> roles = getAllRoles();
            for (SystemRole r : roles) {
                counts.put(r.getId(), employeeRepository.countBySystemRole(r));
            }
        } catch (Exception e) {
            logger.warn("Error counting users per role: {}", e.getMessage());
        }
        return counts;
    }

    @Transactional
    public SystemRole toggleRoleStatus(Long id, String actorEmail) {
        SystemRole role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));

        role.setActive(!role.isActive());
        role.setUpdatedAt(LocalDateTime.now());
        SystemRole saved = roleRepository.save(role);

        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        actorEmail,
                        AuditEventType.ROLE_UPDATED,
                        "TOGGLE_ROLE_STATUS",
                        "Role '" + saved.getDisplayName() + "' status changed to " + (saved.isActive() ? "ACTIVE" : "INACTIVE")
                    )
                    .withEntity("SystemRole", String.valueOf(saved.getId()), saved.getDisplayName())
                    .withSeverity(AuditSeverity.MEDIUM)
                    .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception ignored) {}
        }
        return saved;
    }

    @Transactional
    public SystemRole createRole(String roleName, String displayName, String description, List<String> permissionCodes, String actorEmail) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be empty");
        }

        String formattedName = roleName.trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        if (roleRepository.existsByRoleName(formattedName)) {
            throw new IllegalArgumentException("Role with name '" + formattedName + "' already exists");
        }

        SystemRole role = new SystemRole();
        role.setRoleName(formattedName);
        role.setDisplayName(displayName != null && !displayName.isBlank() ? displayName.trim() : formattedName);
        role.setDescription(description != null ? description.trim() : "");
        role.setSystemRole(false);
        role.setActive(true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        if (permissionCodes != null && !permissionCodes.isEmpty()) {
            Set<AppPermission> perms = new HashSet<>();
            for (String code : permissionCodes) {
                permissionRepository.findByCode(code.trim()).ifPresent(perms::add);
            }
            role.setPermissions(perms);
        }

        SystemRole saved = roleRepository.save(role);
        logger.info("Created new RBAC role: {} by {}", saved.getRoleName(), actorEmail);

        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        actorEmail,
                        AuditEventType.ROLE_CREATED,
                        "CREATE_ROLE",
                        "Created RBAC role '" + saved.getDisplayName() + "' (" + saved.getRoleName() + ") with " + (saved.getPermissions() != null ? saved.getPermissions().size() : 0) + " permissions."
                    )
                    .withEntity("SystemRole", String.valueOf(saved.getId()), saved.getDisplayName())
                    .withSeverity(AuditSeverity.MEDIUM)
                    .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception e) {
                logger.warn("Failed to audit role creation: {}", e.getMessage());
            }
        }

        return saved;
    }

    @Transactional
    public SystemRole updateRole(Long id, String displayName, String description, Boolean active, List<String> permissionCodes, String actorEmail) {
        SystemRole role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));

        String oldDesc = role.getDescription();
        if (displayName != null && !displayName.isBlank()) {
            role.setDisplayName(displayName.trim());
        }
        if (description != null) {
            role.setDescription(description.trim());
        }
        if (active != null) {
            role.setActive(active);
        }
        role.setUpdatedAt(LocalDateTime.now());

        if (permissionCodes != null) {
            Set<AppPermission> perms = new HashSet<>();
            for (String code : permissionCodes) {
                permissionRepository.findByCode(code.trim()).ifPresent(perms::add);
            }
            role.setPermissions(perms);
        }

        SystemRole updated = roleRepository.save(role);
        logger.info("Updated RBAC role: {} by {}", updated.getRoleName(), actorEmail);

        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        actorEmail,
                        AuditEventType.ROLE_UPDATED,
                        "UPDATE_ROLE",
                        "Updated RBAC role '" + updated.getDisplayName() + "' (" + updated.getRoleName() + "). Total permissions: " + (updated.getPermissions() != null ? updated.getPermissions().size() : 0)
                    )
                    .withEntity("SystemRole", String.valueOf(updated.getId()), updated.getDisplayName())
                    .withSeverity(AuditSeverity.MEDIUM)
                    .withStatus(AuditStatus.SUCCESS)
                    .withChanges(oldDesc, updated.getDescription(), "description")
                );
            } catch (Exception e) {
                logger.warn("Failed to audit role update: {}", e.getMessage());
            }
        }

        return updated;
    }

    @Transactional
    public SystemRole duplicateRole(Long id, String newRoleName, String actorEmail) {
        SystemRole source = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Source role not found with ID: " + id));

        String formattedName = newRoleName.trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        if (roleRepository.existsByRoleName(formattedName)) {
            throw new IllegalArgumentException("Role with name '" + formattedName + "' already exists");
        }

        SystemRole copy = new SystemRole();
        copy.setRoleName(formattedName);
        copy.setDisplayName(source.getDisplayName() + " (Copy)");
        copy.setDescription("Duplicated from " + source.getRoleName() + ": " + source.getDescription());
        copy.setSystemRole(false);
        copy.setActive(true);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());

        if (source.getPermissions() != null) {
            copy.setPermissions(new HashSet<>(source.getPermissions()));
        }

        SystemRole saved = roleRepository.save(copy);
        logger.info("Duplicated RBAC role: {} -> {} by {}", source.getRoleName(), saved.getRoleName(), actorEmail);

        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        actorEmail,
                        AuditEventType.ROLE_CREATED,
                        "DUPLICATE_ROLE",
                        "Duplicated role '" + source.getRoleName() + "' to new role '" + saved.getRoleName() + "' with " + (saved.getPermissions() != null ? saved.getPermissions().size() : 0) + " permissions."
                    )
                    .withEntity("SystemRole", String.valueOf(saved.getId()), saved.getDisplayName())
                    .withSeverity(AuditSeverity.MEDIUM)
                    .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception e) {
                logger.warn("Failed to audit role duplication: {}", e.getMessage());
            }
        }

        return saved;
    }

    @Transactional
    public void deleteRole(Long id, String actorEmail) {
        SystemRole role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));

        if (role.isSystemRole()) {
            throw new IllegalStateException("Protected system role '" + role.getRoleName() + "' cannot be deleted.");
        }

        // Detach employees assigned to this role
        List<Employee> employees = employeeRepository.findAll();
        for (Employee e : employees) {
            if (e.getSystemRole() != null && e.getSystemRole().getId().equals(id)) {
                e.setSystemRole(null);
                employeeRepository.save(e);
            }
        }

        String roleName = role.getRoleName();
        roleRepository.delete(role);
        logger.info("Deleted custom RBAC role: {} by {}", roleName, actorEmail);

        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        actorEmail,
                        AuditEventType.ROLE_DELETED,
                        "DELETE_ROLE",
                        "Deleted custom RBAC role '" + roleName + "' (ID: " + id + ")."
                    )
                    .withEntity("SystemRole", String.valueOf(id), roleName)
                    .withSeverity(AuditSeverity.HIGH)
                    .withStatus(AuditStatus.SUCCESS)
                );
            } catch (Exception e) {
                logger.warn("Failed to audit role deletion: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public void assignRoleToEmployee(Long employeeId, Long roleId, String actorEmail) {
        Employee emp = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));

        SystemRole newRole = null;
        if (roleId != null && roleId > 0) {
            newRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));
        }

        String oldRoleName = emp.getSystemRole() != null ? emp.getSystemRole().getRoleName() : emp.getRole().name();
        emp.setSystemRole(newRole);
        
        // Synchronize base role enum if matching
        if (newRole != null) {
            try {
                Role matchingEnum = Role.valueOf(newRole.getRoleName());
                emp.setRole(matchingEnum);
            } catch (Exception ignored) {}
        }

        employeeRepository.save(emp);
        String newRoleName = newRole != null ? newRole.getRoleName() : "NONE";
        logger.info("Assigned RBAC role to employee {}: {} -> {} by {}", emp.getEmail(), oldRoleName, newRoleName, actorEmail);

        if (auditLogService != null) {
            try {
                auditLogService.record(
                    PlatformAuditEvent.of(
                        actorEmail,
                        AuditEventType.ROLE_ASSIGNED,
                        "ASSIGN_ROLE",
                        "Assigned role '" + newRoleName + "' to user " + emp.getName() + " (" + emp.getEmail() + ")."
                    )
                    .withEntity("Employee", String.valueOf(emp.getId()), emp.getName())
                    .withSeverity(AuditSeverity.HIGH)
                    .withStatus(AuditStatus.SUCCESS)
                    .withChanges("Role: " + oldRoleName, "Role: " + newRoleName, "role")
                );
            } catch (Exception e) {
                logger.warn("Failed to audit role assignment: {}", e.getMessage());
            }
        }
    }

    // =========================================================================
    // SEEDING
    // =========================================================================

    @Transactional
    public void seedDefaultRolesAndPermissions() {
        // 1. Seed Permissions
        seedPermission("dashboard.view", "DASHBOARD", "View Dashboard", "Access administrative summary KPIs and activity stream", false);

        // Learning
        seedPermission("courses.view", "COURSES", "View Courses", "Browse course catalog, modules, and details", false);
        seedPermission("courses.create", "COURSES", "Create Courses", "Author and draft new courses", false);
        seedPermission("courses.update", "COURSES", "Edit Courses", "Modify course syllabus, pricing, and settings", false);
        seedPermission("courses.delete", "COURSES", "Delete Courses", "Permanently remove courses", true);
        seedPermission("courses.publish", "COURSES", "Publish Courses", "Publish or unpublish courses for public enrollment", false);
        seedPermission("courses.approve", "COURSES", "Approve Courses", "Review and approve instructor course submissions", true);

        seedPermission("categories.view", "CATEGORIES", "View Categories", "Browse taxonomy categories", false);
        seedPermission("categories.create", "CATEGORIES", "Create Categories", "Add new course categories", false);
        seedPermission("categories.update", "CATEGORIES", "Edit Categories", "Update category titles and descriptions", false);
        seedPermission("categories.delete", "CATEGORIES", "Delete Categories", "Remove course categories", true);

        seedPermission("batches.view", "BATCHES", "View Batches", "Browse batches and live class schedules", false);
        seedPermission("batches.create", "BATCHES", "Create Batches", "Schedule new batches and class timings", false);
        seedPermission("batches.update", "BATCHES", "Edit Batches", "Update batch details and instructors", false);
        seedPermission("batches.delete", "BATCHES", "Delete Batches", "Remove batch schedules", false);

        seedPermission("students.view", "STUDENTS", "View Students", "Browse student profiles, enrollments, and progress", false);
        seedPermission("students.create", "STUDENTS", "Create Students", "Manually register student accounts", false);
        seedPermission("students.update", "STUDENTS", "Edit Students", "Update student details and credentials", false);
        seedPermission("students.delete", "STUDENTS", "Delete Students", "Delete or ban student accounts", true);
        seedPermission("students.export", "STUDENTS", "Export Students", "Export student data to CSV/Excel", false);

        seedPermission("instructors.view", "INSTRUCTORS", "View Instructors", "Browse instructor profiles and metrics", false);
        seedPermission("instructors.create", "INSTRUCTORS", "Create Instructors", "Register instructor accounts", false);
        seedPermission("instructors.update", "INSTRUCTORS", "Edit Instructors", "Update instructor bio and status", false);
        seedPermission("instructors.approve", "INSTRUCTORS", "Approve Instructors", "Verify and approve instructor KYC", true);
        seedPermission("instructors.delete", "INSTRUCTORS", "Delete Instructors", "Remove instructor accounts", true);

        seedPermission("enrollments.view", "ENROLLMENTS", "View Enrollments", "Browse course enrollments", false);
        seedPermission("enrollments.create", "ENROLLMENTS", "Create Enrollments", "Manually enroll students in courses", false);
        seedPermission("enrollments.update", "ENROLLMENTS", "Update Enrollments", "Modify enrollment expiration and details", false);
        seedPermission("enrollments.suspend", "ENROLLMENTS", "Suspend Enrollments", "Temporarily suspend student course access", true);
        seedPermission("enrollments.cancel", "ENROLLMENTS", "Cancel Enrollments", "Terminate student course enrollment", true);
        seedPermission("enrollments.revoke", "ENROLLMENTS", "Revoke Enrollments", "Administratively revoke course access", true);
        seedPermission("enrollments.bulk_manage", "ENROLLMENTS", "Bulk Manage Enrollments", "Execute batch enrollments and status changes", true);
        seedPermission("enrollments.export", "ENROLLMENTS", "Export Enrollments", "Export enrollment data to CSV", false);
        seedPermission("enrollments.view_payment", "ENROLLMENTS", "View Payment Details", "View financial and transaction records of enrollments", false);
        seedPermission("enrollments.view_analytics", "ENROLLMENTS", "View Enrollment Analytics", "Inspect enrollment trends and conversion analytics", false);

        seedPermission("curriculum.view", "CURRICULUM", "View Curriculum", "View course lessons and topics", false);
        seedPermission("curriculum.create", "CURRICULUM", "Create Curriculum", "Add lessons, videos, and attachments", false);
        seedPermission("curriculum.delete", "CURRICULUM", "Delete Curriculum", "Remove lessons or sections", false);

        seedPermission("certificates.view", "CERTIFICATES", "View Certificates", "Browse issued course completion certificates and claims", false);
        seedPermission("certificates.review", "CERTIFICATES", "Review Requests", "Review pending student certificate claim submissions", false);
        seedPermission("certificates.approve", "CERTIFICATES", "Approve & Issue", "Approve and officially generate student certificates", true);
        seedPermission("certificates.reject", "CERTIFICATES", "Reject Requests", "Reject ineligible student certificate claims", true);
        seedPermission("certificates.revoke", "CERTIFICATES", "Revoke Certificates", "Administratively revoke or nullify issued credentials", true);
        seedPermission("certificates.reissue", "CERTIFICATES", "Reissue Certificates", "Reissue corrected replacement certificates", true);
        seedPermission("certificates.export", "CERTIFICATES", "Export Certificates", "Export certificate records to CSV", false);
        seedPermission("certificates.analytics", "CERTIFICATES", "View Analytics", "Inspect certificate issuance and conversion analytics", false);

        seedPermission("assignments.view", "ASSIGNMENTS", "View Submissions", "Browse student assignment submissions", false);
        seedPermission("assignments.grade", "ASSIGNMENTS", "Grade Submissions", "Score assignments and provide feedback", false);

        // Commerce
        seedPermission("orders.view", "COMMERCE", "View Orders", "Browse sales orders and transactions", false);
        seedPermission("payments.view", "COMMERCE", "View Payments", "Inspect payment gateways and settlements", false);
        seedPermission("coupons.view", "COMMERCE", "View Coupons", "Browse discount coupons", false);
        seedPermission("coupons.create", "COMMERCE", "Create Coupons", "Generate promotional discount codes", false);
        seedPermission("coupons.delete", "COMMERCE", "Delete Coupons", "Remove coupon codes", false);
        seedPermission("refunds.view", "COMMERCE", "View Refunds", "Inspect refund requests and transactions", false);
        seedPermission("refunds.create", "COMMERCE", "Process Refunds", "Approve and execute payment refunds", true);

        // CRM
        seedPermission("leads.view", "CRM", "View Leads", "Browse CRM prospective leads", false);
        seedPermission("leads.create", "CRM", "Create Leads", "Capture new sales leads", false);
        seedPermission("leads.update", "CRM", "Edit Leads", "Update lead stage and pipeline notes", false);
        seedPermission("leads.delete", "CRM", "Delete Leads", "Remove sales leads", false);
        seedPermission("enquiries.view", "CRM", "View Enquiries", "Browse contact and course inquiries", false);
        seedPermission("enquiries.update", "CRM", "Respond Enquiries", "Update inquiry status and admin notes", false);
        seedPermission("followups.view", "CRM", "View Follow-ups", "Track scheduled sales calls and follow-ups", false);
        seedPermission("followups.create", "CRM", "Log Follow-ups", "Schedule client follow-ups", false);

        // Communication
        seedPermission("notifications.view", "COMMUNICATION", "View Notifications", "Access notification center", false);
        seedPermission("notifications.create", "COMMUNICATION", "Send Notifications", "Broadcast platform announcements", false);
        seedPermission("announcements.view", "COMMUNICATION", "View Announcements", "Browse noticeboard announcements", false);
        seedPermission("announcements.create", "COMMUNICATION", "Publish Announcements", "Post public notices", false);
        seedPermission("messages.view", "COMMUNICATION", "View Messages", "Inspect internal message threads", false);

        // Content
        seedPermission("feedback.view", "CONTENT", "View Feedback", "Inspect course ratings and feedback", false);
        seedPermission("pages.view", "CONTENT", "Manage Pages", "Manage static content pages", false);
        seedPermission("blogs.view", "CONTENT", "Manage Blogs", "Author blog posts and articles", false);
        seedPermission("faqs.view", "CONTENT", "Manage FAQs", "Manage help and FAQ entries", false);
        seedPermission("testimonials.view", "CONTENT", "Manage Testimonials", "Manage student success stories", false);
        seedPermission("media.view", "CONTENT", "Media Library", "Upload and browse media files", false);

        // Analytics
        seedPermission("reports.view", "ANALYTICS", "View Reports", "Inspect LMS revenue and enrollment reports", false);
        seedPermission("reports.export", "ANALYTICS", "Export Reports", "Download analytic CSV exports", false);

        // System & Security
        seedPermission("users.view", "SYSTEM", "View Users", "Browse administrative user accounts", true);
        seedPermission("users.create", "SYSTEM", "Create Users", "Create administrative staff accounts", true);
        seedPermission("users.manage_roles", "SYSTEM", "Assign Roles", "Assign RBAC roles to administrative users", true);
        seedPermission("roles.view", "ROLES", "View Roles", "Inspect RBAC roles and permissions", true);
        seedPermission("roles.create", "ROLES", "Create Roles", "Define new custom RBAC roles", true);
        seedPermission("roles.update", "ROLES", "Edit Roles & Permissions", "Modify role permissions matrix", true);
        seedPermission("roles.delete", "ROLES", "Delete Roles", "Delete custom non-system roles", true);
        seedPermission("audit_logs.view", "AUDIT_LOGS", "View Audit Logs", "Inspect security and activity logs", true);
        seedPermission("audit_logs.export", "AUDIT_LOGS", "Export Audit Logs", "Export forensic audit trails to CSV", true);
        seedPermission("monitoring.view", "MONITORING", "System Health", "Inspect system health and error telemetry", true);
        seedPermission("monitoring.resolve", "MONITORING", "Resolve Errors", "Mark system errors as resolved", true);
        seedPermission("settings.view", "SETTINGS", "View Settings", "Inspect application settings", false);
        seedPermission("settings.update", "SETTINGS", "Update Settings", "Modify platform policies and parameters", true);
        seedPermission("security_settings.update", "SETTINGS", "Manage Security Settings", "Modify password policies and authentication rules", true);

        // 2. Seed Default Roles
        List<AppPermission> allPerms = permissionRepository.findAll();
        Set<AppPermission> allPermsSet = new HashSet<>(allPerms);

        // Super Admin (All Permissions)
        seedRole("SUPER_ADMIN", "Super Administrator", "Full unrestricted access to all platform, financial, security, and administrative functions.", true, allPermsSet);

        // Standard Admin
        Set<AppPermission> adminPerms = new HashSet<>(allPerms);
        seedRole("ADMIN", "Administrator", "Standard administrative access to LMS operations, courses, students, and configuration.", true, adminPerms);

        // Instructor
        Set<AppPermission> instructorPerms = new HashSet<>();
        for (AppPermission p : allPerms) {
            String code = p.getCode();
            if (code.startsWith("courses.") || code.startsWith("curriculum.") || code.startsWith("assignments.") ||
                code.startsWith("students.view") || code.startsWith("feedback.view") || code.startsWith("announcements.view") ||
                code.startsWith("notifications.view") || code.equals("dashboard.view")) {
                instructorPerms.add(p);
            }
        }
        seedRole("INSTRUCTOR", "Instructor", "Course creator and teacher access for curriculum authoring, student progress, and grading.", true, instructorPerms);

        // Student
        Set<AppPermission> studentPerms = new HashSet<>();
        for (AppPermission p : allPerms) {
            String code = p.getCode();
            if (code.equals("dashboard.view") || code.equals("courses.view") || code.equals("notifications.view")) {
                studentPerms.add(p);
            }
        }
        seedRole("STUDENT", "Student", "Standard learner access to enrolled courses, quizzes, submissions, and certificates.", true, studentPerms);

        // Staff / Support
        Set<AppPermission> staffPerms = new HashSet<>();
        for (AppPermission p : allPerms) {
            String code = p.getCode();
            if (code.startsWith("leads.") || code.startsWith("enquiries.") || code.startsWith("followups.") ||
                code.startsWith("students.view") || code.startsWith("orders.view") || code.startsWith("notifications.") ||
                code.equals("dashboard.view")) {
                staffPerms.add(p);
            }
        }
        seedRole("STAFF", "Support & Sales Staff", "Customer support and sales lead management access.", true, staffPerms);

        // 3. Link default employees to system roles
        try {
            roleRepository.findByRoleName("SUPER_ADMIN").ifPresent(superAdminRole -> {
                Employee admin = employeeRepository.findByEmail("admin@edutake.com");
                if (admin != null && admin.getSystemRole() == null) {
                    admin.setSystemRole(superAdminRole);
                    employeeRepository.save(admin);
                }
            });
            roleRepository.findByRoleName("INSTRUCTOR").ifPresent(instRole -> {
                Employee inst = employeeRepository.findByEmail("djupadhyay005@gmail.com");
                if (inst != null && inst.getSystemRole() == null) {
                    inst.setSystemRole(instRole);
                    employeeRepository.save(inst);
                }
            });
        } catch (Exception e) {
            logger.debug("Notice on linking default employees to system roles: {}", e.getMessage());
        }
    }

    private void seedPermission(String code, String module, String name, String description, boolean sensitive) {
        if (!permissionRepository.existsByCode(code)) {
            AppPermission perm = new AppPermission(code, module, name, description, sensitive);
            permissionRepository.saveAndFlush(perm);
        }
    }

    private void seedRole(String name, String displayName, String description, boolean isSystem, Set<AppPermission> perms) {
        Optional<SystemRole> existing = roleRepository.findByRoleName(name);
        SystemRole role;
        if (existing.isEmpty()) {
            role = new SystemRole(name, displayName, description, isSystem, true);
        } else {
            role = existing.get();
            role.setDisplayName(displayName);
            role.setDescription(description);
            role.setSystemRole(isSystem);
            role.setActive(true);
        }
        role.setPermissions(perms);
        roleRepository.saveAndFlush(role);
        logger.info("Seeded RBAC role {} with {} permissions", role.getRoleName(), perms != null ? perms.size() : 0);
    }
}
