package com.newsai.aichatbot.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.newsai.aichatbot.client.dto.ArticleDetailDto;
import com.newsai.aichatbot.client.dto.ArticleSearchResponseDto;

/**
 * Client gọi REST API sẵn có của backend Spring Boot (module {@code article}):
 *  - GET /api/articles/search
 *  - GET /api/articles/{id}
 *
 * Đây là ranh giới duy nhất giữa chatbot và backend: chỉ qua HTTP/JSON công khai,
 * KHÔNG import bất kỳ class Java nào của backend, KHÔNG truy cập trực tiếp
 * PostgreSQL. Nhờ vậy module này build/chạy độc lập và không thể vô tình sửa
 * hay phá vỡ code của thành viên khác.
 */
public class NewsApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public NewsApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Tương ứng GET /api/articles/search?keyword=...&page=0&size={size}
     * (chỉ set keyword + page + size; các filter khác backend hỗ trợ nhưng
     * chatbot bản đầu chưa cần dùng tới, theo đúng README của dự án).
     */
    public ArticleSearchResponseDto searchArticles(String keyword, int size) {
        String encodedKeyword = java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = baseUrl + "/api/articles/search?keyword=" + encodedKeyword
                + "&page=0&size=" + size;
        String body = get(url);
        try {
            return mapper.readValue(body, ArticleSearchResponseDto.class);
        } catch (IOException e) {
            throw new NewsApiException("Không parse được kết quả search từ backend: " + url, e);
        }
    }

    /** Tương ứng GET /api/articles/{id}, dùng để lấy full content cho tóm tắt. */
    public ArticleDetailDto getArticleById(Long id) {
        String url = baseUrl + "/api/articles/" + id;
        String body = get(url);
        try {
            return mapper.readValue(body, ArticleDetailDto.class);
        } catch (IOException e) {
            throw new NewsApiException("Không parse được article id=" + id + " từ backend", e);
        }
    }

    /** Lấy nhiều bài chi tiết cùng lúc, bỏ qua item nào lỗi thay vì làm hỏng cả câu trả lời. */
    public List<ArticleDetailDto> getArticlesByIds(List<Long> ids) {
        return ids.stream()
                .map(id -> {
                    try {
                        return getArticleById(id);
                    } catch (NewsApiException e) {
                        System.err.println("[canh-bao] bo qua article id=" + id + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new NewsApiException(
                        "Backend tra ve HTTP " + response.statusCode() + " cho " + url
                                + " -- kiem tra backend co dang chay khong (mac dinh http://localhost:8080).");
            }
            return response.body();
        } catch (HttpTimeoutException e) {
            throw new NewsApiException("Goi backend qua thoi gian cho: " + url, e);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new NewsApiException(
                    "Khong ket noi duoc backend tai " + url
                            + " -- dam bao Spring Boot dang chay (mvnw spring-boot:run).", e);
        }
    }
}
