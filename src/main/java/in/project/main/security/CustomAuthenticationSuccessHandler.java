package in.project.main.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import in.project.main.entities.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Role role = userDetails.getRole();
        
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
