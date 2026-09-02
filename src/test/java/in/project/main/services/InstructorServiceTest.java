package in.project.main.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.EducationApplication;
import in.project.main.dto.InstructorDTO;
import in.project.main.dto.InstructorDetailDTO;
import in.project.main.dto.InstructorStatsDTO;
import in.project.main.entities.Category;
import in.project.main.entities.Course;
import in.project.main.entities.Employee;
import in.project.main.entities.Instructor;
import in.project.main.entities.Role;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.entities.enums.InstructorStatus;
import in.project.main.entities.enums.VerificationStatus;
import in.project.main.repositories.CategoryRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.EmployeeRepository;
import in.project.main.repositories.InstructorRepository;

@SpringBootTest(classes = EducationApplication.class)
@Transactional
public class InstructorServiceTest {

    @Autowired
    private InstructorService instructorService;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    public void setup() {
        testCategory = categoryRepository.findByName("Software Engineering").orElse(null);
        if (testCategory == null) {
            testCategory = new Category();
            testCategory.setName("Software Engineering");
            testCategory.setSlug("software-engineering-" + System.currentTimeMillis());
            testCategory.setDescription("Engineering & Architecture");
            testCategory = categoryRepository.save(testCategory);
        }
    }

    @Test
    public void testCreateInstructorSuccess() {
        InstructorDTO dto = new InstructorDTO();
        dto.setFirstName("Linus");
        dto.setLastName("Torvalds");
        dto.setEmail("linus.test@edutake.com");
        dto.setPhone("+1 (555) 987-6543");
        dto.setHeadline("Creator of Linux & Git");
        dto.setSpecialization("Operating Systems & Kernel");
        dto.setBio("Pioneer of open-source operating systems.");
        dto.setSkills("C, Linux, Git, Systems Architecture");
        dto.setStatus(InstructorStatus.ACTIVE);
        dto.setVerificationStatus(VerificationStatus.VERIFIED);
        dto.setPassword("kernelSecret123");

        Instructor created = instructorService.createInstructor(dto, "admin@edutake.com");

        assertNotNull(created.getId());
        assertEquals("Linus Torvalds", created.getName());
        assertEquals("linus.test@edutake.com", created.getEmail());
        assertEquals(InstructorStatus.ACTIVE, created.getStatus());
        assertEquals(VerificationStatus.VERIFIED, created.getVerificationStatus());
        assertEquals(4, created.getSkillList().size());

        // Verify Employee table sync for authentication
        Employee emp = employeeRepository.findByEmail("linus.test@edutake.com");
        assertNotNull(emp);
        assertEquals(Role.INSTRUCTOR, emp.getRole());
        assertEquals("Linus Torvalds", emp.getName());
    }

    @Test
    public void testCreateInstructorDuplicateEmailBlocked() {
        InstructorDTO dto1 = new InstructorDTO();
        dto1.setFirstName("Ada");
        dto1.setLastName("Lovelace");
        dto1.setEmail("ada.test@edutake.com");
        instructorService.createInstructor(dto1, "admin@edutake.com");

        InstructorDTO dto2 = new InstructorDTO();
        dto2.setFirstName("Ada");
        dto2.setLastName("Duplicate");
        dto2.setEmail("ada.test@edutake.com");

        assertThrows(IllegalArgumentException.class, () -> {
            instructorService.createInstructor(dto2, "admin@edutake.com");
        });
    }

    @Test
    public void testUpdateInstructorDetailsAndSync() {
        InstructorDTO dto = new InstructorDTO();
        dto.setFirstName("James");
        dto.setLastName("Gosling");
        dto.setEmail("james.gosling@edutake.com");
        dto.setHeadline("Father of Java");
        dto.setSpecialization("Java Language");
        dto.setStatus(InstructorStatus.ACTIVE);

        Instructor created = instructorService.createInstructor(dto, "admin@edutake.com");

        // Update fields
        InstructorDTO updateDTO = new InstructorDTO();
        updateDTO.setFirstName("Dr. James");
        updateDTO.setLastName("Gosling");
        updateDTO.setEmail("james.gosling.updated@edutake.com");
        updateDTO.setHeadline("Lead Architect & Java Pioneer");
        updateDTO.setSpecialization("Distributed Systems");
        updateDTO.setStatus(InstructorStatus.ACTIVE);
        updateDTO.setVerificationStatus(VerificationStatus.VERIFIED);

        Instructor updated = instructorService.updateInstructor(created.getId(), updateDTO, "admin@edutake.com");

        assertEquals("Dr. James Gosling", updated.getName());
        assertEquals("james.gosling.updated@edutake.com", updated.getEmail());
        assertEquals("Lead Architect & Java Pioneer", updated.getHeadline());

        // Verify Employee credentials updated
        Employee emp = employeeRepository.findByEmail("james.gosling.updated@edutake.com");
        assertNotNull(emp);
        assertEquals("Dr. James Gosling", emp.getName());
    }

