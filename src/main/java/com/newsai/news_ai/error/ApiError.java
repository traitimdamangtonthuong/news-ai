package com.newsai.news_ai.error;

import java.time.LocalDateTime;

public record ApiError(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        String path
) {
}