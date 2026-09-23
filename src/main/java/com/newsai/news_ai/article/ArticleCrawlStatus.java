package com.newsai.news_ai.article;

/**
 * Outcome of processing one feed item against the article store.
 */
public enum ArticleCrawlStatus {
    /** No article existed for the canonical URL; a new row was stored. */
    CREATED,
    /** An existing article was found and crawler-managed data changed. */
    UPDATED,
    /** The article was observed but no crawler-managed data changed. */
    UNCHANGED
}