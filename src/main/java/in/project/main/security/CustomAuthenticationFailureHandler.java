package in.project.main.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
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
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String attemptedEmail = request.getParameter("email");
        if (attemptedEmail == null || attemptedEmail.isBlank()) {
            attemptedEmail = "anonymous";
        } else {
            attemptedEmail = attemptedEmail.trim().toLowerCase();
        }

        String clientIp = AuditContextFilter.extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String failureReason = exception != null ? exception.getMessage() : "Bad credentials";

        try {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                attemptedEmail,
                AuditEventType.LOGIN_FAILED,
                "USER_LOGIN_FAILED",
                "Failed sign-in attempt for email '" + attemptedEmail + "' from IP " + clientIp + "."
            )
            .withActor(null, attemptedEmail, null, "ANONYMOUS")
            .withEntity("USER", attemptedEmail, attemptedEmail)
            .withStatus(AuditStatus.FAILED)
            .withSeverity(AuditSeverity.HIGH)
            .withFailure(failureReason)
            .withContext(clientIp, userAgent, null);

            auditLogService.record(audit);
        } catch (Exception ignored) {}

        response.sendRedirect(request.getContextPath() + "/login?error=true");
    }
}
