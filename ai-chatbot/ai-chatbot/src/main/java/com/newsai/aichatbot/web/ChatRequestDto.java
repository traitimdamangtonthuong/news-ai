package com.newsai.aichatbot.web;

/** Body JSON gửi lên POST /api/chat: {"question": "..."} */
public record ChatRequestDto(String question) {
}
