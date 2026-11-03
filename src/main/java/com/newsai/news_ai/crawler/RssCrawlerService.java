package com.newsai.news_ai.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.newsai.news_ai.article.Article;
import com.newsai.news_ai.article.ArticleCrawlOutcome;
import com.newsai.news_ai.article.ArticleCrawlStatus;
import com.newsai.news_ai.article.ArticleService;
import com.newsai.news_ai.category.Category;
import com.newsai.news_ai.category.CategoryRepository;
import com.newsai.news_ai.error.ResourceNotFoundException;
import com.newsai.news_ai.source.Source;
import com.newsai.news_ai.source.SourceRepository;

/**
 * Orchestrates one crawl: fetch feed → parse → create/update/unchanged.
 * Per-item failures are collected and reported instead of aborting the run.
 * Identity and refresh semantics live in {@link ArticleService#upsertFromCrawl}.
 *
 * <p>For genuinely NEW articles, also fires {@link ArticlePageScraper} to read
 * the breadcrumb and assign a subcategory (e.g. "AI" under "Khoa học công nghệ").
 * This extra HTTP call is skipped for already-existing articles to avoid
 * performance impact.
 */
@Service
public class RssCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(RssCrawlerService.class);

    private final SourceRepository sourceRepository;
    private final ArticleService articleService;
    private final CategoryRepository categoryRepository;
    private final RssFeedFetcher feedFetcher;
    private final FeedItemMapper feedItemMapper;
    private final CrawlerProperties properties;
    private final RssCategoryResolver rssCategoryResolver;
    private final ArticlePageScraper articlePageScraper;
    @org.springframework.beans.factory.annotation.Autowired
    public RssCrawlerService(
            SourceRepository sourceRepository,
            ArticleService articleService,
            CategoryRepository categoryRepository,
            RssFeedFetcher feedFetcher,
            FeedItemMapper feedItemMapper,
            CrawlerProperties properties,
            RssCategoryResolver rssCategoryResolver,
            ArticlePageScraper articlePageScraper) {
        this.sourceRepository = sourceRepository;
        this.articleService = articleService;
        this.categoryRepository = categoryRepository;
        this.feedFetcher = feedFetcher;
        this.feedItemMapper = feedItemMapper;
        this.properties = properties;
        this.rssCategoryResolver = rssCategoryResolver;
        this.articlePageScraper = articlePageScraper;
    }

    /** Backward-compat constructor (for tests that don't need scraper). */
    public RssCrawlerService(
            SourceRepository sourceRepository,
            ArticleService articleService,
            CategoryRepository categoryRepository,
            RssFeedFetcher feedFetcher,
            FeedItemMapper feedItemMapper,
            CrawlerProperties properties,
            RssCategoryResolver rssCategoryResolver) {
        this(sourceRepository, articleService, categoryRepository,
             feedFetcher, feedItemMapper, properties,
             rssCategoryResolver, new ArticlePageScraper());
    }

    /**
     * Crawls every configured source. NOT wrapped in one big transaction:
     * each item is saved in its own transaction (via ArticleService.upsertFromCrawl)
     * so a failing item rolls back only itself instead of poisoning the whole
     * run (which would otherwise make the commit throw UnexpectedRollbackException).
     */
    public List<CrawlResult> crawlAll() {
        return sourceRepository.findAll().stream()
                .map(this::crawlSource)
                .toList();
    }

    public CrawlResult crawlSource(Long sourceId) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source", sourceId));
        return crawlSource(source);
    }

    private CrawlResult crawlSource(Source source) {
        String sourceName = source.getName();

        if (source.getRssUrl() == null || source.getRssUrl().isBlank()) {
            return new CrawlResult(source.getId(), sourceName, 0, 0, 0, 0, 0,
                    List.of("Source '" + sourceName + "' has no RSS URL configured."));
        }

        FeedParseResult parsed;
        try (InputStream input = feedFetcher.fetch(source.getRssUrl())) {
            parsed = feedItemMapper.parse(input, source);
        } catch (RssCrawlException e) {
            return new CrawlResult(source.getId(), sourceName, 0, 0, 0, 0, 0,
                    List.of(e.getMessage()));
        } catch (IOException e) {
            return new CrawlResult(source.getId(), sourceName, 0, 0, 0, 0, 0,
                    List.of("Failed to read feed '" + source.getRssUrl() + "': " + e.getMessage()));
        }

        List<RssFeedItem> items = parsed.items();
        if (items.size() > properties.getMaxItemsPerFeed()) {
            items = items.subList(0, properties.getMaxItemsPerFeed());
        }

        Counters counters = new Counters();
        List<String> errors = new ArrayList<>();

        if (parsed.skippedMalformed() > 0) {
            errors.add("Skipped " + parsed.skippedMalformed()
                    + " entries missing required title/link.");
        }

        // In-run guard only: the persistent canonicalUrl lookup in
        // ArticleService is the cross-run duplicate mechanism. This set stops
        // the same canonical URL from being written twice within one run.
        Set<String> seenCanonical = new HashSet<>();

        for (RssFeedItem item : items) {
            String canonical = ArticleUrlNormalizer.normalize(item.url());
            if (!seenCanonical.add(canonical)) {
                counters.unchanged++;
                continue;
            }
            upsertItem(item, source, counters, errors);
        }

        return new CrawlResult(source.getId(), sourceName, items.size(),
                counters.created, counters.updated, counters.unchanged, counters.failed, errors);
    }

    private void upsertItem(RssFeedItem item, Source source, Counters counters, List<String> errors) {
        try {
            applyOutcome(articleService.upsertFromCrawl(toArticle(item, source)), counters);
        } catch (DataIntegrityViolationException e) {
            // Concurrent create/backfill race: another process just wrote this
            // canonical URL. Re-read it and process the item as existing.
            try {
                applyOutcome(articleService.upsertFromCrawl(toArticle(item, source)), counters);
            } catch (Exception e2) {
                counters.failed++;
                errors.add("Failed to save item '" + item.title() + "' (" + item.url() + "): "
                        + e2.getMessage());
            }
        } catch (Exception e) {
            counters.failed++;
            errors.add("Failed to save item '" + item.title() + "' (" + item.url() + "): "
                    + e.getMessage());
        }
    }

    private void applyOutcome(ArticleCrawlOutcome outcome, Counters counters) {
        switch (outcome.status()) {
            case CREATED -> counters.created++;
            case UPDATED -> counters.updated++;
            case UNCHANGED -> counters.unchanged++;
        }
    }

    private Article toArticle(RssFeedItem item, Source source) {
        Article article = new Article();

        // --- Bước 1: Thử lấy category từ XML <category> tags (Nhân Dân, ...) ---
        Category category = null;
        if (item.categories() != null) {
            for (String categoryName : item.categories()) {
                category = categoryRepository
                        .findByName(categoryName)
                        .orElse(null);
                if (category != null) {
                    break;
                }
            }
        }

        // --- Bước 2: Fallback — đoán category từ slug RSS URL ---
        if (category == null && source.getRssUrl() != null) {
            String resolvedCategoryName = rssCategoryResolver.resolveCategoryName(source.getRssUrl());
            if (resolvedCategoryName != null) {
                category = categoryRepository
                        .findByName(resolvedCategoryName)
                        .orElseGet(() -> categoryRepository.save(new Category(null, resolvedCategoryName)));
            }
        }

        // --- Bước 3: Scrape breadcrumb từ trang bài viết để lấy subcategory ---
        // Chỉ gọi khi article là MỚI (chưa có trong DB).
        // Kiểm tra bằng canonical URL trước khi gọi Jsoup.
        String canonical = ArticleUrlNormalizer.normalize(item.url());
        boolean isNew = !articleService.existsByCanonicalUrl(canonical);
        
        // Mặc định lấy tóm tắt từ RSS nếu chưa có nội dung chi tiết
        String fullContent = item.content(); 

        if (isNew) {
            ArticleScrapeResult scrapeResult = articlePageScraper.scrape(item.url());
            BreadcrumbResult breadcrumb = scrapeResult.breadcrumb();

            // Xử lý gán danh mục (Category) từ breadcrumb
            if (breadcrumb.hasData()) {
                Category currentParent = category;
                for (BreadcrumbResult.BreadcrumbNode node : breadcrumb.chain()) {
                    Category cat = findOrCreateCategory(node.name(), node.slug(), currentParent);
                    if (cat != null) {
                        currentParent = cat;
                    }
                }
                if (currentParent != null) {
                    category = currentParent;
                }
            }

            // Ghi đè content lấy từ RSS bằng nội dung chi tiết (HTML) cào được
            if (scrapeResult.content() != null && !scrapeResult.content().isBlank()) {
                fullContent = scrapeResult.content();
            }
        }
        String cleanTitle = HtmlUtils.htmlUnescape(item.title());
        String cleanDescription = HtmlUtils.htmlUnescape(item.description());

        article.setTitle(cleanTitle);
        article.setDescription(cleanDescription);
        article.setContent(fullContent); // <--- Đã được lấp đầy bằng nội dung chi tiết
        article.setSource(source);
        article.setCategory(category); 
        article.setUrl(item.url());
        article.setCanonicalUrl(canonical);
        article.setImageUrl(item.imageUrl());
        article.setPublishedAt(item.publishedAt());
        return article;
    }

    /**
     * Tìm category theo tên (và parent nếu là subcategory), tự tạo nếu chưa có.
     * Thread-safe: nếu race condition xảy ra, findByName sẽ tìm lại.
     */
    private Category findOrCreateCategory(String name, String slug, Category parent) {
        if (name == null || name.isBlank()) return null;

        // Tìm theo parent context
        if (parent != null) {
            return categoryRepository.findByNameAndParent(name, parent)
                    .orElseGet(() -> {
                        Category sub = new Category(null, name, slug, parent);
                        try {
                            return categoryRepository.save(sub);
                        } catch (DataIntegrityViolationException e) {
                            // Race condition: another thread just created it
                            return categoryRepository.findByNameAndParent(name, parent)
                                    .orElseGet(() -> categoryRepository.findByName(name).orElse(null));
                        }
                    });
        }

        // Top-level category
        return categoryRepository.findByName(name)
                .orElseGet(() -> {
                    Category cat = new Category(null, name, slug, null);
                    try {
                        return categoryRepository.save(cat);
                    } catch (DataIntegrityViolationException e) {
                        return categoryRepository.findByName(name).orElse(null);
                    }
                });
    }

    /** Per-item outcome counters for one crawled source. */
    private static final class Counters {
        int created;
        int updated;
        int unchanged;
        int failed;
    }
}
