package in.project.main.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.EducationApplication;
import in.project.main.dto.CourseDTO;
import in.project.main.entities.Category;
import in.project.main.entities.Course;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.repositories.CategoryRepository;
import in.project.main.repositories.CourseRepository;

@SpringBootTest(classes = EducationApplication.class)
@Transactional
public class CourseServiceTest {

    @Autowired
    private CourseService courseService;

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
            testCategory.setSlug("software-engineering");
            testCategory.setDescription("Engineering & Architecture");
            testCategory = categoryRepository.save(testCategory);
        }
    }

    @Test
    public void testCreateCourseGeneratesUniqueSlug() throws java.io.IOException {
        CourseDTO dto1 = new CourseDTO();
        dto1.setName("React Microfrontends Masterclass");
        dto1.setShortDescription("Build microfrontends with Module Federation.");
        dto1.setDescription("Deep dive syllabus into React Module Federation.");
        dto1.setOriginalPrice(BigDecimal.valueOf(1499));
        dto1.setDiscountedPrice(BigDecimal.valueOf(999));
        dto1.setCategoryId(testCategory.getId());
        dto1.setLevel(CourseLevel.INTERMEDIATE);
        dto1.setStatus(CourseStatus.DRAFT);

        Course created1 = courseService.createCourse(dto1, null);
        assertNotNull(created1.getId());
        assertEquals("react-microfrontends-masterclass", created1.getSlug());

        // Create duplicate title course -> slug should receive unique suffix
        CourseDTO dto2 = new CourseDTO();
        dto2.setName("React Microfrontends Masterclass");
        dto2.setShortDescription("Duplicate title test.");
        dto2.setOriginalPrice(BigDecimal.valueOf(1499));
        dto2.setCategoryId(testCategory.getId());
        dto2.setStatus(CourseStatus.DRAFT);

        Course created2 = courseService.createCourse(dto2, null);
        assertNotNull(created2.getId());
        assertTrue(created2.getSlug().startsWith("react-microfrontends-masterclass-"));
    }

    @Test
    public void testPricingHelperMethods() {
        Course freeCourse = new Course();
        freeCourse.setOriginalPrice(BigDecimal.ZERO);
        assertTrue(freeCourse.isFree());
        assertEquals(BigDecimal.ZERO, freeCourse.getEffectivePrice());

        Course discountedCourse = new Course();
        discountedCourse.setOriginalPrice(BigDecimal.valueOf(1000));
        discountedCourse.setDiscountedPrice(BigDecimal.valueOf(800));
        assertFalse(discountedCourse.isFree());
        assertEquals(BigDecimal.valueOf(800), discountedCourse.getEffectivePrice());
        assertEquals(Integer.valueOf(20), discountedCourse.getDiscountPercentage());
    }

    @Test
    public void testStatusTransitions() throws java.io.IOException {
        CourseDTO dto = new CourseDTO();
        dto.setName("Docker & Kubernetes DevOps Blueprint");
        dto.setShortDescription("Container orchestration mastery.");
        dto.setOriginalPrice(BigDecimal.valueOf(2499));
        dto.setCategoryId(testCategory.getId());
        dto.setStatus(CourseStatus.DRAFT);

        Course course = courseService.createCourse(dto, null);
        assertEquals(CourseStatus.DRAFT, course.getStatus());

        // Publish
        Course published = courseService.publishCourse(course.getId());
        assertEquals(CourseStatus.PUBLISHED, published.getStatus());
        assertNotNull(published.getPublishedAt());

        // Archive
        Course archived = courseService.archiveCourse(course.getId());
        assertEquals(CourseStatus.ARCHIVED, archived.getStatus());

        // Restore returns course to DRAFT for review
        Course restored = courseService.restoreCourse(course.getId());
        assertEquals(CourseStatus.DRAFT, restored.getStatus());
    }

    @Test
    public void testPublishValidationRejectsIncompleteCourse() {
        Course course = new Course();
        course.setName(""); // invalid title
        course.setOriginalPrice(null); // missing price
        course.setStatus(CourseStatus.DRAFT);

        assertThrows(IllegalStateException.class, () -> {
            courseService.validatePublishReadiness(course);
        });
    }
}
