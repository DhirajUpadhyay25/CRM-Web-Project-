package in.project.main.controllers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;

import in.project.main.dto.PurchasedCourse;
import in.project.main.entities.Course;
import in.project.main.entities.User;
import in.project.main.repositories.OrdersRepository;
import in.project.main.repositories.UserRepository;
import in.project.main.services.CourseService;
import in.project.main.services.UserService;
import jakarta.validation.Valid;

@Controller
@SessionAttributes("sessionUser")
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
	
	@GetMapping({"/", "/index"})
	public String openIndexPage(Model model, @SessionAttribute(name="sessionUser", required=false) User sessionUser)
	{
		List<Course> coursesList = courseService.getAllCourseDetails();
		model.addAttribute("coursesList", coursesList);
		
		if(sessionUser != null)
		{
			List<Object[]> purchasedCourseList = ordersRepository.findPurchasedCoursesByEmail(sessionUser.getEmail());
			
			List<String> purchasedCoursesNameList = new ArrayList<>();
			for(Object[] course : purchasedCourseList)
			{
				String courseName = (String) course[3];
				purchasedCoursesNameList.add(courseName);
			}
			
			model.addAttribute("purchasedCoursesNameList", purchasedCoursesNameList);
		}
		
		
		return "index";
	}
	
	//-----------register starts---------------------------------
	@GetMapping("/register")
	public String openRegisterPage(Model model)
	{
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
	public String openLoginPage(Model model)
	{
		model.addAttribute("user", new User());
		return "login";
	}
	@PostMapping("/loginForm")
	public String handleLoginForm(@ModelAttribute("user") User user, Model model)
	{
		boolean isAuthenticated = userService.loginUserService(user.getEmail(), user.getPassword());
		if(isAuthenticated)
		{
			User authenticatedUser = userRepository.findByEmail(user.getEmail());
			
			if(authenticatedUser.isBanStatus())
			{
				model.addAttribute("errorMsg", "Sorry, your account is banned, please contact admin, thank you...!!");
				return "login";
			}
			model.addAttribute("sessionUser", authenticatedUser);
			
			return "user-profile";
		}
		else
		{
			model.addAttribute("errorMsg", "Incorrect Email id or Password");
			return "login";
		}
	}
	//-----------------------login finished---------------------------------
	
	
	@GetMapping("/logout")
	public String logout(SessionStatus sessionStatus)
	{
		sessionStatus.setComplete();
		return "login";
	}
	
	@GetMapping("/userProfile")
	public String openUserProfile()
	{
		return "user-profile";
	}
	
	@GetMapping("/myCourses")
	public String myCoursesPage(@SessionAttribute("sessionUser") User sessionUser, Model model)
	{
		List<Object[]> pcDbList = ordersRepository.findPurchasedCoursesByEmail(sessionUser.getEmail());
		
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