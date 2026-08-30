package in.project.main.services;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import in.project.main.dto.CourseDTO;
import in.project.main.dto.CourseStatsDTO;
import in.project.main.entities.Category;
import in.project.main.entities.Course;
import in.project.main.entities.enums.CourseLevel;
import in.project.main.entities.enums.CourseStatus;
import in.project.main.repositories.CategoryRepository;
import in.project.main.repositories.CourseRepository;
import in.project.main.repositories.OrdersRepository;

@Service
public class CourseService {
    
    private static final String UPLOAD_DIR = "upload/courses/";
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    public CourseStatsDTO getCourseStatistics() {
        long total = courseRepository.count();
        long published = courseRepository.countByStatus(CourseStatus.PUBLISHED);
        long draft = courseRepository.countByStatus(CourseStatus.DRAFT);
        long archived = courseRepository.countByStatus(CourseStatus.ARCHIVED);
        long featured = courseRepository.countByFeaturedTrue();
        long free = courseRepository.countFreeCourses();
        long paid = courseRepository.countPaidCourses();

        return new CourseStatsDTO(total, published, draft, archived, featured, free, paid);
    }

    public List<Course> getAllCourseDetails() {
        return courseRepository.findAll();
    }

    public Course getCourseDetailsById(Long id) {
        if (id == null) return null;
        return courseRepository.findById(id).orElse(null);
    }
    
