package com.newsai.aichatbot.client.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Bản sao cấu trúc dữ liệu (chỉ để đọc JSON) khớp với
 * {@code com.newsai.news_ai.article.ArticleSearchItem} bên backend.
 *
 * Đây KHÔNG phải import trực tiếp class của backend — module này hoàn toàn
 * độc lập, chỉ nói chuyện với backend qua REST API (HTTP + JSON), nên không
 * có dependency biên dịch nào tới source code của các thành viên khác.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleSearchItemDto(
        Long id,
        String title,
        String description,
        String imageUrl,
        LocalDateTime publishedAt,
        Reference source,
        Reference category
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reference(Long id, String name) {
    }
}
