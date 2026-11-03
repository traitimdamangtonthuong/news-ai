package com.newsai.news_ai.category;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    /**
     * Tìm category theo slug (ví dụ "ai", "suc-khoe").
     * Dùng khi map breadcrumb href sang Category entity.
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Tìm subcategory theo tên VÀ parent cụ thể.
     * Tránh nhầm "AI" của "Khoa học" với "AI" giả sử có ở chỗ khác.
     */
    Optional<Category> findByNameAndParent(String name, Category parent);

    /**
     * Tìm subcategory theo slug VÀ parent.
     */
    Optional<Category> findBySlugAndParent(String slug, Category parent);
}
