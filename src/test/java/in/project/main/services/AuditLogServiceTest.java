package in.project.main.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import in.project.main.entities.AuditLog;
import in.project.main.entities.SystemErrorLog;
import in.project.main.entities.enums.AuditCategory;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.repositories.AuditLogRepository;
import in.project.main.repositories.SystemErrorLogRepository;

public class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private SystemErrorLogRepository systemErrorLogRepository;
    private AuditLogService auditLogService;
    private SystemMonitoringService systemMonitoringService;

    private AuditLog sampleLog;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        systemErrorLogRepository = mock(SystemErrorLogRepository.class);

        auditLogService = new AuditLogService();
        ReflectionTestUtils.setField(auditLogService, "auditLogRepository", auditLogRepository);

        systemMonitoringService = new SystemMonitoringService();
        ReflectionTestUtils.setField(systemMonitoringService, "systemErrorLogRepository", systemErrorLogRepository);
        ReflectionTestUtils.setField(systemMonitoringService, "auditLogRepository", auditLogRepository);
        ReflectionTestUtils.setField(systemMonitoringService, "auditLogService", auditLogService);

        sampleLog = new AuditLog();
        sampleLog.setId(101L);
        sampleLog.setActorEmail("admin@edutake.com");
        sampleLog.setActorName("Admin User");
        sampleLog.setActorRole("ADMIN");
        sampleLog.setAction("COURSE_CREATED");
        sampleLog.setEventType(AuditEventType.COURSE_CREATED);
        sampleLog.setCategory(AuditCategory.COURSE);
        sampleLog.setEntityType("COURSE");
        sampleLog.setEntityId("5");
        sampleLog.setEntityName("Java Masterclass");
        sampleLog.setDescription("Admin created new course.");
        sampleLog.setStatus(AuditStatus.SUCCESS);
        sampleLog.setSeverity(AuditSeverity.INFO);
        sampleLog.setIpAddress("192.168.1.100");
        sampleLog.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testRecordAuditEvent_Success() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleLog);

        PlatformAuditEvent event = PlatformAuditEvent.of(
            "admin@edutake.com",
            AuditEventType.COURSE_CREATED,
            "COURSE_CREATED",
            "Admin created course Java Masterclass"
        )
        .withEntity("COURSE", "5", "Java Masterclass")
        .withStatus(AuditStatus.SUCCESS)
        .withSeverity(AuditSeverity.INFO);

        AuditLog saved = auditLogService.record(event);

        assertNotNull(saved);
        assertEquals("admin@edutake.com", saved.getActorEmail());
        assertEquals("COURSE_CREATED", saved.getAction());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testSensitiveDataRedaction() {
        String sensitivePayload = "{\"password\":\"SecretPass123!\",\"apiKey\":\"key_live_9988776655443322\",\"token\":\"eyJhbGciOi...\",\"card\":\"4111222233334444\"}";
        String sanitized = auditLogService.redact(sensitivePayload);

        assertFalse(sanitized.contains("SecretPass123!"));
        assertFalse(sanitized.contains("key_live_9988776655443322"));
        assertFalse(sanitized.contains("4111222233334444"));
        assertTrue(sanitized.contains("[REDACTED]"));
    }

    @Test
    void testGetKpiMetrics() {
        when(auditLogRepository.count()).thenReturn(100L);
        when(auditLogRepository.countByStatus(AuditStatus.SUCCESS)).thenReturn(90L);
        when(auditLogRepository.countByStatus(AuditStatus.FAILED)).thenReturn(8L);
        when(auditLogRepository.countByStatus(AuditStatus.DENIED)).thenReturn(2L);
        when(auditLogRepository.countByCategory(AuditCategory.SECURITY)).thenReturn(5L);
        when(auditLogRepository.countSince(any(LocalDateTime.class))).thenReturn(25L);
        when(auditLogRepository.countDistinctActorsSince(any(LocalDateTime.class))).thenReturn(12L);

        Map<String, Object> kpi = auditLogService.getKpiMetrics();

        assertNotNull(kpi);
        assertEquals(100L, kpi.get("totalEvents"));
        assertEquals(90L, kpi.get("successCount"));
        assertEquals(8L, kpi.get("failedCount"));
        assertEquals(2L, kpi.get("deniedCount"));
        assertEquals(5L, kpi.get("securityCount"));
        assertEquals(90.0, kpi.get("successRate"));
        assertEquals(25L, kpi.get("eventsLast24h"));
        assertEquals(12L, kpi.get("activeActors24h"));
    }

    @Test
    void testExportCsv() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(sampleLog)));
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleLog);

        byte[] csvBytes = auditLogService.exportCsv(
            null, null, null, null, null, null, null, null, "admin@edutake.com"
        );

        assertNotNull(csvBytes);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("ID,Timestamp,Actor Email,Actor Name"));
        assertTrue(csvContent.contains("admin@edutake.com"));
        assertTrue(csvContent.contains("Java Masterclass"));
    }

    @Test
    void testRecordSystemErrorGrouping() {
        SystemErrorLog errorLog = new SystemErrorLog();
        errorLog.setId(1L);
        errorLog.setErrorType("java.lang.NullPointerException");
        errorLog.setOccurrenceCount(1);
        errorLog.setStatus("UNRESOLVED");

        when(systemErrorLogRepository.findByErrorSignatureAndStatus(any(String.class), any(String.class)))
            .thenReturn(Optional.empty());
        when(systemErrorLogRepository.save(any(SystemErrorLog.class))).thenReturn(errorLog);
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleLog);

        Throwable ex = new NullPointerException("Null reference test");
        SystemErrorLog saved = systemMonitoringService.recordError(
            ex, "/test/endpoint", "GET", 500, "TEST", "test@edutake.com"
        );

        assertNotNull(saved);
        assertEquals(1, saved.getOccurrenceCount());
        verify(systemErrorLogRepository).save(any(SystemErrorLog.class));
    }
}
