package com.newsai.news_ai.article;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newsai.news_ai.crawler.ArticleUrlNormalizer;
import com.newsai.news_ai.category.Category;
import com.newsai.news_ai.error.ResourceNotFoundException;
import com.newsai.news_ai.category.CategoryRepository;
import com.newsai.news_ai.source.Source;
import com.newsai.news_ai.source.SourceRepository;

@Service 
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final SourceRepository sourceRepository;
    private final CategoryRepository categoryRepository;

    public ArticleService(
            ArticleRepository articleRepository,
            SourceRepository sourceRepository,
            CategoryRepository categoryRepository) {

        this.articleRepository = articleRepository;
        this.sourceRepository = sourceRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    };

    public Article getArticleById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
    }

    @Transactional
    public Article addArticle(Article article) {
        article.setId(null);
        article.setCrawledAt(LocalDateTime.now());

        if (article.getSource() != null && article.getSource().getId() != null) {
            Long sourceId = article.getSource().getId();

            Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source", sourceId));

            article.setSource(source);
        }

        if (article.getCategory() != null && article.getCategory().getId() != null) {
            Long categoryId = article.getCategory().getId();

            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

            article.setCategory(category);
            article.setCategorySource(CategorySource.MANUAL);
        }

        return articleRepository.save(article);
    }

    /**
     * Creates or refreshes an article from crawler data.
     *
     * <p>Identity is the {@code canonicalUrl} computed with
     * {@code ArticleUrlNormalizer.normalize}. Existing rows that predate
     * canonical URLs are matched by their raw {@code url} and backfilled with
     * a canonical URL during this refresh.
     *
     * <p>{@code crawledAt} is preserved across refreshes; {@code lastCrawledAt}
     * advances on every successful observation; {@code updatedAt} advances only
     * when a crawler-managed field (title, description, content, image URL,
     * published date, eligible category) actually changed. Unchanged articles
     * are not written again beyond {@code lastCrawledAt}. Categories owned by
     * {@code MANUAL} are never overwritten, and a feed that no longer provides
     * a category never clears an existing assignment.
     */
    

    @Transactional
    public ArticleCrawlOutcome upsertFromCrawl(Article candidate) {
        String originalUrl = candidate.getUrl();
        String canonical = ArticleUrlNormalizer.normalize(originalUrl);
        candidate.setCanonicalUrl(canonical);

        // 1. Look up by canonical URL first
        Article existing = articleRepository.findByCanonicalUrl(canonical).orElse(null);

        // 2. Legacy backfill: match by raw URL (for rows without canonicalUrl)
        if (existing == null) {
            existing = articleRepository.findByUrl(originalUrl).orElse(null);
        }

        // Backfill canonical URL on legacy rows that lacked one
        if (existing != null && existing.getCanonicalUrl() == null) {
            existing.setCanonicalUrl(canonical);
        }

        if (existing == null) {
            // Create new article
            candidate.setId(null);
            candidate.setCrawledAt(java.time.LocalDateTime.now());
            candidate.setLastCrawledAt(java.time.LocalDateTime.now());
            candidate.setUpdatedAt(java.time.LocalDateTime.now());

            resolveSourceAndCategory(candidate);
            Article saved = articleRepository.save(candidate);
            return new ArticleCrawlOutcome(ArticleCrawlStatus.CREATED, saved);
        }

        // Existing article â€” determine what changed
        boolean changed = false;

        if (!Objects.equals(existing.getTitle(), candidate.getTitle())) {
            existing.setTitle(candidate.getTitle());
            changed = true;
        }
        if (!Objects.equals(existing.getDescription(), candidate.getDescription())) {
            existing.setDescription(candidate.getDescription());
            changed = true;
        }
        if (!Objects.equals(existing.getContent(), candidate.getContent())) {
            existing.setContent(candidate.getContent());
            changed = true;
        }
        if (!Objects.equals(existing.getImageUrl(), candidate.getImageUrl())) {
            existing.setImageUrl(candidate.getImageUrl());
            changed = true;
        }
        if (!Objects.equals(existing.getPublishedAt(), candidate.getPublishedAt())) {
            existing.setPublishedAt(candidate.getPublishedAt());
            changed = true;
        }

        // Category: only update if crawler-owned and feed provides a category
        if (candidate.getCategory() != null) {
            if (existing.getCategorySource() == null
                    || existing.getCategorySource() == CategorySource.CRAWLER) {
                if (!Objects.equals(existing.getCategory(), candidate.getCategory())) {
                    existing.setCategory(candidate.getCategory());
                    existing.setCategorySource(CategorySource.CRAWLER);
                    changed = true;
                }
            }
            // MANUAL-owned categories are never overwritten by the crawler
        }

        // SubCategory: update if crawler provides one and existing is null or CRAWLER-owned
        // if (candidate.getSubCategory() != null) {
        //     if (existing.getCategorySource() == null
        //             || existing.getCategorySource() == CategorySource.CRAWLER) {
        //         if (!Objects.equals(existing.getSubCategory(), candidate.getSubCategory())) {
        //             existing.setSubCategory(candidate.getSubCategory());
        //             changed = true;
        //         }
        //     }
        // }

        existing.setLastCrawledAt(java.time.LocalDateTime.now());
        if (changed) {
            existing.setUpdatedAt(java.time.LocalDateTime.now());
        }

        Article saved = articleRepository.save(existing);
        ArticleCrawlStatus status = changed ? ArticleCrawlStatus.UPDATED : ArticleCrawlStatus.UNCHANGED;
        return new ArticleCrawlOutcome(status, saved);
    }

    private void resolveSourceAndCategory(Article article) {
        if (article.getSource() != null && article.getSource().getId() != null) {
            Long sourceId = article.getSource().getId();
            article.setSource(sourceRepository.findById(sourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Source", sourceId)));
        }
        if (article.getCategory() != null && article.getCategory().getId() != null) {
            Long categoryId = article.getCategory().getId();
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
            article.setCategory(category);
            article.setCategorySource(CategorySource.CRAWLER);
        }
    }

    /**
     * Re-attaches the crawler-supplied category inside the current transaction.
     * Returns {@code null} when it is absent or no longer exists, so callers can
     * decide whether a category is actually assignable.
     */
    private Category resolveCategory(Category candidate) {
        if (candidate == null || candidate.getId() == null) {
            return null;
        }
        return categoryRepository.findById(candidate.getId()).orElse(null);
    }

    @Transactional
    public Article updateArticle(Long id, Article updatedArticle) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));

        article.setTitle(updatedArticle.getTitle());
        article.setDescription(updatedArticle.getDescription());
        article.setContent(updatedArticle.getContent());

        if (updatedArticle.getSource() != null && updatedArticle.getSource().getId() != null) {
            Long sourceId = updatedArticle.getSource().getId();

            Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source", sourceId));

            article.setSource(source);
        }

        if (updatedArticle.getCategory() != null && updatedArticle.getCategory().getId() != null) {
            Long categoryId = updatedArticle.getCategory().getId();

            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

            article.setCategory(category);
            article.setCategorySource(CategorySource.MANUAL);
        }

        article.setUrl(updatedArticle.getUrl());
        article.setImageUrl(updatedArticle.getImageUrl());
        article.setPublishedAt(updatedArticle.getPublishedAt());

        return articleRepository.save(article);
    }

    /**
     * Kiểm tra bài viết đã tồn tại theo canonical URL (dùng bởi crawler).
     * Dùng SELECT EXISTS — không load toàn bộ entity.
     */
    public boolean existsByCanonicalUrl(String canonicalUrl) {
        return articleRepository.existsByCanonicalUrl(canonicalUrl);
    }

    public void deleteArticle(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article", id);
        }

        articleRepository.deleteById(id);
    }

    public List<Article> getArticlesBySourceId(Long sourceId) {
        return articleRepository.findBySourceId(sourceId);
    }

    public List<Article> getArticlesByCategoryId(Long categoryId) {
        return articleRepository.findByCategoryId(categoryId);
    }

    public Article getArticleByUrl(String url) {
        return articleRepository.findByUrl(url)
                .orElseThrow(() -> new ResourceNotFoundException("Article with url: " + url));
    }

    /**
     * Searches articles with optional filters and returns a bounded page of
     * summary data. Full article content remains available from the article
     * detail endpoint instead of being repeated for every search result.
     */
    public ArticleSearchResponse searchArticles(
            String keyword,
            Long sourceId,
            Long categoryId,
            String publishedFrom,
            String publishedTo,
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0.");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("size must be between 1 and 50.");
        }

        LocalDateTime from = parseDateParam(publishedFrom, true);
        LocalDateTime to = parseDateParam(publishedTo, false);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("publishedFrom must be before publishedTo.");
        }

        Page<Article> results = articleRepository.findAll(
                searchSpecification(normalizeKeyword(keyword), sourceId, categoryId, from, to),
                PageRequest.of(page, size,
                        Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))));

        return new ArticleSearchResponse(
                results.getContent().stream().map(this::toSearchItem).toList(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    private ArticleSearchItem toSearchItem(Article article) {
        ArticleSearchItem.Reference source = new ArticleSearchItem.Reference(
                article.getSource().getId(),
                article.getSource().getName());

        ArticleSearchItem.Reference category = article.getCategory() == null
                ? null
                : new ArticleSearchItem.Reference(
                        article.getCategory().getId(),
                        article.getCategory().getName());

        return new ArticleSearchItem(
                article.getId(),
                article.getTitle(),
                article.getDescription(),
                article.getImageUrl(),
                article.getPublishedAt(),
                source,
                category);
    }

    /**
     * Builds only the predicates requested by the caller. This avoids the
     * {@code :parameter IS NULL OR ...} JPQL pattern, for which PostgreSQL
     * cannot infer a type when an optional parameter is null.
     */
    private Specification<Article> searchSpecification(
            String keyword,
            Long sourceId,
            Long categoryId,
            LocalDateTime publishedFrom,
            LocalDateTime publishedTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null) {
                String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), pattern, '\\')));
            }
            if (sourceId != null) {
                predicates.add(criteriaBuilder.equal(root.get("source").get("id"), sourceId));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (publishedFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("publishedAt"), publishedFrom));
            }
            if (publishedTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("publishedAt"), publishedTo));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Blank / null keyword becomes {@code null} meaning "match all".
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Accepts either a date ({@code yyyy-MM-dd}) or a date-time in ISO format
     * ({@code yyyy-MM-ddTHH:mm[:ss]}). For plain dates, {@code startOfDay=true}
     * (the {@code from} bound) means 00:00 and {@code startOfDay=false} (the
     * {@code to} bound) means 23:59:59, so a single date covers the whole day.
     */
    private LocalDateTime parseDateParam(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            LocalDate date = LocalDate.parse(trimmed);
            return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        }

        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid date '" + value + "'. Use 'yyyy-MM-dd' or 'yyyy-MM-ddTHH:mm[:ss]'.");
        }
    }
}
