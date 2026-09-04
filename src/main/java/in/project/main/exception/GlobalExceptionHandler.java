package in.project.main.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.TypeMismatchException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request, Model model)
    {
        logger.warn("Missing request parameter: {} - URI: {}", ex.getParameterName(), request.getRequestURI());
        model.addAttribute("errorMsg", "Missing required field: " + ex.getParameterName() + ". Please go back and try again.");
        return "error";
    }

    @ExceptionHandler(TypeMismatchException.class)
    public String handleTypeMismatch(TypeMismatchException ex, HttpServletRequest request, Model model)
    {
        logger.warn("Type mismatch for parameter '{}': value '{}' cannot be converted to {} - URI: {}",
                ex.getPropertyName(), ex.getValue(), ex.getRequiredType(), request.getRequestURI());
        model.addAttribute("errorMsg", "Invalid value for field '" + ex.getPropertyName() + "'. Please go back and try again.");
        return "error";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, HttpServletRequest request, Model model)
    {
        logger.error("Runtime error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

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

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, HttpServletRequest request, Model model)
    {
        logger.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

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
}
