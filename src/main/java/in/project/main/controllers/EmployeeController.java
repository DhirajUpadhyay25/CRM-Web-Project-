package in.project.main.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import in.project.main.security.CustomUserDetails;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import in.project.main.entities.Employee;
import in.project.main.entities.Inquiry;
import in.project.main.entities.Orders;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.services.CourseService;
import in.project.main.services.EmployeeService;
import in.project.main.services.OrderService;

import java.security.Principal;
import in.project.main.entities.Role;
import in.project.main.entities.SystemRole;
import in.project.main.repositories.UserRepository;
import in.project.main.services.RbacService;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class EmployeeController
{
	@Autowired
	private EmployeeService employeeService;
	
	@Autowired
	private CourseService courseService;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RbacService rbacService;

	@GetMapping("/employeeProfile")
	public String openEmployeeProfilePage()
	{
		return "employee-profile";
	}
	
	@GetMapping("/admin/users")
	public String openEmployeeManagementPage(Model model,
					@RequestParam(name="q", required = false) String query,
					@RequestParam(name="role", required = false) String role,
					@RequestParam(name="page", defaultValue = "0") int page,
					@RequestParam(name="size", defaultValue = "10") int size)
	{
		Pageable pageable = PageRequest.of(page, size);
		Page<Employee> employeePage = employeeService.searchEmployees(query, role, pageable);
		
		model.addAttribute("employeePage", employeePage);
		model.addAttribute("roles", rbacService.getAllRoles());
		model.addAttribute("roleEnums", Role.values());
		model.addAttribute("currentQuery", query != null ? query : "");
		model.addAttribute("currentRole", role != null ? role : "ALL");
		
		// Summary metrics
		model.addAttribute("totalStaffCount", employeeRepository.count());
		model.addAttribute("totalAdminCount", employeeRepository.findByRole(Role.ADMIN).size());
		model.addAttribute("totalInstructorCount", employeeRepository.findByRole(Role.INSTRUCTOR).size());
		model.addAttribute("totalStudentCount", userRepository.count());
		
		return "admin/users/list";
	}

	@PostMapping("/admin/users/create")
	public String createEmployee(
			@RequestParam String name,
			@RequestParam String email,
			@RequestParam String phoneno,
			@RequestParam String city,
			@RequestParam(required = false) String password,
			@RequestParam(required = false) Long roleId,
			Principal principal,
			RedirectAttributes ra)
	{
		String actorEmail = principal != null ? principal.getName() : "ADMIN";
		try {
			Employee emp = new Employee();
			emp.setName(name);
			emp.setEmail(email);
			emp.setPhoneno(phoneno);
			emp.setCity(city);
			emp.setPassword(password);

			employeeService.createEmployeeWithRole(emp, roleId, actorEmail);
			ra.addFlashAttribute("successMsg", "Platform user '" + name + "' created successfully.");
		} catch (Exception e) {
			ra.addFlashAttribute("errorMsg", "Failed to create user: " + e.getMessage());
		}
		return "redirect:/admin/users";
	}

	@PostMapping("/admin/users/{id}/edit")
	public String editEmployee(
			@PathVariable Long id,
			@RequestParam String name,
			@RequestParam String phoneno,
			@RequestParam String city,
			@RequestParam(required = false) Long roleId,
			Principal principal,
			RedirectAttributes ra)
	{
		String actorEmail = principal != null ? principal.getName() : "ADMIN";
		try {
			employeeService.updateEmployeeProfile(id, name, phoneno, city, roleId, actorEmail);
			ra.addFlashAttribute("successMsg", "User details updated successfully.");
		} catch (Exception e) {
			ra.addFlashAttribute("errorMsg", "Failed to update user: " + e.getMessage());
		}
		return "redirect:/admin/users";
	}

	@PostMapping("/admin/users/{id}/reset-password")
	public String resetPassword(
			@PathVariable Long id,
			@RequestParam String newPassword,
			Principal principal,
			RedirectAttributes ra)
	{
		String actorEmail = principal != null ? principal.getName() : "ADMIN";
		try {
			employeeService.resetEmployeePassword(id, newPassword, actorEmail);
			ra.addFlashAttribute("successMsg", "Password reset successfully.");
		} catch (Exception e) {
			ra.addFlashAttribute("errorMsg", "Failed to reset password: " + e.getMessage());
		}
		return "redirect:/admin/users";
	}

	@PostMapping("/admin/users/{id}/delete")
	public String deleteEmployeePost(
			@PathVariable Long id,
			Principal principal,
			RedirectAttributes ra)
	{
		String actorEmail = principal != null ? principal.getName() : "ADMIN";
		try {
			employeeService.deleteEmployeeById(id, actorEmail);
			ra.addFlashAttribute("successMsg", "User deleted successfully.");
		} catch (Exception e) {
			ra.addFlashAttribute("errorMsg", "Failed to delete user: " + e.getMessage());
		}
		return "redirect:/admin/users";
	}
	
	//---------------legacy add/edit/delete employee endpoints for backward compatibility starts-----------------------------
	@GetMapping("/admin/users/new")
	public String openAddCoursePage(Model model)
	{
		model.addAttribute("employee", new Employee());
		model.addAttribute("roles", rbacService.getAllRoles());
		return "admin/users/add";
	}
	
	@PostMapping("/admin/users/new")
	public String addEmployeeForm(@ModelAttribute("employee") Employee employee, @RequestParam(required = false) Long roleId, Principal principal, Model model)
	{
		String actorEmail = principal != null ? principal.getName() : "ADMIN";
		try
		{
			employeeService.createEmployeeWithRole(employee, roleId, actorEmail);
			model.addAttribute("successMsg", "Employee added successfully");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			model.addAttribute("errorMsg", "Employee not added: " + e.getMessage());
		}
		model.addAttribute("roles", rbacService.getAllRoles());
		return "admin/users/add";
	}
	
	@GetMapping("/admin/users/edit")
	public String openEditEmployeePage(@RequestParam("employeeEmail") String employeeEmail, Model model)
	{
		Employee employee = employeeService.getEmployeeDetails(employeeEmail);
		model.addAttribute("employee", employee);
		model.addAttribute("newEmployeeObj", new Employee());
		model.addAttribute("roles", rbacService.getAllRoles());
		return "admin/users/edit";
	}
	
	@PostMapping("/admin/users/edit")
	public String updateEmployeeDetailsForm(@ModelAttribute("newEmployeeObj") Employee newEmployeeObj, @RequestParam(required = false) Long roleId, Principal principal, RedirectAttributes redirectAttributes)
	{
		String actorEmail = principal != null ? principal.getName() : "ADMIN";
		try
		{
			Employee oldEmployeeObj = employeeService.getEmployeeDetails(newEmployeeObj.getEmail());
			if (oldEmployeeObj != null) {
				employeeService.updateEmployeeProfile(oldEmployeeObj.getId(), newEmployeeObj.getName(), newEmployeeObj.getPhoneno(), newEmployeeObj.getCity(), roleId, actorEmail);
			}
			redirectAttributes.addFlashAttribute("successMsg", "Employee details updated successfully");
		}
		catch(Exception e)
		{
			redirectAttributes.addFlashAttribute("errorMsg", "Employee details not updated: " + e.getMessage());
			e.printStackTrace();
		}
		return "redirect:/admin/users";
	}
	
	@GetMapping("/admin/users/delete")
	public String deleteEmployeeDetails(@RequestParam("employeeEmail") String employeeEmail, Principal principal, RedirectAttributes redirectAttributes)
	{
		String actorEmail = principal != null ? principal.getName() : "ADMIN";
		try
		{
			Employee emp = employeeService.getEmployeeDetails(employeeEmail);
			if (emp != null) {
				employeeService.deleteEmployeeById(emp.getId(), actorEmail);
			}
			redirectAttributes.addFlashAttribute("successMsg", "Employee deleted successfully");
		}
		catch(Exception e)
		{
			redirectAttributes.addFlashAttribute("errorMsg", "Employee not deleted: " + e.getMessage());
			e.printStackTrace();
		}
		return "redirect:/admin/users";
	}
	
	//-------------open sell course page------------------------
	@GetMapping("/sellCourse")
	public String openSellCoursePage(Model model)
	{
		List<String> courseNameList = courseService.getAllCourseNames();
		model.addAttribute("courseNameList", courseNameList);
		
		String uuidOrderId = UUID.randomUUID().toString();
		model.addAttribute("uuidOrderId", uuidOrderId);
		
		model.addAttribute("orders", new Orders());
		
		return "sell-course";
	}
	@PostMapping("/sellCourseForm")
	public String sellCourseForm(@ModelAttribute("orders") Orders orders, @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes)
	{
		try
		{
			orderService.storeEmployeeSale(orders, userDetails.getUsername());
			redirectAttributes.addFlashAttribute("successMsg", "Course provided successfully");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("errorMsg", "Course not provided due to some error");
		}
		return "redirect:/sellCourse";
	}
	
	
	//-------------inquiry management------------------------
	@GetMapping("/inquiryManagement")
	public String openIquiryManagementPage(Model model)
	{
		model.addAttribute("inquiry", new Inquiry());
		return "inquiry-management";
	}
	

}