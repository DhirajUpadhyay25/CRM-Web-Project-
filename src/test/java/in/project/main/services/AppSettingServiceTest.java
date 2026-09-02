package in.project.main.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import in.project.main.entities.AppSetting;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AppSettingRepository;

public class AppSettingServiceTest {

    private AppSettingRepository settingRepository;
    private AuditLogService auditLogService;
    private AppSettingService settingService;

    @BeforeEach
    void setUp() {
        settingRepository = mock(AppSettingRepository.class);
        auditLogService = mock(AuditLogService.class);

        settingService = new AppSettingService();
        ReflectionTestUtils.setField(settingService, "settingRepository", settingRepository);
        ReflectionTestUtils.setField(settingService, "auditLogService", auditLogService);
    }

    @Test
    void testGetDefaultValueWhenNotFound() {
        when(settingRepository.findBySettingKey("unknown.key")).thenReturn(Optional.empty());
        String val = settingService.get("unknown.key", "fallback");
        assertEquals("fallback", val);
    }

    @Test
    void testGetBooleanAndIntParsing() {
        AppSetting s1 = new AppSetting("platform.student_registration_enabled", "true", "PLATFORM", "BOOLEAN", "Reg", "Desc", false);
        AppSetting s2 = new AppSetting("security.min_password_length", "8", "SECURITY", "NUMBER", "Min Pass", "Desc", false);

        when(settingRepository.findBySettingKey("platform.student_registration_enabled")).thenReturn(Optional.of(s1));
        when(settingRepository.findBySettingKey("security.min_password_length")).thenReturn(Optional.of(s2));

        assertTrue(settingService.getBoolean("platform.student_registration_enabled", false));
        assertEquals(8, settingService.getInt("security.min_password_length", 6));
    }

    @Test
    void testGetCategorySettingsMapMasksEncryptedSecrets() {
        AppSetting s1 = new AppSetting("email.smtp_host", "smtp.gmail.com", "EMAIL", "STRING", "Host", "Desc", false);
        AppSetting s2 = new AppSetting("email.smtp_password", "mySuperSecretPassword", "EMAIL", "ENCRYPTED", "Password", "Desc", true);

        when(settingRepository.findBySettingCategoryOrderBySettingKeyAsc("EMAIL")).thenReturn(Arrays.asList(s1, s2));

        Map<String, Object> map = settingService.getCategorySettingsMap("EMAIL");
        assertEquals("smtp.gmail.com", map.get("email.smtp_host"));
        assertEquals("••••••••", map.get("email.smtp_password"));
    }

    @Test
    void testUpdateSettingPersistsAndLogsAudit() {
        AppSetting existing = new AppSetting("general.app_name", "EduTake", "GENERAL", "STRING", "App Name", "Desc", false);
        existing.setId(10L);

        when(settingRepository.findBySettingKey("general.app_name")).thenReturn(Optional.of(existing));
        when(settingRepository.save(any(AppSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        AppSetting updated = settingService.updateSetting("general.app_name", "EduTake LMS Pro", "admin@edutake.com");

        assertNotNull(updated);
        assertEquals("EduTake LMS Pro", updated.getSettingValue());
        assertEquals("admin@edutake.com", updated.getUpdatedBy());
        verify(auditLogService).record(any(PlatformAuditEvent.class));
    }

    @Test
    void testSendTestEmailValidatesAndAudits() {
        settingService.sendTestEmail("test@example.com", "admin@edutake.com");
        verify(auditLogService).record(any(PlatformAuditEvent.class));
    }
}
