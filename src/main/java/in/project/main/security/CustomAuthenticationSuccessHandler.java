package in.project.main.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import in.project.main.audit.AuditContextFilter;
import in.project.main.entities.Role;
import in.project.main.entities.enums.AuditEventType;
import in.project.main.entities.enums.AuditSeverity;
import in.project.main.entities.enums.AuditStatus;
import in.project.main.events.PlatformAuditEvent;
import in.project.main.services.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Role role = userDetails.getRole();
        String email = userDetails.getUsername();
        String name = userDetails.getName();
        String roleStr = role != null ? role.name() : "USER";
        String clientIp = AuditContextFilter.extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // Record LOGIN_SUCCESS audit event
        try {
            PlatformAuditEvent audit = PlatformAuditEvent.of(
                email,
                AuditEventType.LOGIN_SUCCESS,
                "USER_LOGIN",
                "User " + (name != null ? name : email) + " successfully signed in (" + roleStr + ")."
            )
            .withActor(email, email, name, roleStr)
            .withEntity("USER", email, email)
            .withStatus(AuditStatus.SUCCESS)
            .withSeverity(AuditSeverity.INFO)
            .withContext(clientIp, userAgent, null);

            auditLogService.record(audit);
        } catch (Exception ignored) {}

        String redirectUrl = request.getContextPath();
        if (role == Role.ADMIN) {
            redirectUrl = "/admin/dashboard";
        } else if (role == Role.EMPLOYEE) {
            redirectUrl = "/employeeProfile";
        } else if (role == Role.INSTRUCTOR) {
            redirectUrl = "/instructor/dashboard";
        } else if (role == Role.STUDENT) {
            redirectUrl = "/student/dashboard";
        }
        
        response.sendRedirect(redirectUrl);
    }
}
