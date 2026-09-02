package in.project.main.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import in.project.main.services.SystemMonitoringService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for the EduTake application.
 * Catches unhandled exceptions, records system error telemetry,
 * and displays the error page instead of exposing stack traces.
 */
@ControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired(required = false)
    private SystemMonitoringService systemMonitoringService;

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, HttpServletRequest request, Model model)
    {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        if (systemMonitoringService != null && request != null) {
            try {
                systemMonitoringService.recordError(
                    ex,
                    request.getRequestURI(),
                    request.getMethod(),
                    500,
                    "CORE",
                    request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null
                );
            } catch (Exception ignored) {}
        }

        model.addAttribute("errorMsg", "Something went wrong. Please try again later.");
        return "error";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, HttpServletRequest request, Model model)
    {
        logger.error("Runtime error: {}", ex.getMessage(), ex);

        if (systemMonitoringService != null && request != null) {
            try {
                systemMonitoringService.recordError(
                    ex,
                    request.getRequestURI(),
                    request.getMethod(),
                    500,
                    "CORE",
                    request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null
                );
            } catch (Exception ignored) {}
        }

        model.addAttribute("errorMsg", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.");
        return "error";
    }
}
