package com.newsai.news_ai.crawler;

import java.util.List;

/**
 * Result of parsing one feed: the usable items plus a count of entries that
 * were skipped because they were missing required fields (title/link).
 */
public record FeedParseResult(
        List<RssFeedItem> items,
        int skippedMalformed
) {
}