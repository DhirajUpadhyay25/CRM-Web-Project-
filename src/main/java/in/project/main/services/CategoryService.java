package in.project.main.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.project.main.dto.CategoryDTO;
import in.project.main.entities.Category;
import in.project.main.repositories.CategoryRepository;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    public Category createCategory(CategoryDTO dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setActive(dto.isActive());
        
        // Generate unique slug
        String slug = generateSlug(dto.getName());
        category.setSlug(slug);

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

        // If name changes, check for duplicates and update slug
        if (!category.getName().equalsIgnoreCase(dto.getName())) {
            if (categoryRepository.existsByName(dto.getName())) {
                throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
            }
            category.setName(dto.getName());
            category.setSlug(generateSlug(dto.getName()));
        }

        category.setDescription(dto.getDescription());
        category.setActive(dto.isActive());

        return categoryRepository.save(category);
    }

    private String generateSlug(String name) {
        String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        String uniqueSlug = baseSlug;
        int count = 1;
        while (categoryRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = baseSlug + "-" + count;
            count++;
        }
        return uniqueSlug;
    }
}
