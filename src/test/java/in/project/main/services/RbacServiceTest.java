package in.project.main.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import in.project.main.entities.AppPermission;
import in.project.main.entities.Employee;
import in.project.main.entities.Role;
import in.project.main.entities.SystemRole;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AppPermissionRepository;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.SystemRoleRepository;

public class RbacServiceTest {

    private SystemRoleRepository roleRepository;
    private AppPermissionRepository permissionRepository;
    private EmployeeRepository employeeRepository;
    private AuditLogService auditLogService;
    private RbacService rbacService;

    @BeforeEach
    void setUp() {
        roleRepository = mock(SystemRoleRepository.class);
        permissionRepository = mock(AppPermissionRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        auditLogService = mock(AuditLogService.class);

        rbacService = new RbacService();
        ReflectionTestUtils.setField(rbacService, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(rbacService, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(rbacService, "employeeRepository", employeeRepository);
        ReflectionTestUtils.setField(rbacService, "auditLogService", auditLogService);
    }

    @Test
    void testSuperAdminBypassesAllPermissionChecks() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "admin@edutake.com", "pass",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );

        assertTrue(rbacService.hasPermission(auth, "settings.update"));
        assertTrue(rbacService.hasPermission(auth, "courses.delete"));
    }

    @Test
    void testDirectPermissionAuthorityEvaluation() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "staff@edutake.com", "pass",
            Arrays.asList(
                new SimpleGrantedAuthority("ROLE_STAFF"),
                new SimpleGrantedAuthority("PERM_leads.view"),
                new SimpleGrantedAuthority("PERM_leads.create")
            )
        );

        assertTrue(rbacService.hasPermission(auth, "leads.view"));
        assertTrue(rbacService.hasPermission(auth, "leads.create"));
        assertFalse(rbacService.hasPermission(auth, "settings.update"));
    }

    @Test
    void testCreateCustomRole() {
        AppPermission p1 = new AppPermission("courses.view", "COURSES", "View Courses", "Desc", false);
        when(roleRepository.existsByRoleName("CONTENT_EDITOR")).thenReturn(false);
        when(permissionRepository.findByCode("courses.view")).thenReturn(Optional.of(p1));
        when(roleRepository.save(any(SystemRole.class))).thenAnswer(inv -> inv.getArgument(0));

        SystemRole role = rbacService.createRole("CONTENT_EDITOR", "Content Editor", "Editor role", List.of("courses.view"), "admin@edutake.com");

        assertNotNull(role);
        assertEquals("CONTENT_EDITOR", role.getRoleName());
        assertEquals("Content Editor", role.getDisplayName());
        assertFalse(role.isSystemRole());
        assertEquals(1, role.getPermissions().size());
        verify(auditLogService).record(any(PlatformAuditEvent.class));
    }

    @Test
    void testDeleteSystemRoleThrowsException() {
        SystemRole sysRole = new SystemRole("ADMIN", "Administrator", "Core Admin", true, true);
        sysRole.setId(1L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sysRole));

        assertThrows(IllegalStateException.class, () -> {
            rbacService.deleteRole(1L, "admin@edutake.com");
        });
    }

    @Test
    void testDuplicateRoleCopiesPermissions() {
        AppPermission p1 = new AppPermission("students.view", "STUDENTS", "View Students", "Desc", false);
        SystemRole source = new SystemRole("STUDENT_COORDINATOR", "Coordinator", "Desc", false, true);
        source.setId(5L);
        source.setPermissions(new HashSet<>(List.of(p1)));

        when(roleRepository.findById(5L)).thenReturn(Optional.of(source));
        when(roleRepository.existsByRoleName("SENIOR_COORDINATOR")).thenReturn(false);
        when(roleRepository.save(any(SystemRole.class))).thenAnswer(inv -> inv.getArgument(0));

        SystemRole duplicated = rbacService.duplicateRole(5L, "SENIOR_COORDINATOR", "admin@edutake.com");

        assertNotNull(duplicated);
        assertEquals("SENIOR_COORDINATOR", duplicated.getRoleName());
        assertEquals(1, duplicated.getPermissions().size());
        verify(auditLogService).record(any(PlatformAuditEvent.class));
    }

    @Test
    void testAssignRoleToEmployee() {
        Employee emp = new Employee();
        emp.setId(20L);
        emp.setName("Staff John");
        emp.setEmail("john@edutake.com");
        emp.setRole(Role.EMPLOYEE);

        SystemRole customRole = new SystemRole("STAFF", "Support Staff", "Desc", false, true);
        customRole.setId(2L);

        when(employeeRepository.findById(20L)).thenReturn(Optional.of(emp));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(customRole));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        rbacService.assignRoleToEmployee(20L, 2L, "admin@edutake.com");

        assertEquals(customRole, emp.getSystemRole());
        verify(auditLogService).record(any(PlatformAuditEvent.class));
    }
}
