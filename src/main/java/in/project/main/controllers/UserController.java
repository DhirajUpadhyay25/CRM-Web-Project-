package in.project.main.controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import in.project.main.security.CustomUserDetails;
import in.project.main.entities.Role;
import org.springframework.web.multipart.MultipartFile;

import in.project.main.dto.PurchasedCourse;
import in.project.main.entities.Course;
import in.project.main.entities.User;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.services.CourseService;
import in.project.main.services.UserService;
import in.project.main.services.CategoryService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;

@Controller
public class UserController
{
	private String UPLOAD_DIR = System.getProperty("user.dir") + "/upload/";
	private final String IMAGE_URL = "/upload/";	
	@Autowired
	private UserService userService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private CourseService courseService;
	
	@Autowired
	private OrdersRepository ordersRepository;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private CourseRepository courseRepository;
	
	@Value("${app.razorpay.key-id}")
	private String razorpayKeyId;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@GetMapping({"/", "/index"})
	public String openIndexPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails, jakarta.servlet.http.HttpServletRequest request)
	{
		request.getSession(true); // Force session creation for CSRF token
		
		// Query featured & published courses directly from database
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 8);
		org.springframework.data.domain.Page<Course> featuredPage = courseService.getFeaturedCourses(pageable);
		
		List<Course> coursesList;
		if (featuredPage.hasContent()) {
			coursesList = featuredPage.getContent();
		} else {
			coursesList = courseService.getPublishedCourses(pageable).getContent();
		}
		
		model.addAttribute("coursesList", coursesList);
		model.addAttribute("razorpayKeyId", razorpayKeyId);

		// Platform dynamic stats
		long totalStudentsCount = userRepository.countByBanStatusFalse();
		long totalCoursesCount = courseRepository.countByStatus(CourseStatus.PUBLISHED);
		int totalInstructorsCount = employeeRepository.findByRole(Role.INSTRUCTOR).size();

		model.addAttribute("totalStudentsCount", totalStudentsCount);
		model.addAttribute("totalCoursesCount", totalCoursesCount);
		model.addAttribute("totalInstructorsCount", totalInstructorsCount);
		model.addAttribute("categories", categoryService.getActiveCategories());
		
		if(userDetails != null && userDetails.getRole() == Role.STUDENT)
		{
			List<Object[]> purchasedCourseList = ordersRepository.findPurchasedCoursesByEmail(userDetails.getUsername());
			
			List<String> purchasedCoursesNameList = new ArrayList<>();
			for(Object[] course : purchasedCourseList)
			{
				String courseName = (String) course[3];
				purchasedCoursesNameList.add(courseName);
			}
			
			model.addAttribute("purchasedCoursesNameList", purchasedCoursesNameList);
			
			User sessionUser = userRepository.findByEmail(userDetails.getUsername());
			model.addAttribute("sessionUser", sessionUser);
		}
		else if (userDetails != null) {
			model.addAttribute("sessionUser", new User());
		}
		
		return "index";
	}
	
	//-----------register starts---------------------------------
	@GetMapping("/register")
	public String openRegisterPage(Model model, jakarta.servlet.http.HttpServletRequest request)
	{
		request.getSession(true); // Force session creation for CSRF token
		model.addAttribute("user", new User());
		return "register";
	}
	
	@PostMapping("/regForm")
	public String handleRegForm(
	        @Valid @ModelAttribute("user") User user,
	        BindingResult result,
	        @RequestParam("image") MultipartFile file,
	        Model model)
	{

	    // Validation Check
	    if(result.hasErrors())
	    {
	        return "register";
	    }

	    try
	    {

	        // ---------- IMAGE UPLOAD ----------

	        if(!file.isEmpty())
	        {
	            // Original file name
	            String originalFileName = file.getOriginalFilename();

	            // Unique file name
	            String fileName = System.currentTimeMillis() + "_" + originalFileName;

	            // Create upload folder if not exists
	            File uploadPath = new File(UPLOAD_DIR);

	            if(!uploadPath.exists())
	            {
	                uploadPath.mkdirs();
	            }

	            // Save file
	            File saveFile = new File(UPLOAD_DIR + fileName);

	            file.transferTo(saveFile);

	            // Save image URL into database
	            user.setImageName(IMAGE_URL + fileName);
	        }

	        // ---------- ENCODE PASSWORD ----------
			user.setPassword(passwordEncoder.encode(user.getPassword()));

	        // ---------- SAVE USER ----------

	        userService.registerUserService(user);

	        model.addAttribute("successMsg", "Registered Successfully");

	        return "register";

	    }
	    catch(Exception e)
	    {
	        e.printStackTrace();

	        model.addAttribute("errorMsg", "Registration Failed");

	        return "error";
	    }
	}
	
	//-----------------------login starts---------------------------------
	@GetMapping("/login")
	public String openLoginPage(Model model, jakarta.servlet.http.HttpServletRequest request)
	{
		request.getSession(true); // Force session creation for CSRF token
		model.addAttribute("user", new User());
		return "login";
	}

	
	@GetMapping("/userProfile")
	public String openUserProfile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model)
	{
		User sessionUser = userRepository.findByEmail(userDetails.getUsername());
		model.addAttribute("sessionUser", sessionUser);
		
		// Let's also fetch recent purchases to show on dashboard
		List<Object[]> pcDbList = ordersRepository.findPurchasedCoursesByEmail(userDetails.getUsername());
		List<PurchasedCourse> purchasedCoursesList = new ArrayList<>();
		for(Object[] course : pcDbList) {
			PurchasedCourse pc = new PurchasedCourse();
			pc.setPurchasedOn((String)course[0]);
			pc.setCourseName((String)course[3]);
			purchasedCoursesList.add(pc);
		}
		model.addAttribute("recentPurchases", purchasedCoursesList);
		
		return "user-profile";
	}
	
	@PostMapping("/updateUserProfile")
	public String updateUserProfile(
			@ModelAttribute("sessionUser") User updatedUser,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestParam(value = "image", required = false) org.springframework.web.multipart.MultipartFile file,
			Model model) 
	{
		try {
			User existingUser = userRepository.findByEmail(userDetails.getUsername());
			
			// Update basic details
			existingUser.setName(updatedUser.getName());
			existingUser.setPhoneno(updatedUser.getPhoneno());
			existingUser.setCity(updatedUser.getCity());
			
			// Update image if provided
			if (file != null && !file.isEmpty()) {
				String originalFileName = file.getOriginalFilename();
				String fileName = System.currentTimeMillis() + "_" + originalFileName;
				java.io.File uploadPath = new java.io.File(UPLOAD_DIR);
				if (!uploadPath.exists()) {
					uploadPath.mkdirs();
				}
				java.io.File saveFile = new java.io.File(UPLOAD_DIR + fileName);
				file.transferTo(saveFile);
				existingUser.setImageName(IMAGE_URL + fileName);
			}
			
			// Save updated user (using repository directly since service might hash password blindly)
			userRepository.save(existingUser);
			
			model.addAttribute("successMsg", "Profile updated successfully!");
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("errorMsg", "Failed to update profile.");
		}
		
		// Reload dashboard
		return openUserProfile(userDetails, model);
	}
	
	@GetMapping("/myCourses")
	public String myCoursesPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model)
	{
		List<Object[]> pcDbList = ordersRepository.findPurchasedCoursesByEmail(userDetails.getUsername());
		
		List<PurchasedCourse> purchasedCoursesList = new ArrayList<>();
		
		for(Object[] course : pcDbList)
		{
//			System.out.println(course[0]);
//			System.out.println(course[1]);
//			System.out.println(course[2]);
//			System.out.println(course[3]);
//			System.out.println(course[4]);
			
			PurchasedCourse purchasedCourse = new PurchasedCourse();
			
			purchasedCourse.setPurchasedOn((String)course[0]);
			purchasedCourse.setDescription((String)course[1]);
			purchasedCourse.setImageUrl((String)course[2]);
			purchasedCourse.setCourseName((String)course[3]);
			purchasedCourse.setUpdatedOn((String)course[4]);
			
			purchasedCoursesList.add(purchasedCourse);
		}
		
		model.addAttribute("purchasedCoursesList", purchasedCoursesList);
		
		return "my-courses";
	}
}