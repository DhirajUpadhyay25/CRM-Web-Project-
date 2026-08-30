package in.project.main.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.entities.Course;
import in.project.main.entities.Enrollment;
import in.project.main.entities.User;
import in.project.main.entities.enums.EnrollmentStatus;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EnrollmentRepository;
import in.project.main.repositories.UserRepository;

@Service
public class CustomerService
{
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private EnrollmentRepository enrollmentRepository;
	
	@Autowired
	private CourseRepository courseRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	public Page<User> getAllUserDetailsByPagination(Pageable pageable)
	{
		return userRepository.findAll(pageable);
	}
	
	public User getCustomerDetails(String userEmail)
	{
		return userRepository.findByEmail(userEmail);
	}
	
	public Optional<User> getCustomerDetailsById(Long id)
	{
		return userRepository.findById(id);
	}
	
	public void updateUserBanStatus(User user)
	{
		userRepository.save(user);
	}
	
	public boolean existsByEmail(String email)
	{
		return userRepository.existsByEmail(email);
	}
	
	public Map<String, Object> getStudentStatistics()
	{
		Map<String, Object> stats = new HashMap<>();
		
		long totalStudents = userRepository.count();
		long activeStudents = userRepository.countByBanStatusFalse();
		long bannedStudents = userRepository.countByBanStatusTrue();
		
		LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		long newThisMonth = 0;
		try {
			newThisMonth = userRepository.countNewStudentsSince(monthStart);
		} catch (Exception e) {
			// Field may not exist on older data
		}
		
		long enrolledStudents = 0;
		long notEnrolledStudents = 0;
		try {
			enrolledStudents = enrollmentRepository.countDistinctEnrolledUsers();
			notEnrolledStudents = totalStudents - enrolledStudents;
			if (notEnrolledStudents < 0) notEnrolledStudents = 0;
		} catch (Exception e) {
			// Graceful fallback
		}
		
		long activeEnrollments = 0;
		try {
			activeEnrollments = enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE);
		} catch (Exception e) {}
		
		long completedEnrollments = 0;
		try {
			completedEnrollments = enrollmentRepository.countByStatus(EnrollmentStatus.COMPLETED);
		} catch (Exception e) {}
		
		stats.put("totalStudents", totalStudents);
		stats.put("activeStudents", activeStudents);
		stats.put("bannedStudents", bannedStudents);
		stats.put("newThisMonth", newThisMonth);
		stats.put("enrolledStudents", enrolledStudents);
		stats.put("notEnrolledStudents", notEnrolledStudents);
		stats.put("activeEnrollments", activeEnrollments);
		stats.put("completedEnrollments", completedEnrollments);
		
		return stats;
	}
	
	public Page<User> searchAndFilterStudents(String keyword, Boolean banStatus, Pageable pageable)
	{
		String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
		return userRepository.searchFilterAndSortUsers(cleanKeyword, banStatus, pageable);
	}
	
	@Transactional
	public User createStudent(User user, String rawPassword)
	{
		if (existsByEmail(user.getEmail())) {
			throw new IllegalArgumentException("A student with this email already exists.");
		}
		
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setBanStatus(false);
		
		return userRepository.save(user);
	}
	
	@Transactional
	public User updateStudent(Long id, User updatedData)
	{
		User existing = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));
		
		if (updatedData.getName() != null) existing.setName(updatedData.getName());
		if (updatedData.getEmail() != null) existing.setEmail(updatedData.getEmail());
		if (updatedData.getPhoneno() != null) existing.setPhoneno(updatedData.getPhoneno());
		if (updatedData.getCity() != null) existing.setCity(updatedData.getCity());
		
		return userRepository.save(existing);
	}
	
	@Transactional
	public void toggleBanStatus(Long id)
	{
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));
		user.setBanStatus(!user.isBanStatus());
		userRepository.save(user);
	}
	
	@Transactional
	public void setBanStatus(Long id, boolean banStatus)
	{
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));
		user.setBanStatus(banStatus);
		userRepository.save(user);
	}
	
	@Transactional
	public void deleteStudent(Long id)
	{
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));
		
		List<Enrollment> enrollments = enrollmentRepository.findByUserIdWithCourse(user.getId());
		if (enrollments != null && !enrollments.isEmpty()) {
			throw new IllegalStateException("Cannot delete student '" + user.getName() + 
					"' because they have " + enrollments.size() + " active enrollment(s). Please remove enrollments first or deactivate the account instead.");
		}
		
		userRepository.delete(user);
	}
	
	public List<Enrollment> getStudentEnrollments(Long userId)
	{
		return enrollmentRepository.findByUserIdWithCourse(userId);
	}
	
	public List<Enrollment> getStudentEnrollmentsByEmail(String email)
	{
		return enrollmentRepository.findByUserEmailOrderByEnrolledAtDesc(email);
	}
	
	@Transactional
	public Enrollment enrollStudentInCourse(Long userId, Long courseId)
	{
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + userId));
		
		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));
		
		if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
			throw new IllegalStateException("Student is already enrolled in this course.");
		}
		
		Enrollment enrollment = new Enrollment();
		enrollment.setUser(user);
		enrollment.setCourse(course);
		enrollment.setStatus(EnrollmentStatus.ACTIVE);
		
		return enrollmentRepository.save(enrollment);
	}
	
	@Transactional
	public void unenrollStudent(Long enrollmentId)
	{
		Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
				.orElseThrow(() -> new IllegalArgumentException("Enrollment not found with ID: " + enrollmentId));
		
		enrollment.setStatus(EnrollmentStatus.CANCELLED);
		enrollmentRepository.save(enrollment);
	}
	
	public List<Course> getAllCourses()
	{
		return courseRepository.findAll();
	}
}
