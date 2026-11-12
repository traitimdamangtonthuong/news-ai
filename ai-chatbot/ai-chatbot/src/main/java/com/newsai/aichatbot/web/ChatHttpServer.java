package com.newsai.aichatbot.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.newsai.aichatbot.client.NewsApiException;
import com.newsai.aichatbot.model.ChatAnswer;
import com.newsai.aichatbot.service.NewsChatbotService;

/**
 * Web server tối giản cho chatbot, dùng {@code com.sun.net.httpserver} có
 * sẵn trong JDK — KHÔNG cần thêm Spring/Javalin/bất kỳ framework HTTP nào,
 * giữ đúng tinh thần "module Java thuần, tách rời backend Spring Boot".
 *
 * Route:
 *  - GET  /            -> trả về trang chat (static/index.html trong classpath)
 *  - POST /api/chat    -> nhận {"question": "..."} , trả về JSON ChatAnswer
 *                         (summary + citations), gọi NewsChatbotService bên trong.
 */
public class ChatHttpServer {

    private final NewsChatbotService chatbotService;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    public ChatHttpServer(NewsChatbotService chatbotService, int port) {
        this.chatbotService = chatbotService;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleIndex);
        server.createContext("/api/chat", this::handleChat);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlainText(exchange, 405, "Method Not Allowed");
            return;
        }
        try (InputStream in = getClass().getResourceAsStream("/static/index.html")) {
            if (in == null) {
                sendPlainText(exchange, 500, "Khong tim thay static/index.html trong classpath.");
                return;
            }
            byte[] html = in.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, html.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html);
            }
        }
    }

    private void handleChat(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendPlainText(exchange, 405, "Method Not Allowed - dung POST voi body JSON {\"question\": \"...\"}");
            return;
        }

        String question;
        try (InputStream body = exchange.getRequestBody()) {
            byte[] raw = body.readAllBytes();
            ChatRequestDto req = mapper.readValue(raw, ChatRequestDto.class);
            question = req.question();
        } catch (Exception e) {
            sendJson(exchange, 400, new ChatAnswer("Body JSON khong hop le. Vi du dung: {\"question\": \"AI co gi moi\"}", List.of()));
            return;
        }

        if (question == null || question.isBlank()) {
            sendJson(exchange, 400, new ChatAnswer("Cau hoi dang trong.", List.of()));
            return;
        }

        try {
            ChatAnswer answer = chatbotService.answer(question);
            sendJson(exchange, 200, answer);
        } catch (NewsApiException e) {
            sendJson(exchange, 200, new ChatAnswer("Loi khi goi backend: " + e.getMessage(), List.of()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, new ChatAnswer("Loi khong xac dinh phia chatbot: " + e.getMessage(), List.of()));
        }
    }

    private void sendJson(HttpExchange exchange, int status, ChatAnswer answer) throws IOException {
        byte[] json = mapper.writeValueAsBytes(answer);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, json.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json);
        }
    }

    private void sendPlainText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
