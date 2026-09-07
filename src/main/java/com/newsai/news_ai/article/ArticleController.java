package com.newsai.news_ai.article;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/{id}")
    public Article getArticleById(@PathVariable Long id) {
        return articleService.getArticleById(id);
    }

    @PostMapping
    public Article addArticle(@RequestBody Article article) {
        return articleService.addArticle(article);
    }

    @PutMapping("/{id}")
    public Article updateArticle(
        @PathVariable Long id,
        @RequestBody Article article
    ) {
        return articleService.updateArticle(id, article);
    }

    @DeleteMapping("/{id}")
    public String deleteArticle(
        @PathVariable Long id
    ) {
        boolean deleted = articleService.deleteArticle(id);

        if(deleted) {
            return "Article deleted successfully!";
        }

        return "Article not found!";
    }
}