    @Test
    public void testInstructorStatusLifecycle() {
        InstructorDTO dto = new InstructorDTO();
        dto.setFirstName("Guido");
        dto.setLastName("van Rossum");
        dto.setEmail("guido.test@edutake.com");
        dto.setStatus(InstructorStatus.ACTIVE);

        Instructor created = instructorService.createInstructor(dto, "admin@edutake.com");
        assertEquals(InstructorStatus.ACTIVE, created.getStatus());

        // Suspend
        Instructor suspended = instructorService.updateInstructorStatus(created.getId(), InstructorStatus.SUSPENDED, "Investigation pending", "admin@edutake.com");
        assertEquals(InstructorStatus.SUSPENDED, suspended.getStatus());

        // Ban
        Instructor banned = instructorService.updateInstructorStatus(created.getId(), InstructorStatus.BANNED, "Terms violation", "admin@edutake.com");
        assertEquals(InstructorStatus.BANNED, banned.getStatus());

        // Reactivate
        Instructor reactivated = instructorService.updateInstructorStatus(created.getId(), InstructorStatus.ACTIVE, "Reinstated by admin", "admin@edutake.com");
        assertEquals(InstructorStatus.ACTIVE, reactivated.getStatus());
    }

    @Test
    public void testCourseAssignmentAndUnassignment() {
        InstructorDTO dto = new InstructorDTO();
        dto.setFirstName("Martin");
        dto.setLastName("Fowler");
        dto.setEmail("martin.fowler@edutake.com");
        dto.setStatus(InstructorStatus.ACTIVE);
        Instructor instructor = instructorService.createInstructor(dto, "admin@edutake.com");

        Course course = new Course();
        course.setName("Refactoring Patterns Masterclass");
        course.setSlug("refactoring-masterclass-" + System.currentTimeMillis());
        course.setShortDescription("Clean architecture and patterns.");
        course.setOriginalPrice(BigDecimal.valueOf(1999));
        course.setCategory(testCategory);
        course.setLevel(CourseLevel.ADVANCED);
        course.setStatus(CourseStatus.PUBLISHED);
        course = courseRepository.save(course);

        // Assign
        instructorService.assignCourse(instructor.getId(), course.getId(), "admin@edutake.com");

        Course assignedCourse = courseRepository.findById(course.getId()).orElse(null);
        assertNotNull(assignedCourse);
        assertNotNull(assignedCourse.getInstructorRef());
        assertEquals(instructor.getId(), assignedCourse.getInstructorRef().getId());
        assertEquals("Martin Fowler", assignedCourse.getInstructor());
        assertEquals("martin.fowler@edutake.com", assignedCourse.getInstructorEmail());

        List<Course> instructorCourses = instructorService.getInstructorCourses(instructor.getId());
        assertEquals(1, instructorCourses.size());

        // Unassign
        instructorService.unassignCourse(instructor.getId(), course.getId(), "admin@edutake.com");

        Course unassignedCourse = courseRepository.findById(course.getId()).orElse(null);
        assertNotNull(unassignedCourse);
        assertNull(unassignedCourse.getInstructorRef());
        assertNull(unassignedCourse.getInstructor());
        assertNull(unassignedCourse.getInstructorEmail());
    }

    @Test
    public void testCannotAssignCourseToSuspendedOrBannedInstructor() {
        InstructorDTO dto = new InstructorDTO();
        dto.setFirstName("Banned");
        dto.setLastName("User");
        dto.setEmail("banned.user@edutake.com");
        dto.setStatus(InstructorStatus.BANNED);
        Instructor instructor = instructorService.createInstructor(dto, "admin@edutake.com");

        Course course = new Course();
        course.setName("Blocked Course");
        course.setSlug("blocked-course-" + System.currentTimeMillis());
        course.setShortDescription("Description");
        course.setOriginalPrice(BigDecimal.valueOf(999));
        course.setCategory(testCategory);
        final Course savedCourse = courseRepository.save(course);

        assertThrows(IllegalStateException.class, () -> {
            instructorService.assignCourse(instructor.getId(), savedCourse.getId(), "admin@edutake.com");
        });
    }

