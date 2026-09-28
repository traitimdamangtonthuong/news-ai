package com.newsai.aichatbot.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.newsai.aichatbot.model.ArticleContext;

/**
 * Tóm tắt bằng LLM thật — gọi Gemini API (Google AI Studio) qua REST, dùng
 * java.net.http.HttpClient có sẵn trong JDK, không cần thêm SDK ngoài.
 *
 * Cần biến môi trường {@code LLM_API_KEY} = API key lấy tại
 * https://aistudio.google.com/apikey (gói miễn phí đủ dùng để demo/nộp bài).
 *
 * Nếu chưa cấu hình key, hoặc gọi API lỗi (mất mạng, hết quota, key sai...),
 * class này tự động rơi về {@link ExtractiveArticleSummarizer} để chatbot vẫn
 * trả lời được thay vì crash — luôn in cảnh báo ra console để dễ debug.
 */
public class LlmArticleSummarizer implements ArticleSummarizer {

    // gemini-2.5-flash da bi Google go khoi danh sach cho user moi (thang 9/2026);
    // gemini-3.8-flash la model duoc khuyen nghi thay the, van dung chung REST
    // endpoint generateContent nen khong can doi logic parse response.
    private static final String MODEL = "gemini-3.8-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

    // Cat bot noi dung moi bai de khong vuot gioi han token / ton quota vo ich.
    private static final int MAX_CHARS_PER_ARTICLE = 1500;

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ArticleSummarizer fallback = new ExtractiveArticleSummarizer();

    public LlmArticleSummarizer(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String summarize(String question, List<ArticleContext> articles) {
        if (apiKey == null || apiKey.isBlank()) {
            // Chua cau hinh LLM_API_KEY -> dung tam ban extractive de chatbot van chay duoc.
            return fallback.summarize(question, articles);
        }
        if (articles == null || articles.isEmpty()) {
            return "Không tìm thấy bài báo nào liên quan đến câu hỏi này trong dữ liệu đã crawl.";
        }

        try {
            String requestBody = mapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", buildPrompt(question, articles)))
                    )),
                    "generationConfig", Map.of(
                            "thinkingConfig", Map.of("thinkingLevel", "low")
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                System.err.println("[canh-bao] Gemini API loi HTTP " + response.statusCode()
                        + ": " + response.body() + " -- dung ban extractive thay the.");
                return fallback.summarize(question, articles);
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                System.err.println("[canh-bao] Gemini API tra ve rong -- dung ban extractive thay the. Response: "
                        + response.body());
                return fallback.summarize(question, articles);
            }
            return textNode.asText().strip();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("[canh-bao] Khong goi duoc Gemini API (" + e.getMessage()
                    + ") -- dung ban extractive thay the.");
            return fallback.summarize(question, articles);
        }
    }

    private String buildPrompt(String question, List<ArticleContext> articles) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý tổng hợp tin tức. Dựa CHỈ vào nội dung các bài báo được cung cấp ")
          .append("dưới đây, hãy trả lời câu hỏi của người dùng bằng tiếng Việt, viết lại bằng văn ")
          .append("phong tự nhiên, súc tích. Không bịa thêm thông tin không có trong bài báo. ")
          .append("Nếu các bài báo không đủ thông tin để trả lời, hãy nói rõ điều đó.\n\n");
        sb.append("Câu hỏi của người dùng: ").append(question).append("\n\n");
        sb.append("Các bài báo liên quan:\n");

        int i = 1;
        for (ArticleContext a : articles) {
            String text = a.text() == null ? "" : a.text();
            if (text.length() > MAX_CHARS_PER_ARTICLE) {
                text = text.substring(0, MAX_CHARS_PER_ARTICLE) + "...";
            }
            sb.append(i++).append(". [").append(a.sourceName()).append("] ")
              .append(a.title()).append("\n").append(text).append("\n\n");
        }
        return sb.toString();
    }
}
