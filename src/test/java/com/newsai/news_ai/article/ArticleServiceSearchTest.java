package com.newsai.news_ai.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.newsai.news_ai.category.Category;
import com.newsai.news_ai.category.CategoryRepository;
import com.newsai.news_ai.source.Source;
import com.newsai.news_ai.source.SourceRepository;

class ArticleServiceSearchTest {

    private ArticleRepository articleRepository;
    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleRepository = mock(ArticleRepository.class);
        articleService = new ArticleService(
                articleRepository,
                mock(SourceRepository.class),
                mock(CategoryRepository.class));
    }

    @Test
    void searchReturnsPagedArticleSummariesSortedByNewestFirst() {
        Article article = new Article();
        article.setId(4L);
        article.setTitle("AI in Vietnam");
        article.setDescription("Summary");
        article.setImageUrl("https://example.com/image.jpg");
        article.setPublishedAt(LocalDateTime.of(2026, 9, 23, 10, 0));
        article.setSource(new Source(2L, "VnExpress", "https://vnexpress.net", null));
        article.setCategory(new Category(3L, "Technology"));

        when(articleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(article), Pageable.ofSize(20), 41));

        ArticleSearchResponse response = articleService.searchArticles(
                "  AI  ", 2L, 3L, "2026-09-01", "2026-09-30", 0, 20);

        assertEquals(1, response.items().size());
        assertEquals(41, response.totalElements());
        assertEquals("VnExpress", response.items().getFirst().source().name());
        assertEquals("Technology", response.items().getFirst().category().name());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(articleRepository).findAll(any(Specification.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals("publishedAt: DESC,id: DESC", pageable.getValue().getSort().toString());
    }

    @Test
    void searchRejectsInvalidPaginationAndDateRangesBeforeQuerying() {
        assertThrows(IllegalArgumentException.class,
                () -> articleService.searchArticles(null, null, null, null, null, -1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> articleService.searchArticles(null, null, null, null, null, 0, 51));
        assertThrows(IllegalArgumentException.class,
                () -> articleService.searchArticles(
                        null, null, null, "2026-09-30", "2026-09-01", 0, 20));

        verifyNoInteractions(articleRepository);
    }

    @Test
    void searchAllowsArticlesWithoutCategories() {
        Article article = new Article();
        article.setId(9L);
        article.setTitle("Uncategorized article");
        article.setSource(new Source(2L, "VnExpress", "https://vnexpress.net", null));

        when(articleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(article)));

        ArticleSearchResponse response = articleService.searchArticles(
                " ", null, null, null, null, 0, 20);

        assertNull(response.items().getFirst().category());
    }
}