    public Optional<Course> getCourseBySlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) return Optional.empty();
        return courseRepository.findBySlug(slug.trim());
    }
    
    public Optional<Course> getPublishedCourseBySlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) return Optional.empty();
        return courseRepository.findBySlugAndStatus(slug.trim(), CourseStatus.PUBLISHED);
    }

    public Page<Course> getAllCourseDetailsByPagination(Pageable pageable) {
        return courseRepository.findAll(pageable);
    }

    public Page<Course> searchAndFilterCourses(String keyword, CourseStatus status, Long categoryId, 
                                               Boolean featured, String pricingType, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanPricing = (pricingType != null && !pricingType.trim().isEmpty()) ? pricingType.trim().toUpperCase() : null;
        return courseRepository.adminSearchAndFilterCourses(cleanKeyword, status, categoryId, featured, cleanPricing, pageable);
    }

    public Page<Course> searchAndFilterCourses(String keyword, CourseStatus status, Long categoryId, Pageable pageable) {
        return searchAndFilterCourses(keyword, status, categoryId, null, null, pageable);
    }
    
    public Page<Course> getPublishedCourses(Pageable pageable) {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED, pageable);
    }

    public Page<Course> getFeaturedCourses(Pageable pageable) {
        return courseRepository.findByFeaturedTrueAndStatus(CourseStatus.PUBLISHED, pageable);
    }

    public Page<Course> getPublicStorefrontCourses(String keyword, Long categoryId, CourseLevel level, 
                                                   String pricingType, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanPricing = (pricingType != null && !pricingType.trim().isEmpty()) ? pricingType.trim().toUpperCase() : null;
        return courseRepository.publicSearchAndFilterCourses(cleanKeyword, categoryId, level, cleanPricing, pageable);
    }

    public List<Course> getRelatedCourses(Category category, Long currentCourseId) {
        if (category == null) return List.of();
        return courseRepository.findTop4ByCategoryAndStatusAndIdNot(category, CourseStatus.PUBLISHED, currentCourseId);
    }

    @Transactional
    public Course createCourse(CourseDTO dto, MultipartFile courseImg) throws IOException {
        validatePricing(dto.getOriginalPrice(), dto.getDiscountedPrice());

        Course course = new Course();
        mapDtoToEntity(dto, course);
        
        course.setSlug(generateSlug(dto.getName()));

        if (courseImg != null && !courseImg.isEmpty()) {
            course.setImageUrl(saveImage(courseImg));
        }

        if (course.getStatus() == CourseStatus.PUBLISHED) {
            validatePublishReadiness(course);
            course.setPublishedAt(LocalDateTime.now());
        }

        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, CourseDTO dto, MultipartFile courseImg) throws IOException {
        validatePricing(dto.getOriginalPrice(), dto.getDiscountedPrice());

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + id));

        // Update slug only if name changed significantly
        if (!course.getName().equalsIgnoreCase(dto.getName())) {
            course.setSlug(generateSlug(dto.getName(), course.getId()));
        }

        mapDtoToEntity(dto, course);

        if (courseImg != null && !courseImg.isEmpty()) {
            course.setImageUrl(saveImage(courseImg));
        }

        if (course.getStatus() == CourseStatus.PUBLISHED) {
            validatePublishReadiness(course);
            if (course.getPublishedAt() == null) {
                course.setPublishedAt(LocalDateTime.now());
            }
        }

        return courseRepository.save(course);
    }

    @Transactional
    public Course publishCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + id));

        validatePublishReadiness(course);

        course.setStatus(CourseStatus.PUBLISHED);
        if (course.getPublishedAt() == null) {
            course.setPublishedAt(LocalDateTime.now());
        }
        return courseRepository.save(course);
    }

    @Transactional
    public Course archiveCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + id));

        course.setStatus(CourseStatus.ARCHIVED);
        return courseRepository.save(course);
    }

    @Transactional
    public Course restoreCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + id));

        course.setStatus(CourseStatus.DRAFT);
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + id));

        // Critical safety check: prevent deletion if customer orders exist
        if (ordersRepository.existsByCourseName(course.getName())) {
            throw new IllegalStateException("Cannot permanently delete course '" + course.getName() + 
                    "' because active orders/enrollments are associated with it. Please Archive the course instead.");
        }

        courseRepository.delete(course);
    }

    public boolean canSafelyDelete(Long id) {
        Course course = getCourseDetailsById(id);
        if (course == null) return false;
        return !ordersRepository.existsByCourseName(course.getName());
    }

    private void validatePricing(BigDecimal originalPrice, BigDecimal discountedPrice) {
        if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Course price cannot be negative.");
        }
        if (discountedPrice != null) {
            if (discountedPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Discounted price cannot be negative.");
            }
            if (originalPrice != null && discountedPrice.compareTo(originalPrice) > 0) {
                throw new IllegalArgumentException("Discounted price cannot exceed the original price.");
            }
        }
    }

    public void validatePublishReadiness(Course course) {
        if (course.getName() == null || course.getName().trim().isEmpty()) {
            throw new IllegalStateException("Course cannot be published: Title is required.");
        }
        if (course.getCategory() == null) {
            throw new IllegalStateException("Course cannot be published: Category is required.");
        }
        if (course.getOriginalPrice() == null) {
            throw new IllegalStateException("Course cannot be published: Price is required.");
        }
        if (course.getShortDescription() == null || course.getShortDescription().trim().isEmpty()) {
            throw new IllegalStateException("Course cannot be published: Short description is required.");
        }
    }

    private void mapDtoToEntity(CourseDTO dto, Course course) {
        course.setName(dto.getName().trim());
        course.setShortDescription(dto.getShortDescription() != null ? dto.getShortDescription().trim() : null);
        course.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        course.setInstructor(dto.getInstructor() != null ? dto.getInstructor().trim() : null);
        course.setInstructorEmail(dto.getInstructorEmail() != null ? dto.getInstructorEmail().trim() : null);
        course.setLevel(dto.getLevel());
        course.setLanguage(dto.getLanguage() != null ? dto.getLanguage().trim() : null);
        course.setDuration(dto.getDuration() != null ? dto.getDuration().trim() : null);
        course.setOriginalPrice(dto.getOriginalPrice());
        course.setDiscountedPrice(dto.getDiscountedPrice());
        course.setStatus(dto.getStatus());
        course.setFeatured(dto.isFeatured());
        course.setSeoTitle(dto.getSeoTitle() != null ? dto.getSeoTitle().trim() : null);
        course.setSeoDescription(dto.getSeoDescription() != null ? dto.getSeoDescription().trim() : null);

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + dto.getCategoryId()));
            course.setCategory(category);
        }
    }

    private String saveImage(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid image type. Permitted formats: JPEG, PNG, WEBP, GIF.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext)) {
                extension = ext;
            }
        }
        String newFilename = UUID.randomUUID().toString() + extension;
        
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(newFilename);
        Files.write(filePath, file.getBytes());

        return "/upload/courses/" + newFilename;
    }

    private String generateSlug(String name) {
        return generateSlug(name, null);
    }

    private String generateSlug(String name, Long existingCourseId) {
        String baseSlug = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (baseSlug.isEmpty()) {
            baseSlug = "course-" + UUID.randomUUID().toString().substring(0, 8);
        }

        String uniqueSlug = baseSlug;
        int count = 1;
        while (true) {
            Optional<Course> existing = courseRepository.findBySlug(uniqueSlug);
            if (existing.isEmpty() || (existingCourseId != null && existing.get().getId().equals(existingCourseId))) {
                break;
            }
            uniqueSlug = baseSlug + "-" + count;
            count++;
        }
        return uniqueSlug;
    }

    public List<String> getAllCourseNames() {
        return courseRepository.findAll().stream()
                .map(Course::getName)
                .collect(Collectors.toList());
    }
    
    public Course getCourseDetails(String courseName) {
        return courseRepository.findByName(courseName);
    }
}
