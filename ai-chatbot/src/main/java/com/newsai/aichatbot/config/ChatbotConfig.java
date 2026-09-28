package com.newsai.aichatbot.config;

/**
 * Cấu hình cho AI Chatbot, đọc từ biến môi trường để không hard-code
 * và không commit thông tin nhạy cảm (API key, URL riêng) vào Git.
 *
 * Biến môi trường hỗ trợ:
 *  - NEWS_AI_BASE_URL   : base URL của backend Spring Boot (mặc định: http://localhost:8080)
 *  - NEWS_AI_TOP_K      : số bài báo tối đa lấy làm ngữ cảnh cho mỗi câu hỏi (mặc định: 5)
 *  - CHATBOT_PORT       : cổng chạy web chat của chatbot (mặc định: 8081, khác cổng
 *                          8080 của backend để chạy song song không xung đột)
 *  - LLM_API_KEY        : (tuỳ chọn) API key cho provider LLM thật, nếu nhóm quyết định
 *                          tích hợp LLM sau này (xem service/LlmArticleSummarizer.java).
 *                          Không đặt giá trị mặc định trong code; nếu không set thì
 *                          chatbot tự dùng ExtractiveArticleSummarizer (không cần key).
 */
public final class ChatbotConfig {

    private final String baseUrl;
    private final int topK;
    private final int port;
    private final String llmApiKey; // có thể null

    private ChatbotConfig(String baseUrl, int topK, int port, String llmApiKey) {
        this.baseUrl = baseUrl;
        this.topK = topK;
        this.port = port;
        this.llmApiKey = llmApiKey;
    }

    public static ChatbotConfig fromEnvironment() {
        String baseUrl = System.getenv().getOrDefault("NEWS_AI_BASE_URL", "http://localhost:8080");
        int topK = parseIntOrDefault(System.getenv("NEWS_AI_TOP_K"), 5);
        int port = parseIntOrDefault(System.getenv("CHATBOT_PORT"), 8081);
        String llmApiKey = System.getenv("LLM_API_KEY"); // null nếu chưa cấu hình
        return new ChatbotConfig(stripTrailingSlash(baseUrl), Math.max(1, topK), port, llmApiKey);
    }

    private static int parseIntOrDefault(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public int topK() {
        return topK;
    }

    public int port() {
        return port;
    }

    public boolean hasLlmApiKey() {
        return llmApiKey != null && !llmApiKey.isBlank();
    }

    public String llmApiKey() {
        return llmApiKey;
    }
}
