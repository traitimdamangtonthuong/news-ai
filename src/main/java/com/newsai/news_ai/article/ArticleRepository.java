package com.newsai.news_ai.article;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArticleRepository
        extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    List<Article> findBySourceId(Long sourceId);

    List<Article> findByCategoryId(Long categoryId);

    Optional<Article> findByUrl(String url);

    Optional<Article> findByCanonicalUrl(String canonicalUrl);

    boolean existsByCanonicalUrl(String canonicalUrl);
}
