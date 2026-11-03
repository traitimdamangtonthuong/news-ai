package com.newsai.news_ai.crawler;

/**
 * Lưu trữ kết quả cào trang HTML: bao gồm phân cấp danh mục và toàn văn nội dung.
 */
public record ArticleScrapeResult(
        BreadcrumbResult breadcrumb,
        String content
) {
    /** Kết quả rỗng khi không tải được trang */
    public static final ArticleScrapeResult EMPTY = new ArticleScrapeResult(BreadcrumbResult.EMPTY, null);
}