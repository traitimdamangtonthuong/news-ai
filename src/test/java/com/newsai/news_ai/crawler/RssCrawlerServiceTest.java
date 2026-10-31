package com.newsai.news_ai.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.newsai.news_ai.article.Article;
import com.newsai.news_ai.article.ArticleCrawlOutcome;
import com.newsai.news_ai.article.ArticleCrawlStatus;
import com.newsai.news_ai.article.ArticleService;
import com.newsai.news_ai.category.Category;
import com.newsai.news_ai.category.CategoryRepository;
import com.newsai.news_ai.error.ResourceNotFoundException;
import com.newsai.news_ai.source.Source;
import com.newsai.news_ai.source.SourceRepository;
import com.newsai.news_ai.crawler.RssCategoryResolver;

/**
 * Crawler orchestration tests. The persistence/refresh semantics live in
 * {@link ArticleService#upsertFromCrawl(Article)} and are covered separately;
 * here {@code articleService} is mocked so only the crawler's per-item
 * routing, counting, category lookup, and error isolation are exercised.
 */
class RssCrawlerServiceTest {

    private SourceRepository sourceRepository;
    private ArticleService articleService;
    private CategoryRepository categoryRepository;
    private RssFeedFetcher feedFetcher;
    private FeedItemMapper feedItemMapper;
    private CrawlerProperties properties;
    private RssCategoryResolver rssCategoryResolver;

