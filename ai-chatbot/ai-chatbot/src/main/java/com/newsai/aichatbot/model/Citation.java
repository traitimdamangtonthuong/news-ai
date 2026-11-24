package com.newsai.aichatbot.model;

/**
 * Trích dẫn nguồn cho một bài báo đã được dùng để tổng hợp câu trả lời.
 * Thứ tự trong danh sách citations khớp số thứ tự [1], [2]... trong câu trả lời.
 *
 * @param publishedAt ngày đăng dạng dd/MM/yyyy, null nếu bài không có ngày
 */
public record Citation(Long articleId, String title, String sourceName, String url, String publishedAt) {
}
