package com.newsai.news_ai.crawler;

import java.util.List;

/**
 * Kết quả đọc breadcrumb từ trang bài viết.
 *
 * <p>Hỗ trợ phân cấp danh mục N-cấp.
 * Ví dụ VnExpress: breadcrumb "Xe > Thị trường > Thế giới"
 * → Trả về một chuỗi (chain) các danh mục theo thứ tự từ lớn đến bé: ["Xe", "Thị trường", "Thế giới"].
 *
 * <p>Nếu không đọc được (lỗi network, cấu trúc thay đổi, …), chuỗi trả về sẽ rỗng (EMPTY).
 */
public record BreadcrumbResult(List<BreadcrumbNode> chain) {
    
    /** Kết quả rỗng (không đọc được breadcrumb hoặc không có). */
    public static final BreadcrumbResult EMPTY = new BreadcrumbResult(List.of());

    /** Kiểm tra xem có lấy được danh mục nào từ breadcrumb hay không. */
    public boolean hasData() {
        return chain != null && !chain.isEmpty();
    }

    /**
     * Đại diện cho một mắt xích (danh mục) trong dải breadcrumb.
     *
     * @param name Tên danh mục hiển thị (ví dụ: "Thế giới")
     * @param slug Đường dẫn tĩnh (ví dụ: "the-gioi")
     */
    public record BreadcrumbNode(String name, String slug) {}
}