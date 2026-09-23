package com.newsai.news_ai.article;

/**
 * How an article's category was assigned.
 *
 * <p>{@link #MANUAL} marks admin/user choices that the crawler must never
 * overwrite; {@link #CRAWLER} marks assignments derived from feed metadata,
 * which a later crawl may update. A {@code null} value means the article has
 * never had a category assigned by any source.
 */
public enum CategorySource {
    MANUAL,
    CRAWLER
}