package com.newsai.aichatbot.model;

/** Một bài báo đã lấy từ backend, dùng làm ngữ cảnh để tổng hợp/tóm tắt. */
public record ArticleContext(
        Long id,
        String title,
        String sourceName,
        String url,
        String text // description + content, đã gộp để đưa vào bước tóm tắt
) {
}
