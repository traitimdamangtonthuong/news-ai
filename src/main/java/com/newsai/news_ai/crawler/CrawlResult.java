package com.newsai.news_ai.crawler;

import java.util.List;

/**
 * Summary returned for one crawled source. Per-item problems are accumulated
 * in {@code errors} instead of failing the whole crawl.
 *
 * <p>{@code fetched} is the number of items read from the feed; every item is
 * then classified as created/updated/unchanged or failed. A failed item is one
 * whose upsert threw after the race-conflict retry.
 */
public record CrawlResult(
        Long sourceId,
        String sourceName,
        int fetched,
        int created,
        int updated,
        int unchanged,
        int failed,
        List<String> errors
) {
}