    private RssCrawlerService service;
private static final String FEED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Test Feed</title>
                <link>https://example.com</link>
                <description>desc</description>
                <item>
                  <title>Existing item</title>
                  <link>https://example.com/1?utm_source=x</link>
                  <category>Technology</category>
                </item>
                <item>
                  <title>In-run duplicate (same normalized URL)</title>
                  <link>https://example.com/1?fbclid=abc</link>
                </item>
                <item>
                  <title>Brand new</title>
                  <link>https://example.com/3</link>
                  <category>Technology</category>
                </item>
              </channel>
            </rss>
            """;

    @BeforeEach
    void setUp() {
        sourceRepository = mock(SourceRepository.class);
        articleService = mock(ArticleService.class);
        categoryRepository = mock(CategoryRepository.class);
        feedFetcher = mock(RssFeedFetcher.class);
        feedItemMapper = new FeedItemMapper();
        properties = new CrawlerProperties();
        rssCategoryResolver = mock(RssCategoryResolver.class);

        service = new RssCrawlerService(
                sourceRepository, articleService, categoryRepository,
                feedFetcher, feedItemMapper, properties, rssCategoryResolver);
    }

    @Test
    void firstCrawlCountsCreatedAndBuildsCanonicalCandidate() throws Exception {
        stubFeed();
        when(categoryRepository.findByName("Technology")).thenReturn(Optional.of(category()));
        when(articleService.upsertFromCrawl(any())).thenAnswer(invocation -> {
            Article candidate = invocation.getArgument(0);
            return new ArticleCrawlOutcome(ArticleCrawlStatus.CREATED, candidate);
        });

        CrawlResult result = service.crawlSource(1L);

        assertEquals(3, result.fetched());
        assertEquals(2, result.created());
        assertEquals(0, result.updated());
        assertEquals(1, result.unchanged()); // item 2 normalized to item 1's URL
        assertEquals(0, result.failed());

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleService, times(2)).upsertFromCrawl(captor.capture());
        List<Article> candidates = captor.getAllValues();

        // Raw publisher URL is kept; canonical identity is the normalized URL.
        assertEquals("https://example.com/1?utm_source=x", candidates.get(0).getUrl());
        assertEquals("https://example.com/1", candidates.get(0).getCanonicalUrl());
        assertEquals("https://example.com/3", candidates.get(1).getUrl());
        assertEquals("https://example.com/3", candidates.get(1).getCanonicalUrl());

        // A feed category that matches a canonical DB category lands on the candidate.
        assertEquals(2L, candidates.get(1).getCategory().getId());
        assertEquals("Technology", candidates.get(1).getCategory().getName());
        // Timestamps are the service layer's job, not the crawler's.
        assertNull(candidates.get(0).getCrawledAt());
    }

    @Test
    void secondIdenticalCrawlCountsUnchanged() throws Exception {
        stubFeed();
        when(articleService.upsertFromCrawl(any())).thenAnswer(invocation -> {
            Article candidate = invocation.getArgument(0);
            return new ArticleCrawlOutcome(ArticleCrawlStatus.UNCHANGED, candidate);
        });

        CrawlResult result = service.crawlSource(1L);

        assertEquals(0, result.created());
        assertEquals(0, result.updated());
        assertEquals(3, result.unchanged());
    }

    @Test
    void changedContentCountsUpdated() throws Exception {
        stubFeed();
        when(articleService.upsertFromCrawl(any())).thenAnswer(invocation -> {
            Article candidate = invocation.getArgument(0);
            return new ArticleCrawlOutcome(ArticleCrawlStatus.UPDATED, candidate);
        });

        CrawlResult result = service.crawlSource(1L);

        assertEquals(2, result.updated());
        assertEquals(1, result.unchanged());
    }

    @Test
    void missingCategoryMappingLeavesCandidateWithoutCategory() throws Exception {
        stubFeed();
        when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(articleService.upsertFromCrawl(any())).thenAnswer(invocation -> {
            Article candidate = invocation.getArgument(0);
            return new ArticleCrawlOutcome(ArticleCrawlStatus.CREATED, candidate);
        });

        service.crawlSource(1L);

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleService, times(2)).upsertFromCrawl(captor.capture());
        assertNull(captor.getAllValues().get(1).getCategory());
    }

    @Test
    void concurrentCreateRaceIsRetriedAsExisting() throws Exception {
        stubFeed();
        // First item hits a unique-constraint race, retry succeeds as unchanged.
        when(articleService.upsertFromCrawl(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"))
                .thenAnswer(invocation -> {
                    Article candidate = invocation.getArgument(0);
                    return new ArticleCrawlOutcome(ArticleCrawlStatus.UNCHANGED, candidate);
                });

        CrawlResult result = service.crawlSource(1L);

        assertEquals(0, result.failed());
        assertEquals(3, result.unchanged());
    }

    @Test
    void oneBadItemDoesNotAbortTheRestOfTheFeed() throws Exception {
        stubFeed();
        when(articleService.upsertFromCrawl(any()))
                .thenThrow(new IllegalStateException("boom"))
                .thenAnswer(invocation -> {
                    Article candidate = invocation.getArgument(0);
                    return new ArticleCrawlOutcome(ArticleCrawlStatus.UPDATED, candidate);
                });

        CrawlResult result = service.crawlSource(1L);

        assertEquals(1, result.failed());
        assertEquals(1, result.updated());
        assertEquals(1, result.unchanged());
        assertEquals(1, result.errors().size());
        assertEquals("Failed to save item 'Existing item' (https://example.com/1?utm_source=x): boom",
                result.errors().get(0));
    }

    @Test
    void sourceWithoutRssUrlProducesErrorResult() {
        when(sourceRepository.findById(7L))
                .thenReturn(Optional.of(new Source(7L, "No Feed", "https://example.com", null)));

        CrawlResult result = service.crawlSource(7L);

        assertEquals(0, result.fetched());
        assertEquals(1, result.errors().size());
        verify(articleService, never()).upsertFromCrawl(any());
    }

    @Test
    void fetchFailureIsReportedInsteadOfThrown() throws Exception {
        when(sourceRepository.findById(1L)).thenReturn(Optional.of(source()));
        when(feedFetcher.fetch(anyString()))
                .thenThrow(new RssCrawlException("Failed to fetch feed 'x': HTTP 500"));

        CrawlResult result = service.crawlSource(1L);

        assertEquals(0, result.fetched());
        assertEquals(0, result.failed());
        assertEquals(1, result.errors().size());
        assertEquals("Failed to fetch feed 'x': HTTP 500", result.errors().get(0));
    }

    @Test
    void unknownSourceThrowsNotFound() {
        when(sourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.crawlSource(999L));
    }

    @Test
    void crawlAllAggregatesEverySource() throws Exception {
        when(sourceRepository.findAll()).thenReturn(List.of(source(), noFeedSource()));
        when(feedFetcher.fetch(anyString())).thenReturn(
                new ByteArrayInputStream(FEED.getBytes(StandardCharsets.UTF_8)));
        when(articleService.upsertFromCrawl(any())).thenAnswer(invocation -> {
            Article candidate = invocation.getArgument(0);
            return new ArticleCrawlOutcome(ArticleCrawlStatus.CREATED, candidate);
        });

        List<CrawlResult> results = service.crawlAll();

        assertEquals(2, results.size());
        CrawlResult first = results.get(0);
        // Items 1 and 3 created; item 2 is an in-run canonical duplicate.
        assertEquals(3, first.fetched());
        assertEquals(2, first.created());
        assertEquals(1, first.unchanged());
        CrawlResult second = results.get(1);
        assertEquals(0, second.fetched());
        assertEquals(1, second.errors().size());
    }

    private void stubFeed() throws Exception {
        when(sourceRepository.findById(1L)).thenReturn(Optional.of(source()));
        when(feedFetcher.fetch(anyString())).thenReturn(
                new ByteArrayInputStream(FEED.getBytes(StandardCharsets.UTF_8)));
    }

    private Source source() {
        return new Source(1L, "Test Source", "https://example.com", "https://example.com/feed.rss");
    }

    private Source noFeedSource() {
        return new Source(7L, "No Feed", "https://example.com", null);
    }

    private Category category() {
        return new Category(2L, "Technology");
    }
}