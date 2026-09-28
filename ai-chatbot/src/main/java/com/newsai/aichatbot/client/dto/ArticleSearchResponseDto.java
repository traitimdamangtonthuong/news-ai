package com.newsai.aichatbot.client.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleSearchResponseDto(
        List<ArticleSearchItemDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
