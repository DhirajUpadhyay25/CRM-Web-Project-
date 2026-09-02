package in.project.main.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import in.project.main.audit.AuditContextFilter;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.services.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorEmail = auth != null ? auth.getName() : "anonymous";
        String clientIp = AuditContextFilter.extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();

        try {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                actorEmail,
                AuditEventType.ACCESS_DENIED,
                "ACCESS_DENIED",
                "Access denied for user '" + actorEmail + "' attempting to access protected route [" + request.getMethod() + " " + uri + "]."
            )
            .withActor(null, actorEmail, null, auth != null ? auth.getAuthorities().toString() : "ANONYMOUS")
            .withEntity("ROUTE", uri, uri)
            .withStatus(AuditStatus.DENIED)
            .withSeverity(AuditSeverity.HIGH)
            .withFailure(accessDeniedException != null ? accessDeniedException.getMessage() : "Forbidden")
            .withContext(clientIp, userAgent, null);

            auditLogService.record(audit);
        } catch (Exception ignored) {}

        response.sendRedirect(request.getContextPath() + "/403");
    }
}
