package com.newsai.aichatbot.client.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Chỉ đọc các field cần thiết từ response JSON của
 * {@code GET /api/articles/{id}} (entity Article đầy đủ, có content).
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} giúp DTO này không vỡ
 * nếu backend thêm/đổi field khác mà chatbot không dùng tới.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleDetailDto(
        Long id,
        String title,
        String description,
        String content,
        String url,
        LocalDateTime publishedAt,
        NamedRef source,
        NamedRef category
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamedRef(Long id, String name) {
    }
}
