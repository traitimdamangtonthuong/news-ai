package com.newsai.aichatbot.client;

/** Ném ra khi gọi REST API của backend news-ai thất bại (lỗi mạng, HTTP status lỗi, JSON không hợp lệ...). */
public class NewsApiException extends RuntimeException {
    public NewsApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public NewsApiException(String message) {
        super(message);
    }
}
