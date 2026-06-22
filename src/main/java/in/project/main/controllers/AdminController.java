package in.project.main.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Course;
import in.project.main.entities.Feedback;
import in.project.main.services.CourseService;
import in.project.main.services.FeedbackService;

@Controller
public class AdminController 
{
	@Autowired
	private CourseService courseService;
	
	@Autowired
	private FeedbackService feedbackService;
	
	@GetMapping("/adminLogin")
	public String openAdminLoginPage()
	{     
		return "admin-login";
	}
	
	@PostMapping("/adminLoginForm")
	public String adminLoginForm(@RequestParam("adminemail") String aemail, @RequestParam("adminpass") String apass, Model model)
	{
		if(aemail.equals("admin@gmail.com") && apass.equals("admin123"))
		{
			return "admin-profile";
		}
		else
		{
			model.addAttribute("errorMsg", "Invalid email id or password");
			return "admin-login";
		}
	}
	
	@GetMapping("/courseManagement")
	public String openCourseManagementPage(Model model,
					@RequestParam(name="page", defaultValue = "0") int page,
					@RequestParam(name="size", defaultValue = "4") int size)
	{
		Pageable pageable = PageRequest.of(page, size);
		
		Page<Course> coursesPage = courseService.getAllCourseDetailsByPagination(pageable);
		
		model.addAttribute("coursesPage", coursesPage);
		
		return "course-management";
	}
	
	//-------------admin logout------------------------
	@GetMapping("/adminLogout")
	public String adminLogout(SessionStatus sessionStatus)
	{
		sessionStatus.setComplete();
		return "admin-login";
	}
	//-------------feedback----------------------------
	@GetMapping("/adminFeedback")
	public String openFeedbackPage(Model model,
			@RequestParam(name="page", defaultValue = "0") int page,
			@RequestParam(name="size", defaultValue = "4") int size)
	{
		Pageable pageable = PageRequest.of(page, size);
		
		Page<Feedback> feedbackPage = feedbackService.getAllFeedbacksByPagination(pageable);
		
		model.addAttribute("feedbackPage", feedbackPage);
		
		return "view-feedbacks";
	}
    @PostMapping("/updateFeedbackStatus")
    public String updateFeedbackStatus(@RequestParam("id") Long id, @RequestParam("status") String status, RedirectAttributes redirectAttributes)
    {
        boolean success = feedbackService.updateFeedbackStatus(id, status);
        if (success) {
            redirectAttributes.addFlashAttribute("successMsg", "Feedback updated successfully.");
        } else 
        {
            redirectAttributes.addFlashAttribute("errorMsg", "Failed to update feedback status.");
        }
        return "redirect:/adminFeedback"; // Redirect to the page where feedbacks are listed
    }
}
