package com.newsai.news_ai.article;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

@Service 
public class ArticleService {
    private final ArticleRepository articleRepository;

    public ArticleService(
        ArticleRepository articleRepository
    ) {
        this.articleRepository = articleRepository;
    }

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    };

    public Article getArticleById(Long id) {
        return articleRepository.findById(id).orElse(null);
    }

    public Article addArticle(Article article) {
        article.setId(null);
        article.setCrawledAt(LocalDateTime.now());

        return articleRepository.save(article);
    }

    public Article updateArticle(Long id, Article updatedArticle) {
        Article article = articleRepository.findById(id).orElse(null);
        if(article == null) return null;
        article.setTitle(updatedArticle.getTitle());
        article.setDescription(updatedArticle.getDescription());
        article.setContent(updatedArticle.getContent());
        article.setSource(updatedArticle.getSource());
        article.setUrl(updatedArticle.getUrl());
        article.setImageUrl(updatedArticle.getImageUrl());
        article.setPublishedAt(updatedArticle.getPublishedAt());

        return articleRepository.save(article);
    }

    public boolean deleteArticle(Long id) {
        if(!articleRepository.existsById(id)) {
            return false;
        }

        articleRepository.deleteById(id);
        return true;
    }
}
