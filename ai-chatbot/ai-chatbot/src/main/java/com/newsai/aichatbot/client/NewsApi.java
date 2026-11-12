package com.newsai.aichatbot.client;

import java.util.List;

import com.newsai.aichatbot.client.dto.ArticleDetailDto;
import com.newsai.aichatbot.client.dto.ArticleSearchResponseDto;
import com.newsai.aichatbot.client.dto.NamedItemDto;

/**
 * Hợp đồng giữa chatbot và backend news-ai: chỉ gồm các REST API công khai đã
 * có sẵn. Tách thành interface để chatbot có thể kiểm thử với dữ liệu giả mà
 * không cần chạy backend.
 */
public interface NewsApi {

    /** GET /api/articles/search */
    ArticleSearchResponseDto searchArticles(SearchRequest request);

    /** GET /api/articles/{id} cho từng id; bài nào lỗi thì bị bỏ qua. Giữ nguyên thứ tự id. */
    List<ArticleDetailDto> getArticlesByIds(List<Long> ids);

    /** GET /api/sources */
    List<NamedItemDto> listSources();

    /** GET /api/categories */
    List<NamedItemDto> listCategories();
}
