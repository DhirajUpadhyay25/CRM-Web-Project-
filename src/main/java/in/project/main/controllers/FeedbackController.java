package in.project.main.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Legacy controller - redirects old feedback routes to the new student feedback module.
 */
@Controller
public class FeedbackController {

    @GetMapping("/provideFeedback")
    public String redirectToGiveFeedback() {
        return "redirect:/student/feedback/give";
    }

    @GetMapping("/feedbackForm")
    public String redirectToFeedbackForm() {
        return "redirect:/student/feedback/give";
    }
}
