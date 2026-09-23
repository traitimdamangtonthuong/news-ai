package com.newsai.news_ai.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.newsai.news_ai.category.Category;
import com.newsai.news_ai.category.CategoryRepository;
import com.newsai.news_ai.crawler.ArticleUrlNormalizer;
import com.newsai.news_ai.source.Source;
import com.newsai.news_ai.source.SourceRepository;

/**
 * Exercises {@link ArticleService#upsertFromCrawl(Article)} against an
 * in-memory {@link ArticleRepository} fake: creation, unchanged/update
 * detection, canonical-URL identity, legacy raw-URL backfill, and the category
 * ownership policy. No external database is required.
 */
class ArticleServiceCrawlUpsertTest {

    private final Map<Long, Article> store = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    private ArticleRepository articleRepository;
    private SourceRepository sourceRepository;
    private CategoryRepository categoryRepository;
    private ArticleService articleService;

    private Source source;
    private Category technology;
    private Category world;

    @BeforeEach
    void setUp() {
        articleRepository = mock(ArticleRepository.class);
        sourceRepository = mock(SourceRepository.class);
        categoryRepository = mock(CategoryRepository.class);

        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> save(inv.getArgument(0)));
        when(articleRepository.saveAndFlush(any(Article.class))).thenAnswer(inv -> save(inv.getArgument(0)));
        when(articleRepository.findById(anyLong())).thenAnswer(inv ->
                Optional.ofNullable(store.get(inv.getArgument(0))));
        when(articleRepository.findByCanonicalUrl(anyString())).thenAnswer(inv ->
                firstBy(a -> Objects.equals(a.getCanonicalUrl(), inv.getArgument(0))));
        when(articleRepository.findByUrl(anyString())).thenAnswer(inv ->
                firstBy(a -> Objects.equals(a.getUrl(), inv.getArgument(0))));
        when(articleRepository.count()).thenAnswer(inv -> (long) store.size());

        source = new Source(99L, "Test Source", "https://example.com", "https://example.com/feed.rss");
        when(sourceRepository.findById(99L)).thenReturn(Optional.of(source));

