package com.newsai.aichatbot.service;

import java.util.ArrayList;
import java.util.List;

import com.newsai.aichatbot.client.NewsApiClient;
import com.newsai.aichatbot.client.dto.ArticleDetailDto;
import com.newsai.aichatbot.client.dto.ArticleSearchItemDto;
import com.newsai.aichatbot.client.dto.ArticleSearchResponseDto;
import com.newsai.aichatbot.model.ArticleContext;
import com.newsai.aichatbot.model.ChatAnswer;
import com.newsai.aichatbot.model.Citation;

/**
 * Luồng xử lý một câu hỏi, đúng theo mô tả tối giản trong README của dự án:
 *
 *   Người dùng gửi câu hỏi
 *   → tách keyword đơn giản
 *   → search Article trong PostgreSQL (qua REST API backend)
 *   → chọn vài bài liên quan làm ngữ cảnh
 *   → tổng hợp (extractive mặc định, hoặc LLM nếu đã cấu hình)
 *   → trả lời kèm title, source và URL bài gốc
 */
public class NewsChatbotService {

    private final NewsApiClient apiClient;
    private final ArticleSummarizer summarizer;
    private final int topK;

    public NewsChatbotService(NewsApiClient apiClient, ArticleSummarizer summarizer, int topK) {
        this.apiClient = apiClient;
        this.summarizer = summarizer;
        this.topK = topK;
    }

    public ChatAnswer answer(String question) {
        String keyword = KeywordExtractor.extract(question);

        ArticleSearchResponseDto searchResult = apiClient.searchArticles(keyword, topK);
        List<ArticleSearchItemDto> items = searchResult.items();

        if (items == null || items.isEmpty()) {
            return new ChatAnswer(
                    "Không tìm thấy bài báo nào phù hợp với từ khoá \"" + keyword
                            + "\" trong dữ liệu đã crawl. Thử hỏi bằng từ khoá khác hoặc kiểm tra xem "
                            + "nguồn tin liên quan đã được crawl chưa.",
                    List.of());
        }

        List<Long> ids = items.stream().map(ArticleSearchItemDto::id).toList();
        List<ArticleDetailDto> details = apiClient.getArticlesByIds(ids);

        List<ArticleContext> contexts = new ArrayList<>();
        List<Citation> citations = new ArrayList<>();
        for (ArticleDetailDto d : details) {
            String sourceName = d.source() != null ? d.source().name() : "Không rõ nguồn";
            String combinedText = String.join(
                    " ",
                    d.description() == null ? "" : d.description(),
                    d.content() == null ? "" : d.content()
            ).strip();

            contexts.add(new ArticleContext(d.id(), d.title(), sourceName, d.url(), combinedText));
            citations.add(new Citation(d.id(), d.title(), sourceName, d.url()));
        }

        String summary = summarizer.summarize(question, contexts);
        return new ChatAnswer(summary, citations);
    }
}
