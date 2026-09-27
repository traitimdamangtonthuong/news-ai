package com.newsai.aichatbot.service;

import java.util.List;

import com.newsai.aichatbot.model.ArticleContext;

/**
 * Trừu tượng hoá bước "tổng hợp nhiều bài báo thành 1 đoạn trả lời".
 * Có 2 cách triển khai:
 *  - {@link ExtractiveArticleSummarizer}: mặc định, thuật toán tự viết,
 *    KHÔNG cần API key / gọi mạng ra ngoài.
 *  - Sau này nhóm có thể viết thêm 1 class implement interface này để gọi
 *    LLM thật (OpenAI/Gemini/Spring AI...) mà không cần sửa NewsChatbotService.
 */
public interface ArticleSummarizer {
    String summarize(String question, List<ArticleContext> articles);
}
