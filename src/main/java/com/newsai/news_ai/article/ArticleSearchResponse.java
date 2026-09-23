package com.newsai.news_ai.article;

import java.util.List;

public record ArticleSearchResponse(
        List<ArticleSearchItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}