package com.newsai.aichatbot.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Trích một keyword đơn giản từ câu hỏi người dùng để gọi
 * {@code GET /api/articles/search?keyword=...}.
 *
 * LƯU Ý QUAN TRỌNG (đọc README.md của ai-chatbot để biết thêm): backend hiện
 * tại tìm theo kiểu {@code LIKE '%keyword%'} với MỘT chuỗi duy nhất trên
 * title/description/content — không phải tìm kiếm đa từ khoá (AND/OR) và
 * không có tokenizer tiếng Việt. Vì vậy lớp này chỉ chọn RA MỘT từ có khả
 * năng mang nhiều thông tin nhất trong câu hỏi (loại bỏ từ hỏi/từ nối phổ
 * biến), thay vì cố gắng ghép nhiều từ khoá — ghép nhiều từ dễ làm giảm số
 * kết quả tìm thấy vì phải khớp NGUYÊN CỤM liên tiếp trong bài báo gốc.
 *
 * Đây là bước tối giản đúng theo lộ trình trong README ("Backend tách keyword
 * đơn giản"). Khi nhóm nâng cấp retrieval (full-text search, embeddings...),
 * chỉ cần thay thế class này, các phần còn lại của chatbot không cần đổi.
 */
public final class KeywordExtractor {

    // Các từ hỏi / từ nối phổ biến trong câu hỏi tiếng Việt và tiếng Anh, không mang
    // nhiều thông tin để tìm kiếm. Danh sách tối giản, có thể mở rộng khi cần.
    private static final Set<String> STOPWORDS = Set.of(
            "là", "gì", "của", "cho", "về", "và", "các", "những", "một", "có",
            "hãy", "tóm", "tắt", "tổng", "hợp", "tôi", "mình", "bạn",
            "giúp", "xin", "vui", "lòng", "như", "thế", "nào", "ở", "đâu",
            "khi", "sao", "tại", "sự", "này", "đó", "được", "với",
            "the", "a", "an", "is", "are", "of", "in", "on", "for", "about",
            "what", "how", "summarize", "summary", "please", "tell", "me"
    );

    private KeywordExtractor() {
    }

    public static String extract(String question) {
        if (question == null || question.isBlank()) {
            return "";
        }
        String[] rawTokens = question.toLowerCase().trim().split("[^\\p{L}0-9]+");
        List<String> candidates = Arrays.stream(rawTokens)
                .filter(t -> t.length() >= 3)
                .filter(t -> !STOPWORDS.contains(t))
                .toList();

        if (candidates.isEmpty()) {
            // Không lọc được từ nào nổi bật -> để backend tìm "match all" thay vì
            // gửi một chuỗi rỗng gây hiểu lầm; trả nguyên câu hỏi đã trim làm phương án cuối.
            return question.trim();
        }

        // Chọn từ dài nhất làm proxy đơn giản cho "danh từ/thực thể quan trọng nhất".
        return candidates.stream()
                .max((a, b) -> Integer.compare(a.length(), b.length()))
                .orElse(candidates.get(0));
    }
}
