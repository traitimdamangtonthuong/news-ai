package com.newsai.aichatbot.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.newsai.aichatbot.client.dto.ArticleDetailDto;
import com.newsai.aichatbot.client.dto.ArticleSearchResponseDto;
import com.newsai.aichatbot.client.dto.NamedItemDto;

/**
 * Client gọi REST API sẵn có của backend Spring Boot:
 *  - GET /api/articles/search
 *  - GET /api/articles/{id}
 *  - GET /api/sources, GET /api/categories (để nhận diện nguồn/chuyên mục trong câu hỏi)
 *
 * Đây là ranh giới duy nhất giữa chatbot và backend: chỉ qua HTTP/JSON công khai,
 * KHÔNG import bất kỳ class Java nào của backend, KHÔNG truy cập trực tiếp
 * PostgreSQL. Nhờ vậy module này build/chạy độc lập và không thể vô tình sửa
 * hay phá vỡ code của thành viên khác.
 */
public class NewsApiClient implements NewsApi {

    private static final int DETAIL_PARALLELISM = 4;

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    // Luồng daemon: không giữ JVM sống khi thoát chương trình.
    private final ExecutorService detailExecutor = Executors.newFixedThreadPool(DETAIL_PARALLELISM, r -> {
        Thread t = new Thread(r, "news-api-detail");
        t.setDaemon(true);
        return t;
    });

    public NewsApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ArticleSearchResponseDto searchArticles(SearchRequest request) {
        StringBuilder url = new StringBuilder(baseUrl).append("/api/articles/search?page=")
                .append(request.page()).append("&size=").append(request.size());
        if (request.keyword() != null && !request.keyword().isBlank()) {
            url.append("&keyword=").append(encode(request.keyword().strip()));
        }
        if (request.sourceId() != null) {
            url.append("&sourceId=").append(request.sourceId());
        }
        if (request.categoryId() != null) {
            url.append("&categoryId=").append(request.categoryId());
        }
        if (request.publishedFrom() != null) {
            url.append("&publishedFrom=").append(request.publishedFrom());
        }
        if (request.publishedTo() != null) {
            url.append("&publishedTo=").append(request.publishedTo());
        }
        String body = get(url.toString());
        try {
            return mapper.readValue(body, ArticleSearchResponseDto.class);
        } catch (IOException e) {
            throw new NewsApiException("Không parse được kết quả search từ backend: " + url, e);
        }
    }

    /** Tiện ích: chỉ theo keyword + size (giữ lại cho tương thích). */
    public ArticleSearchResponseDto searchArticles(String keyword, int size) {
        return searchArticles(new SearchRequest(keyword, null, null, null, null, 0, size));
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

    /** Lấy nhiều bài chi tiết song song, bỏ qua item nào lỗi thay vì làm hỏng cả câu trả lời. */
    @Override
    public List<ArticleDetailDto> getArticlesByIds(List<Long> ids) {
        List<CompletableFuture<ArticleDetailDto>> futures = new ArrayList<>();
        for (Long id : ids) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return getArticleById(id);
                } catch (NewsApiException e) {
                    System.err.println("[canh-bao] bo qua article id=" + id + ": " + e.getMessage());
                    return null;
                }
            }, detailExecutor));
        }
        List<ArticleDetailDto> out = new ArrayList<>();
        for (CompletableFuture<ArticleDetailDto> f : futures) {
            ArticleDetailDto d = f.join();
            if (d != null) {
                out.add(d);
            }
        }
        return out;
    }

    @Override
    public List<NamedItemDto> listSources() {
        return getList("/api/sources");
    }

    @Override
    public List<NamedItemDto> listCategories() {
        return getList("/api/categories");
    }

    private List<NamedItemDto> getList(String path) {
        String url = baseUrl + path;
        String body = get(url);
        try {
            return mapper.readValue(body, new TypeReference<List<NamedItemDto>>() { });
        } catch (IOException e) {
            throw new NewsApiException("Không parse được danh sách từ backend: " + url, e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
