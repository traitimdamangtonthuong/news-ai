package com.newsai.news_ai.article;

/**
 * Result of one {@link ArticleService#upsertFromCrawl(Article)} call.
 *
 * @param status  what happened to the article
 * @param article the stored {@link Article} after the operation
 */
public record ArticleCrawlOutcome(
        ArticleCrawlStatus status,
        Article article) {
}