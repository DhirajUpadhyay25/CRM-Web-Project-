package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Feedback;
import in.project.main.services.FeedbackService;

@Controller
public class AdminController 
{
	@Autowired
	private FeedbackService feedbackService;
	
	@Value("${app.admin.email}")
	private String adminEmail;
	
	@Value("${app.admin.password}")
	private String adminPassword;
	
	//-------------Feedback Management----------------------------
	@GetMapping("/admin/feedback")
	public String openFeedbackPage(Model model,
			@RequestParam(name="page", defaultValue = "0") int page,
			@RequestParam(name="size", defaultValue = "4") int size)
	{
		Pageable pageable = PageRequest.of(page, size);
		
		Page<Feedback> feedbackPage = feedbackService.getAllFeedbacksByPagination(pageable);
		
		model.addAttribute("feedbackPage", feedbackPage);
		
		return "admin/feedback/list";
	}

    @PostMapping("/admin/feedback/updateStatus")
    public String updateFeedbackStatus(@RequestParam("id") Long id, @RequestParam("status") String status, RedirectAttributes redirectAttributes)
    {
        boolean success = feedbackService.updateFeedbackStatus(id, status);
        if (success) {
            redirectAttributes.addFlashAttribute("successMsg", "Feedback updated successfully.");
        } else 
        {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to update feedback status.");
        }
        return "redirect:/admin/feedback";
    }
}