        technology = new Category(1L, "Technology");
        world = new Category(2L, "World");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(technology));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(world));

        articleService = new ArticleService(articleRepository, sourceRepository, categoryRepository);
    }

    private Article save(Article article) {
        if (article.getId() == null) {
            article.setId(idSeq.getAndIncrement());
        }
        store.put(article.getId(), article);
        return article;
    }

    private Optional<Article> firstBy(Predicate<Article> predicate) {
        return store.values().stream().filter(predicate).findFirst();
    }

    private Article candidate(String title, String url) {
        Article article = new Article();
        article.setTitle(title);
        article.setUrl(url);
        article.setCanonicalUrl(ArticleUrlNormalizer.normalize(url));
        article.setDescription("default desc");
        article.setContent("default content");
        article.setImageUrl("https://example.com/img.jpg");
        article.setPublishedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        article.setSource(source);
        return article;
    }

    @Test
    void firstCrawlCreatesArticleWithTimestampsAndCanonicalUrl() {
        ArticleCrawlOutcome outcome =
                articleService.upsertFromCrawl(candidate("Title one", "https://example.com/a?utm_source=x"));

        assertEquals(ArticleCrawlStatus.CREATED, outcome.status());
        Article saved = outcome.article();
        assertNotNull(saved.getId());
        assertNotNull(saved.getCrawledAt());
        assertNotNull(saved.getLastCrawledAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("https://example.com/a", saved.getCanonicalUrl());
        // Original publisher URL is preserved, not replaced by the canonical variant.
        assertEquals("https://example.com/a?utm_source=x", saved.getUrl());
        assertNull(saved.getCategory());
        assertNull(saved.getCategorySource());
    }

    @Test
    void identicalRecrawlIsUnchangedPreservesContentAndAdvancesLastCrawledAt()
            throws Exception {
        ArticleCrawlOutcome first =
                articleService.upsertFromCrawl(candidate("Stable title", "https://example.com/b"));
        LocalDateTime firstCrawledAt = first.article().getCrawledAt();
        LocalDateTime firstUpdatedAt = first.article().getUpdatedAt();
        LocalDateTime firstLastCrawledAt = first.article().getLastCrawledAt();
        Thread.sleep(10);

        ArticleCrawlOutcome second =
                articleService.upsertFromCrawl(candidate("Stable title", "https://example.com/b"));

        assertEquals(ArticleCrawlStatus.UNCHANGED, second.status());
        Article fresh = first.article();
        assertEquals("Stable title", fresh.getTitle());
        // First-seen and content-change timestamps are untouched.
        assertEquals(firstCrawledAt, fresh.getCrawledAt());
        assertEquals(firstUpdatedAt, fresh.getUpdatedAt());
        // But every observation advances the last-seen timestamp.
        assertTrue(fresh.getLastCrawledAt().isAfter(firstLastCrawledAt));
    }

    @Test
    void changedContentProducesUpdatedAndAdvancesUpdatedAt() throws Exception {
        ArticleCrawlOutcome first =
                articleService.upsertFromCrawl(candidate("Old title", "https://example.com/c"));
        LocalDateTime firstCrawledAt = first.article().getCrawledAt();
        LocalDateTime firstUpdatedAt = first.article().getUpdatedAt();
        Thread.sleep(10);

        ArticleCrawlOutcome second =
                articleService.upsertFromCrawl(candidate("New title", "https://example.com/c"));

        assertEquals(ArticleCrawlStatus.UPDATED, second.status());
        Article fresh = first.article();
        assertEquals("New title", fresh.getTitle());
        assertEquals(firstCrawledAt, fresh.getCrawledAt());
        assertTrue(fresh.getUpdatedAt().isAfter(firstUpdatedAt));
    }

    @Test
    void trackingParameterVariantsMatchSameCanonicalArticleAcrossRuns() {
        ArticleCrawlOutcome first = articleService.upsertFromCrawl(
                candidate("Tracking", "https://example.com/a?utm_source=x"));

        ArticleCrawlOutcome second = articleService.upsertFromCrawl(
                candidate("Tracking", "https://example.com/a?utm_campaign=summer"));

        assertEquals(ArticleCrawlStatus.UNCHANGED, second.status());
        assertEquals(1, articleRepository.count());
        Article fresh = first.article();
        // The original URL is retained even though the feed URL changed.
        assertEquals("https://example.com/a?utm_source=x", fresh.getUrl());
        assertEquals("https://example.com/a", fresh.getCanonicalUrl());
    }

    @Test
    void legacyRawUrlMatchIsRefreshedAndBackfilledWithCanonicalUrl() {
        // A row created by the old pipeline has no canonical URL yet.
        Article legacy = new Article();
        legacy.setTitle("Legacy");
        legacy.setUrl("https://example.com/legacy");
        legacy.setDescription("old desc");
        legacy.setContent("old content");
        legacy.setSource(source);
        legacy.setPublishedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        legacy.setCrawledAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        save(legacy);

        Article updatedCandidate = candidate("Legacy", "https://example.com/legacy");
        updatedCandidate.setDescription("refreshed desc");

        ArticleCrawlOutcome outcome = articleService.upsertFromCrawl(updatedCandidate);

        assertEquals(ArticleCrawlStatus.UPDATED, outcome.status());
        Article fresh = firstBy(a -> Objects.equals(a.getCanonicalUrl(), "https://example.com/legacy"))
                .orElseThrow();
        assertEquals("https://example.com/legacy", fresh.getUrl());
        assertEquals("refreshed desc", fresh.getDescription());
        // First-seen timestamp survives the refresh.
        assertEquals(legacy.getCrawledAt(), fresh.getCrawledAt());
        assertEquals(1, articleRepository.count());
    }

    @Test
    void crawlerOwnedCategoryIsUpdatedOnRecrawl() {
        Article firstCandidate = candidate("Category item", "https://example.com/cat");
        firstCandidate.setCategory(technology);

        ArticleCrawlOutcome first = articleService.upsertFromCrawl(firstCandidate);
        assertEquals(ArticleCrawlStatus.CREATED, first.status());
        assertEquals(CategorySource.CRAWLER, first.article().getCategorySource());
        assertEquals("Technology", first.article().getCategory().getName());

        Article secondCandidate = candidate("Category item", "https://example.com/cat");
        secondCandidate.setCategory(world);

        ArticleCrawlOutcome second = articleService.upsertFromCrawl(secondCandidate);
        assertEquals(ArticleCrawlStatus.UPDATED, second.status());

        assertEquals("World", first.article().getCategory().getName());
        assertEquals(CategorySource.CRAWLER, first.article().getCategorySource());
    }

    @Test
    void missingFeedCategoryDoesNotEraseExistingCategory() {
        Article firstCandidate = candidate("Category item", "https://example.com/cat");
        firstCandidate.setCategory(technology);
        ArticleCrawlOutcome first = articleService.upsertFromCrawl(firstCandidate);

        // Latest feed has no category (or no mapping) — the existing one stays.
        ArticleCrawlOutcome second =
                articleService.upsertFromCrawl(candidate("Category item", "https://example.com/cat"));

        assertEquals(ArticleCrawlStatus.UNCHANGED, second.status());
        assertEquals("Technology", first.article().getCategory().getName());
        assertEquals(CategorySource.CRAWLER, first.article().getCategorySource());
    }

    @Test
    void manualCategoryIsNeverOverwrittenByCrawler() {
        // Admin creates the article with a category -> MANUAL ownership.
        Article manual = candidate("Manual item", "https://example.com/manual");
        manual.setCategory(technology);
        Article created = articleService.addArticle(manual);
        assertEquals(CategorySource.MANUAL, created.getCategorySource());

        // Crawler brings a different category — it must be ignored.
        Article crawlCandidate = candidate("Manual item", "https://example.com/manual");
        crawlCandidate.setCategory(world);
        ArticleCrawlOutcome outcome = articleService.upsertFromCrawl(crawlCandidate);

        assertEquals(ArticleCrawlStatus.UNCHANGED, outcome.status());
        assertEquals("Technology", created.getCategory().getName());
        assertEquals(CategorySource.MANUAL, created.getCategorySource());
    }
}