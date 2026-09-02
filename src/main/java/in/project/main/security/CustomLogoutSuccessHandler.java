package in.project.main.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
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
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        if (authentication != null && authentication.getName() != null) {
            String email = authentication.getName();
            String clientIp = AuditContextFilter.extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            try {
                PlatformAuditEvent audit = PlatformAuditEvent.of(
                    email,
                    AuditEventType.LOGOUT,
                    "USER_LOGOUT",
                    "User '" + email + "' signed out."
                )
                .withActor(null, email, null, authentication.getAuthorities().toString())
                .withEntity("USER", email, email)
                .withStatus(AuditStatus.SUCCESS)
                .withSeverity(AuditSeverity.INFO)
                .withContext(clientIp, userAgent, null);

                auditLogService.record(audit);
            } catch (Exception ignored) {}
        }

        response.sendRedirect(request.getContextPath() + "/");
    }
}
