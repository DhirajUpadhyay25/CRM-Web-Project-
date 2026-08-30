package in.project.main.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Course;
import in.project.main.entities.Enrollment;
import in.project.main.entities.User;
import in.project.main.services.CustomerService;

@Controller
@RequestMapping("/admin/students")
public class CustomerController 
{
	@Autowired
	private CustomerService customerService;
	
	@GetMapping
	public String openCustomerManagementPage(
			@RequestParam(name="page", defaultValue = "0") int page,
			@RequestParam(name="size", defaultValue = "10") int size,
			@RequestParam(name="keyword", required = false) String keyword,
			@RequestParam(name="status", required = false) String status,
			@RequestParam(name="sort", defaultValue = "newest") String sort,
			Model model)
	{
		Pageable pageable = createPageable(page, size, sort);
		
		Boolean banStatus = null;
		if (status != null && !status.trim().isEmpty()) {
			switch (status.trim().toUpperCase()) {
				case "ACTIVE": banStatus = false; break;
				case "BANNED": banStatus = true; break;
				case "ALL": banStatus = null; break;
			}
		}
		
		Page<User> userPage = customerService.searchAndFilterStudents(keyword, banStatus, pageable);
		
		Map<String, Object> stats = customerService.getStudentStatistics();
		
		model.addAttribute("userPage", userPage);
		model.addAttribute("stats", stats);
		model.addAttribute("keyword", keyword);
		model.addAttribute("statusFilter", status);
		model.addAttribute("sort", sort);
		model.addAttribute("size", size);
		
		return "admin/students/list";
	}
	
	@GetMapping("/new")
	public String openAddStudentForm(Model model)
	{
		model.addAttribute("newStudent", new User());
		return "admin/students/add";
	}
	
	@PostMapping("/new")
	public String addStudent(
			@ModelAttribute("newStudent") User user,
			@RequestParam("password") String password,
			@RequestParam(value = "confirmPassword", required = false) String confirmPassword,
			RedirectAttributes redirectAttributes)
	{
		try {
			if (password == null || password.trim().isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMsg", "Password is required.");
				return "redirect:/admin/students/new";
			}
			
			if (confirmPassword != null && !password.equals(confirmPassword)) {
				redirectAttributes.addFlashAttribute("errorMsg", "Passwords do not match.");
				return "redirect:/admin/students/new";
			}
			
			if (password.length() < 6) {
				redirectAttributes.addFlashAttribute("errorMsg", "Password must be at least 6 characters.");
				return "redirect:/admin/students/new";
			}
			
			customerService.createStudent(user, password);
			redirectAttributes.addFlashAttribute("successMsg", "Student '" + user.getName() + "' created successfully.");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
			return "redirect:/admin/students/new";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Failed to create student. Please try again.");
			e.printStackTrace();
		}
		
		return "redirect:/admin/students";
	}
	
