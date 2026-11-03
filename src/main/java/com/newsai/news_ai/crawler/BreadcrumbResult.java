package com.newsai.news_ai.crawler;

import java.util.List;
/**
 * Kết quả đọc breadcrumb từ trang bài viết.
 *
 * <p>Ví dụ VnExpress: breadcrumb "Khoa học công nghệ > AI"
 * → parentCategoryName = "Khoa học công nghệ", subCategoryName = "AI"
 *
 * <p>Nếu không đọc được (lỗi network, cấu trúc thay đổi, …) cả hai trường đều null.
 */
public record BreadcrumbResult(List<BreadcrumbNode> chain) {
    
    /** Kết quả rỗng (không đọc được breadcrumb). */
    public static final BreadcrumbResult EMPTY = new BreadcrumbResult(List.of());

    public boolean hasData() {
        return chain != null && !chain.isEmpty();
    }

    /**
     * Đại diện cho một node trong dải breadcrumb
     */
    public record BreadcrumbNode(String name, String slug) {}
}