    @Test
    public void testDeleteInstructorBlockedWhenActiveCoursesAssigned() {
        InstructorDTO dto = new InstructorDTO();
        dto.setFirstName("Kent");
        dto.setLastName("Beck");
        dto.setEmail("kent.beck@edutake.com");
        dto.setStatus(InstructorStatus.ACTIVE);
        Instructor instructor = instructorService.createInstructor(dto, "admin@edutake.com");

        Course course = new Course();
        course.setName("TDD by Example");
        course.setSlug("tdd-example-" + System.currentTimeMillis());
        course.setShortDescription("Test-driven development.");
        course.setOriginalPrice(BigDecimal.valueOf(1299));
        course.setCategory(testCategory);
        course = courseRepository.save(course);

        instructorService.assignCourse(instructor.getId(), course.getId(), "admin@edutake.com");

        // Deletion must be safely blocked
        assertThrows(IllegalStateException.class, () -> {
            instructorService.deleteInstructor(instructor.getId(), "admin@edutake.com");
        });

        // Unassign course, then deletion should succeed
        instructorService.unassignCourse(instructor.getId(), course.getId(), "admin@edutake.com");
        instructorService.deleteInstructor(instructor.getId(), "admin@edutake.com");

        assertNull(instructorRepository.findByEmail("kent.beck@edutake.com"));
        assertNull(employeeRepository.findByEmail("kent.beck@edutake.com"));
    }

    @Test
    public void testSearchAndFilterInstructors() {
        InstructorDTO dto = new InstructorDTO();
        dto.setFirstName("Barbara");
        dto.setLastName("Liskov");
        dto.setEmail("barbara.liskov@edutake.com");
        dto.setSpecialization("Type Theory & Abstraction");
        dto.setSkills("Java, Modula, CLU, Design Patterns");
        dto.setStatus(InstructorStatus.ACTIVE);
        dto.setVerificationStatus(VerificationStatus.VERIFIED);
        instructorService.createInstructor(dto, "admin@edutake.com");

        // Search by keyword
        Page<Instructor> searchResult = instructorService.searchAndFilterInstructors(
                "Liskov", null, null, null, PageRequest.of(0, 10));
        assertFalse(searchResult.isEmpty());
        assertTrue(searchResult.getContent().stream().anyMatch(i -> i.getName().contains("Barbara Liskov")));

        // Search by skill
        Page<Instructor> skillResult = instructorService.searchAndFilterInstructors(
                "Modula", null, null, null, PageRequest.of(0, 10));
        assertFalse(skillResult.isEmpty());

        // Filter by status
        Page<Instructor> statusResult = instructorService.searchAndFilterInstructors(
                null, InstructorStatus.ACTIVE, null, null, PageRequest.of(0, 10));
        assertFalse(statusResult.isEmpty());
    }

    @Test
    public void testInstructorStatistics() {
        InstructorStatsDTO stats = instructorService.getInstructorStatistics();
        assertNotNull(stats);
        assertTrue(stats.getTotalInstructors() >= 0);
    }

    @Test
    public void testInstructorDetailAggregation() {
        InstructorDTO dto = new InstructorDTO();
        dto.setFirstName("Brian");
        dto.setLastName("Goetz");
        dto.setEmail("brian.goetz@edutake.com");
        dto.setHeadline("Java Language Architect");
        dto.setSpecialization("Concurrency & JVM");
        dto.setStatus(InstructorStatus.ACTIVE);
        Instructor instructor = instructorService.createInstructor(dto, "admin@edutake.com");

        InstructorDetailDTO detail = instructorService.getInstructorDetail(instructor.getId());
        assertNotNull(detail);
        assertEquals(instructor.getId(), detail.getInstructor().getId());
        assertEquals("Brian Goetz", detail.getInstructor().getName());
        assertEquals(0, detail.getTotalCourses());
        assertEquals(0, detail.getTotalStudents());
    }
}
