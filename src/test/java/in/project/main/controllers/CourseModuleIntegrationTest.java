package in.project.main.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import in.project.main.EducationApplication;
import in.project.main.dto.CourseDTO;
import in.project.main.entities.Category;
import in.project.main.entities.Course;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.repositories.CategoryRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.services.CourseService;

@SpringBootTest(classes = EducationApplication.class)
@AutoConfigureMockMvc
public class CourseModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CourseService courseService;

    private Course publishedCourse;
    private Course draftCourse;
    private Category testCategory;

    @BeforeEach
    public void setup() {
        if (testCategory == null) {
            testCategory = categoryRepository.findByName("Web Development").orElse(null);
            if (testCategory == null) {
                testCategory = new Category();
                testCategory.setName("Web Development");
                testCategory.setSlug("web-development");
                testCategory.setDescription("Web Dev Courses");
                testCategory = categoryRepository.save(testCategory);
            }
        }

        if (publishedCourse == null) {
            publishedCourse = courseRepository.findBySlug("test-spring-boot-course").orElse(null);
            if (publishedCourse == null) {
                publishedCourse = new Course();
                publishedCourse.setName("Test Spring Boot Course");
                publishedCourse.setSlug("test-spring-boot-course");
                publishedCourse.setShortDescription("A test spring boot course for phase 6 testing.");
                publishedCourse.setDescription("Full syllabus description for testing.");
                publishedCourse.setCategory(testCategory);
                publishedCourse.setOriginalPrice(BigDecimal.valueOf(999));
                publishedCourse.setDiscountedPrice(BigDecimal.valueOf(499));
                publishedCourse.setLevel(CourseLevel.BEGINNER);
                publishedCourse.setStatus(CourseStatus.PUBLISHED);
                publishedCourse.setFeatured(true);
                publishedCourse = courseRepository.save(publishedCourse);
            }
        }

        if (draftCourse == null) {
            draftCourse = courseRepository.findBySlug("test-draft-course").orElse(null);
            if (draftCourse == null) {
                draftCourse = new Course();
                draftCourse.setName("Test Draft Course");
                draftCourse.setSlug("test-draft-course");
                draftCourse.setShortDescription("A secret draft course that should not be visible to public.");
                draftCourse.setDescription("Secret draft description.");
                draftCourse.setCategory(testCategory);
                draftCourse.setOriginalPrice(BigDecimal.valueOf(1999));
                draftCourse.setStatus(CourseStatus.DRAFT);
                draftCourse = courseRepository.save(draftCourse);
            }
        }
    }

    // 1. Public Storefront Catalog
    @Test
    public void testPublicCoursesCatalogRenders() throws Exception {
        mockMvc.perform(get("/courses"))
               .andExpect(status().isOk())
               .andExpect(view().name("public/courses"))
               .andExpect(model().attributeExists("coursesPage"))
               .andExpect(model().attributeExists("categories"))
               .andExpect(model().attributeExists("levels"));
    }

    // 2. Public Storefront Course Detail
    @Test
    public void testPublicCourseDetailRendersForPublishedCourse() throws Exception {
        mockMvc.perform(get("/courses/" + publishedCourse.getSlug()))
               .andExpect(status().isOk())
               .andExpect(view().name("public/course-detail"))
               .andExpect(model().attributeExists("course"))
               .andExpect(model().attribute("isPurchased", false));
    }

    // 3. Draft course is protected from anonymous public visitors
    @Test
    public void testDraftCourseProtectedFromAnonymousAccess() throws Exception {
        mockMvc.perform(get("/courses/" + draftCourse.getSlug()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/courses"));
    }

    // 4. Draft course can be previewed by Administrator
    @Test
    public void testDraftCourseCanBePreviewedByAdmin() throws Exception {
        mockMvc.perform(get("/courses/" + draftCourse.getSlug())
               .with(user("admin@gmail.com").roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("public/course-detail"))
               .andExpect(model().attributeExists("isPreviewMode"));
    }

    // 5. Admin Course Management workspace requires ROLE_ADMIN
    @Test
    public void testAdminCoursesRequiresAdminRole() throws Exception {
        // Anonymous user -> redirected to /login
        mockMvc.perform(get("/admin/courses"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("**/login"));

        // Student user -> forbidden 403
        mockMvc.perform(get("/admin/courses")
               .with(user("student@gmail.com").roles("STUDENT")))
               .andExpect(status().isForbidden());

        // Admin user -> 200 OK with statistics and coursesPage
        mockMvc.perform(get("/admin/courses")
               .with(user("admin@gmail.com").roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/courses/list"))
               .andExpect(model().attributeExists("stats"))
               .andExpect(model().attributeExists("coursesPage"));
    }

    // 6. Admin Add Course Page requires ROLE_ADMIN
    @Test
    public void testAdminAddCoursePageRenders() throws Exception {
        mockMvc.perform(get("/admin/courses/new")
               .with(user("admin@gmail.com").roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/courses/add"))
               .andExpect(model().attributeExists("courseDTO"));
    }
}
