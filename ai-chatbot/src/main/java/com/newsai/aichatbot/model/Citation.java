package com.newsai.aichatbot.model;

/** Trích dẫn nguồn cho một bài báo đã được dùng để tổng hợp câu trả lời. */
public record Citation(Long articleId, String title, String sourceName, String url) {
}
