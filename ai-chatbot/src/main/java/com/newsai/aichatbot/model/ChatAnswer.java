package com.newsai.aichatbot.model;

import java.util.List;

/** Kết quả cuối cùng trả cho người dùng: đoạn tổng hợp + trích dẫn các bài báo gốc. */
public record ChatAnswer(String summary, List<Citation> citations) {

    public boolean hasCitations() {
        return citations != null && !citations.isEmpty();
    }
}
