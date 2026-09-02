package in.project.main.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import in.project.main.services.AppSettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {

    @Autowired
    private AppSettingService settingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean isMaintenance = settingService.getBoolean("maintenance.enabled", false);
        if (!isMaintenance) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 1. Always allow static resources and operational endpoints
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") ||
            path.startsWith("/upload/") || path.startsWith("/uploads/") ||
            path.equals("/favicon.ico") || path.equals("/health") || path.equals("/maintenance") ||
            path.equals("/login") || path.equals("/loginForm") || path.equals("/logout") ||
            path.startsWith("/admin/") || path.equals("/admin")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Check if the current authenticated user has administrative bypass authority
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if ("ROLE_ADMIN".equals(ga.getAuthority()) || "ROLE_SUPER_ADMIN".equals(ga.getAuthority())) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }

        // 3. Block non-admin requests
        String message = settingService.get("maintenance.message", "EduTake is currently undergoing scheduled platform upgrades.");
        String expectedTime = settingService.get("maintenance.expected_time", "Shortly");

        // Handle REST / JSON API calls
        if (path.startsWith("/api/") || "application/json".equalsIgnoreCase(request.getHeader("Accept"))) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format(
                "{\"status\":\"MAINTENANCE\",\"message\":\"%s\",\"expectedTime\":\"%s\"}",
                escapeJson(message), escapeJson(expectedTime)
            ));
            return;
        }

        // Handle Web browser requests
        request.setAttribute("maintenanceMessage", message);
        request.setAttribute("expectedTime", expectedTime);
        request.getRequestDispatcher("/maintenance").forward(request, response);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
