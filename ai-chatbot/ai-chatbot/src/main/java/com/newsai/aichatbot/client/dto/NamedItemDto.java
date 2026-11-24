package com.newsai.aichatbot.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Chỉ đọc {@code id} + {@code name} từ response của {@code GET /api/sources}
 * và {@code GET /api/categories}; các field khác (rssUrl, parent...) bị bỏ qua.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NamedItemDto(Long id, String name) {
}
