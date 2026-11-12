package com.newsai.aichatbot.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Một bài báo đã lấy từ backend và đã làm sạch HTML, dùng làm ngữ cảnh để
 * tổng hợp/tóm tắt.
 *
 * @param description mô tả ngắn (đã bỏ HTML), có thể rỗng
 * @param paragraphs  các đoạn văn của phần nội dung (đã bỏ HTML, ảnh, chú thích), có thể rỗng
 */
public record ArticleContext(
        Long id,
        String title,
        String sourceName,
        String url,
        LocalDateTime publishedAt,
        String description,
        List<String> paragraphs
) {
    /** Toàn bộ văn bản: description + các đoạn nội dung, mỗi phần một dòng. */
    public String text() {
        StringBuilder sb = new StringBuilder();
        if (description != null && !description.isBlank()) {
            sb.append(description.strip());
        }
        if (paragraphs != null) {
            for (String p : paragraphs) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(p);
            }
        }
        return sb.toString();
    }
}
