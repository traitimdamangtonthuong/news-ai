package com.newsai.news_ai.category;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newsai.news_ai.error.ResourceNotFoundException;

@Service 
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(
        CategoryRepository categoryRepository) {
            this.categoryRepository = categoryRepository;
        } 
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    };

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    public Category addCategory(Category category) {
        category.setId(null);
        
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, Category updatedCategory) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setName(updatedCategory.getName());

        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        if(!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }

        categoryRepository.deleteById(id);
    }
}
