package com.newsai.news_ai.article;

import java.time.LocalDateTime;

public record ArticleSearchItem(
        Long id,
        String title,
        String description,
        String imageUrl,
        LocalDateTime publishedAt,
        Reference source,
        Reference category
) {
    public record Reference(Long id, String name) {
    }
}