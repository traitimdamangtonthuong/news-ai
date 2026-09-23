package com.newsai.news_ai.crawler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A single news item extracted from an RSS/Atom feed,
 * already cleaned and URL-resolved by {@link FeedItemMapper}.
 */
public record RssFeedItem(
        String title,
        String url,
        String description,
        String content,
        String imageUrl,
        LocalDateTime publishedAt,
        List<String> categories
) {
}