	@GetMapping("/{id}")
	public String openStudentDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes)
	{
		try {
			User student = customerService.getCustomerDetailsById(id)
					.orElseThrow(() -> new IllegalArgumentException("Student not found"));
			
			List<Enrollment> enrollments = customerService.getStudentEnrollments(id);
			List<Course> allCourses = customerService.getAllCourses();
			
			model.addAttribute("student", student);
			model.addAttribute("enrollments", enrollments);
			model.addAttribute("allCourses", allCourses);
			model.addAttribute("activeEnrollments", enrollments.stream()
					.filter(e -> e.getStatus() == in.project.main.entities.enums.EnrollmentStatus.ACTIVE).count());
			model.addAttribute("completedEnrollments", enrollments.stream()
					.filter(e -> e.getStatus() == in.project.main.entities.enums.EnrollmentStatus.COMPLETED).count());
			
			return "admin/students/detail";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Student not found.");
			return "redirect:/admin/students";
		}
	}
	
	@GetMapping("/{id}/edit")
	public String openEditStudentForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes)
	{
		try {
			User student = customerService.getCustomerDetailsById(id)
					.orElseThrow(() -> new IllegalArgumentException("Student not found"));
			model.addAttribute("editStudent", student);
			return "admin/students/edit";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Student not found.");
			return "redirect:/admin/students";
		}
	}
	
	@PostMapping("/{id}/edit")
	public String updateStudent(
			@PathVariable("id") Long id,
			@ModelAttribute("editStudent") User updatedData,
			RedirectAttributes redirectAttributes)
	{
		try {
			customerService.updateStudent(id, updatedData);
			redirectAttributes.addFlashAttribute("successMsg", "Student updated successfully.");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Failed to update student.");
			e.printStackTrace();
		}
		
		return "redirect:/admin/students/" + id;
	}
	
	@PostMapping("/{id}/toggle-status")
	public String toggleBanStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes)
	{
		try {
			customerService.toggleBanStatus(id);
			User student = customerService.getCustomerDetailsById(id).orElse(null);
			if (student != null) {
				String status = student.isBanStatus() ? "suspended" : "activated";
				redirectAttributes.addFlashAttribute("successMsg", "Student account " + status + " successfully.");
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Failed to change student status.");
			e.printStackTrace();
		}
		return "redirect:/admin/students/" + id;
	}
	
	@PostMapping("/{id}/ban")
	public String banStudent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes)
	{
		try {
			customerService.setBanStatus(id, true);
			redirectAttributes.addFlashAttribute("successMsg", "Student suspended successfully.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Failed to suspend student.");
			e.printStackTrace();
		}
		return "redirect:/admin/students";
	}
	
	@PostMapping("/{id}/unban")
	public String unbanStudent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes)
	{
		try {
			customerService.setBanStatus(id, false);
			redirectAttributes.addFlashAttribute("successMsg", "Student activated successfully.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Failed to activate student.");
			e.printStackTrace();
		}
		return "redirect:/admin/students";
	}
	
	@PostMapping("/{id}/delete")
	public String deleteStudent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes)
	{
		try {
			customerService.deleteStudent(id);
			redirectAttributes.addFlashAttribute("successMsg", "Student deleted successfully.");
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
			return "redirect:/admin/students/" + id;
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Failed to delete student.");
			e.printStackTrace();
		}
		return "redirect:/admin/students";
	}
	
	@PostMapping("/{id}/enroll")
	public String enrollStudent(
			@PathVariable("id") Long id,
			@RequestParam("courseId") Long courseId,
			RedirectAttributes redirectAttributes)
	{
		try {
			customerService.enrollStudentInCourse(id, courseId);
			redirectAttributes.addFlashAttribute("successMsg", "Student enrolled in course successfully.");
		} catch (IllegalStateException | IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Failed to enroll student.");
			e.printStackTrace();
		}
		return "redirect:/admin/students/" + id;
	}
	
	@PostMapping("/enrollment/{enrollmentId}/unenroll")
	public String unenrollStudent(
			@PathVariable("enrollmentId") Long enrollmentId,
			@RequestParam(value = "studentId", required = false) Long studentId,
			RedirectAttributes redirectAttributes)
	{
		try {
			customerService.unenrollStudent(enrollmentId);
			redirectAttributes.addFlashAttribute("successMsg", "Student removed from course successfully.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMsg", "Failed to remove enrollment.");
			e.printStackTrace();
		}
		if (studentId != null) {
			return "redirect:/admin/students/" + studentId;
		}
		return "redirect:/admin/students";
	}
	
	@GetMapping("/courses")
	public String getAllCustomerCourses(
			@RequestParam("userEmail") String email,
			@RequestParam("userName") String custName,
			Model model)
	{
		List<Enrollment> enrollments = customerService.getStudentEnrollmentsByEmail(email);
		model.addAttribute("enrollments", enrollments);
		model.addAttribute("custName", custName);
		model.addAttribute("custEmail", email);
		
		return "admin/students/courses";
	}
	
	private Pageable createPageable(int page, int size, String sort) {
		Sort sortObj;
		if (sort == null) sort = "newest";
		switch (sort) {
			case "oldest":
				sortObj = Sort.by("id").ascending();
				break;
			case "name_asc":
				sortObj = Sort.by("name").ascending();
				break;
			case "name_desc":
				sortObj = Sort.by("name").descending();
				break;
			case "email_asc":
				sortObj = Sort.by("email").ascending();
				break;
			default:
				sortObj = Sort.by("id").descending();
				break;
		}
		return PageRequest.of(page, size, sortObj);
	}
}
