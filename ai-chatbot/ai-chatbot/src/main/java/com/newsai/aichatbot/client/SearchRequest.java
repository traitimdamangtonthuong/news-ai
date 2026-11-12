package com.newsai.aichatbot.client;

import java.time.LocalDate;

/**
 * Tham số cho {@code GET /api/articles/search}. Mọi field đều tuỳ chọn trừ
 * {@code page} (từ 0) và {@code size} (backend giới hạn 1-50). Ngày dùng định dạng yyyy-MM-dd.
 */
public record SearchRequest(
        String keyword,
        Long sourceId,
        Long categoryId,
        LocalDate publishedFrom,
        LocalDate publishedTo,
        int page,
        int size
) {
}
