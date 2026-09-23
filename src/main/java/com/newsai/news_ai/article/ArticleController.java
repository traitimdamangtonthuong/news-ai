package com.newsai.news_ai.article;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController 
@RequestMapping("/api/articles") // đặt URL cơ sở cho controller.
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public List<Article> getALlArticles() {
        return articleService.getAllArticles(); 
    }

    // GET /api/articles/search?keyword=AI&sourceId=1&categoryId=2&publishedFrom=2026-01-01&publishedTo=2026-01-31&page=0&size=20
    @GetMapping("/search")
    public ArticleSearchResponse searchArticles(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long sourceId,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String publishedFrom,
        @RequestParam(required = false) String publishedTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return articleService.searchArticles(
                keyword, sourceId, categoryId, publishedFrom, publishedTo, page, size);
    }

    @GetMapping("/{id}")
    public Article getArticleById(@PathVariable Long id) {
        return articleService.getArticleById(id);
    }

    @GetMapping("/source/{sourceId}")
    public List<Article> getArticlesBySourceId(
        @PathVariable Long sourceId
    ) {
        return articleService.getArticlesBySourceId(sourceId);
    }

    @GetMapping("/category/{categoryId}")
    public List<Article> getArticlesByCategoryId(
        @PathVariable Long categoryId
    ) {
        return articleService.getArticlesByCategoryId(categoryId);
    }

    @GetMapping("/by-url")
    public Article getArticleByUrl(
        @RequestParam String url
    ) {
        return articleService.getArticleByUrl(url);
    }

    @PostMapping
    public Article addArticle(@Valid @RequestBody Article article) {
        return articleService.addArticle(article);
    }

    @PutMapping("/{id}")
    public Article updateArticle(
        @PathVariable Long id,
        @Valid @RequestBody Article article
    ) {
        return articleService.updateArticle(id, article);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}


