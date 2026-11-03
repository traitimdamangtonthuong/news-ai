package com.newsai.news_ai.article;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.newsai.news_ai.category.Category;
import com.newsai.news_ai.source.Source;

@Entity // Object nÃ y cÃ³ thá»ƒ Ä‘Æ°á»£c lÆ°u trong relational database báº±ng JPA.
@Table(name = "articles", uniqueConstraints = @UniqueConstraint(
        name = "uk_articles_canonical_url",
        columnNames = "canonicalUrl"))
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Subcategory (danh muc con) doc tu breadcrumb trang bai viet.
     * Vi du: category = "Khoa hoc cong nghe", subCategory = "AI".
     */
    // @ManyToOne
    // @JoinColumn(name = "subcategory_id")
    // private Category subCategory;

    @NotBlank(message = "URL is required")
    @Size(max = 2048)
    @Column(nullable = false, length = 2048, unique = true)
    private String url;

    @Column(length = 2048)
    private String imageUrl;

    private LocalDateTime publishedAt;
    private LocalDateTime crawledAt;

    @Column(length = 2048)
    private String canonicalUrl;

    private LocalDateTime lastCrawledAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private CategorySource categorySource;

    public Article() {

    }

    public Article(
            Long id,
            String title,
            String description,
            String content,
            Source source,
            String url,
            String imageUrl,
            LocalDateTime publishedAt,
            LocalDateTime crawledAt,
            Category category
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.content = content;
        this.source = source;
        this.url = url;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
        this.crawledAt = crawledAt;
        this.category = category;
    }

    public Long getId(){
        return id;
    } 

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(LocalDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public void setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    public LocalDateTime getLastCrawledAt() {
        return lastCrawledAt;
    }

    public void setLastCrawledAt(LocalDateTime lastCrawledAt) {
        this.lastCrawledAt = lastCrawledAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CategorySource getCategorySource() {
        return categorySource;
    }

    public void setCategorySource(CategorySource categorySource) {
        this.categorySource = categorySource;
    }
}
