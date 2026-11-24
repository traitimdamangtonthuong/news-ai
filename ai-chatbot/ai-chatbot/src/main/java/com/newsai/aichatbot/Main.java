package com.newsai.aichatbot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.newsai.aichatbot.client.NewsApiClient;
import com.newsai.aichatbot.client.NewsApiException;
import com.newsai.aichatbot.config.ChatbotConfig;
import com.newsai.aichatbot.model.ChatAnswer;
import com.newsai.aichatbot.model.Citation;
import com.newsai.aichatbot.service.ArticleSummarizer;
import com.newsai.aichatbot.service.LlmArticleSummarizer;
import com.newsai.aichatbot.service.NewsChatbotService;
import com.newsai.aichatbot.web.ChatHttpServer;

/**
 * Diem khoi chay cua AI Chatbot - module HOAN TOAN DOC LAP voi backend:
 *  - Khong nam trong src/main/java/com/newsai/news_ai (source cua backend).
 *  - Khong import bat ky class nao cua backend hay cua cac thanh vien khac.
 *  - Chi giao tiep voi backend qua REST API HTTP da co san.
 *
 * Chay (sau khi backend Spring Boot da chay o localhost:8080):
 *   cd ai-chatbot
 *   .\mvnw.cmd package        (hoac ./mvnw package tren macOS/Linux)
 *   java -jar target\ai-chatbot-0.1.0.jar
 *
 * Mac dinh mo web chat tai http://localhost:8081 (mo link do bang trinh duyet).
 * Muon dung che do chat qua terminal (khong can trinh duyet) nhu ban cu:
 *   java -jar target\ai-chatbot-0.1.0.jar --cli
 *
 * Tuy chinh qua bien moi truong (xem ChatbotConfig):
 *   NEWS_AI_BASE_URL, NEWS_AI_TOP_K, CHATBOT_PORT, LLM_API_KEY (tuy chon), LLM_MODEL (tuy chon).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        ChatbotConfig config = ChatbotConfig.fromEnvironment();
        NewsApiClient apiClient = new NewsApiClient(config.baseUrl());

        // Dung LLM (Gemini) neu da co LLM_API_KEY; neu chua thi tu dong fallback
        // sang ExtractiveArticleSummarizer ben trong LlmArticleSummarizer.
        ArticleSummarizer summarizer = new LlmArticleSummarizer(config.llmApiKey(), config.llmModel());
        System.out.println(config.hasLlmApiKey()
                ? "Tom tat    : Gemini (tu dong dung ban trich cau neu Gemini loi)"
                : "Tom tat    : trich cau (extractive), khong can API key");

        NewsChatbotService chatbot = new NewsChatbotService(apiClient, summarizer, config.topK());

        boolean cliMode = args.length > 0 && ("--cli".equalsIgnoreCase(args[0]) || "-cli".equalsIgnoreCase(args[0]));

        if (cliMode) {
            runCli(chatbot, config);
        } else {
            runWeb(chatbot, config);
        }
    }

    private static void runWeb(NewsChatbotService chatbot, ChatbotConfig config) throws Exception {
        ChatHttpServer server = new ChatHttpServer(chatbot, config.port());
        server.start();

        System.out.println("=== News AI Chatbot (web) ===");
        System.out.println("Backend  : " + config.baseUrl());
        System.out.println("Chat UI  : http://localhost:" + config.port());
        System.out.println("Mo duong link tren bang trinh duyet de chat.");
        System.out.println("Nhan Ctrl+C de dung server.");

        // Giu tien trinh chay vi HttpServer chay tren thread rieng.
        Thread.currentThread().join();
    }

    private static void runCli(NewsChatbotService chatbot, ChatbotConfig config) throws Exception {
        System.out.println("=== News AI Chatbot (terminal) ===");
        System.out.println("Backend: " + config.baseUrl());
        System.out.println("Go cau hoi roi Enter. Go 'exit' hoac 'thoat' de dung.\n");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print("Ban: ");
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                String question = line.strip();
                if (question.isEmpty()) {
                    continue;
                }
                if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("thoat")) {
                    System.out.println("Tam biet!");
                    break;
                }

                try {
                    ChatAnswer answer = chatbot.answer(question);
                    printAnswer(answer);
                } catch (NewsApiException e) {
                    System.out.println("Loi khi goi backend: " + e.getMessage());
                }
            }
        }
    }

    private static void printAnswer(ChatAnswer answer) {
        System.out.println("\nChatbot:");
        System.out.println(answer.summary());
        if (answer.hasCitations()) {
            System.out.println("\nNguon:");
            int n = 1;
            for (Citation c : answer.citations()) {
                System.out.println("  [" + n++ + "] " + c.title() + " - " + c.sourceName()
                        + (c.publishedAt() != null ? " - " + c.publishedAt() : "") + " - " + c.url());
            }
        }
        System.out.println();
    }
}
