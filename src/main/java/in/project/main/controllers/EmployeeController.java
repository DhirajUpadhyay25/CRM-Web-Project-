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
	

	@GetMapping("/employeeProfile")
	public String openEmployeeProfilePage()
	{
		return "employee-profile";
	}
	
	@GetMapping("/admin/users")
	public String openEmployeeManagementPage(Model model,
					@RequestParam(name="page", defaultValue = "0") int page,
					@RequestParam(name="size", defaultValue = "5") int size)
	{
		Pageable pageable = PageRequest.of(page, size);
		
		Page<Employee> employeePage = employeeService.getAllEmployeeDetailsByPagination(pageable);
		
		model.addAttribute("employeePage", employeePage);
		
		return "admin/users/list";
	}
	
	//---------------add employee starts-----------------------------
	@GetMapping("/admin/users/new")
	public String openAddCoursePage(Model model)
	{
		model.addAttribute("employee", new Employee());
		return "admin/users/add";
	}
	
	@PostMapping("/admin/users/new")
	public String addEmployeeForm(@ModelAttribute("employee") Employee employee, Model model)
	{
		try
		{
			employeeService.addEmployee(employee);
			model.addAttribute("successMsg", "Employee added successfully");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			model.addAttribute("errorMsg", "Employee not added due to some error");
		}
		return "admin/users/add";
	}
	//---------------add employee ends-----------------------------
	
	
	//---------------edit employee starts-----------------------------
	@GetMapping("/admin/users/edit")
	public String openEditEmployeePage(@RequestParam("employeeEmail") String employeeEmail, Model model)
	{
		Employee employee = employeeService.getEmployeeDetails(employeeEmail);
		
		model.addAttribute("employee", employee);
		model.addAttribute("newEmployeeObj", new Employee());
		
		return "admin/users/edit";
	}
	
	@PostMapping("/admin/users/edit")
	public String updateEmployeeDetailsForm(@ModelAttribute("newEmployeeObj") Employee newEmployeeObj, RedirectAttributes redirectAttributes)
	{
		try
		{
			Employee oldEmployeeObj = employeeService.getEmployeeDetails(newEmployeeObj.getEmail());
			newEmployeeObj.setId(oldEmployeeObj.getId());
			
			employeeService.updateEmployeeDetails(newEmployeeObj);
			
			redirectAttributes.addFlashAttribute("successMsg", "Employee details updated successfully");
		}
		catch(Exception e)
		{
			redirectAttributes.addFlashAttribute("errorMsg", "Employee details not updated due to some error");
			e.printStackTrace();
		}
		
		return "redirect:/admin/users";
	}
	//---------------edit employee ends-----------------------------
	
	@GetMapping("/admin/users/delete")
	public String deleteEmployeeDetails(@RequestParam("employeeEmail") String employeeEmail, RedirectAttributes redirectAttributes)
	{
		try
		{
			employeeService.deleteEmployeeDetails(employeeEmail);
			redirectAttributes.addFlashAttribute("successMsg", "Employee deleted successfully");
		}
		catch(Exception e)
		{
			redirectAttributes.addFlashAttribute("errorMsg", "Employee not deleted due to some error");
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