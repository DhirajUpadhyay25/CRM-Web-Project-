package in.project.main.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for the EduTake application.
 * Catches unhandled exceptions and displays the error page
 * instead of exposing stack traces to the user.
 */
@ControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model)
    {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        model.addAttribute("errorMsg", "Something went wrong. Please try again later.");
        return "error";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model)
    {
        logger.error("Runtime error: {}", ex.getMessage(), ex);
        model.addAttribute("errorMsg", ex.getMessage());
        return "error";
    }
}
