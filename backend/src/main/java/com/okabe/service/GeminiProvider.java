package com.okabe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HTTP client cho Groq API (định dạng tương thích OpenAI).
 * Free tier: 14,400 req/day, không cần credit card.
 * Docs: https://console.groq.com/docs/openai
 */
@Service
@Slf4j
public class GeminiProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${app.ai.groq.max-tokens:1000}")
    private int maxTokens;

    public GeminiProvider(
            @Value("${app.ai.groq.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // ─── Non-streaming ────────────────────────────────────────────────────────

    // Gọi Groq Chat Completion API (blocking, trả về toàn bộ phản hồi)
    public String generateContent(String systemPrompt, List<Map<String, String>> messages) {
        List<Map<String, String>> allMessages = buildMessageList(systemPrompt, messages);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", allMessages,
                "max_tokens", maxTokens,
                "temperature", 0.7
        );

        try {
            log.debug("Calling Groq API (blocking) with model: {}", model);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return extractTextFromChoice(response);

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Groq API error: " + e.getMessage(), e);
        }
    }

    // ─── Streaming ────────────────────────────────────────────────────────────

    // Gọi Groq Chat Completion với stream=true, gọi onToken cho mỗi chunk, onComplete khi kết thúc
    public void streamContent(
            String systemPrompt,
            List<Map<String, String>> messages,
            Consumer<String> onToken,
            Consumer<String> onComplete) {

        List<Map<String, String>> allMessages = buildMessageList(systemPrompt, messages);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", allMessages,
                "max_tokens", maxTokens,
                "temperature", 0.7,
                "stream", true
        );

        log.debug("Calling Groq API (streaming) with model: {}", model);

        try {
            restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((request, response) -> {
                        StringBuilder fullText = new StringBuilder();
                        try (InputStream is = response.getBody();
                             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) break;
                                    String token = extractTokenFromChunk(data);
                                    if (token != null && !token.isEmpty()) {
                                        onToken.accept(token);
                                        fullText.append(token);
                                    }
                                }
                            }
                        }
                        onComplete.accept(fullText.toString());
                        return null;
                    });

        } catch (Exception e) {
            log.error("Groq streaming failed: {}", e.getMessage(), e);
            throw new RuntimeException("Groq streaming error: " + e.getMessage(), e);
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    // Xây dựng danh sách messages bao gồm system prompt và lịch sử hội thoại
    private List<Map<String, String>> buildMessageList(String systemPrompt, List<Map<String, String>> messages) {
        List<Map<String, String>> all = new ArrayList<>();
        all.add(Map.of("role", "system", "content", systemPrompt));
        messages.forEach(m -> all.add(Map.of(
                "role", mapRole(m.get("role")),
                "content", m.get("content")
        )));
        return all;
    }

    // Trích xuất nội dung từ phản hồi JSON của Groq
    @SuppressWarnings("unchecked")
    private String extractTextFromChoice(Map<String, Object> response) {
        if (response == null) return "Xin lỗi, không nhận được phản hồi từ AI.";
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("Failed to parse Groq response: {}", e.getMessage());
            return "Xin lỗi, tôi gặp lỗi khi xử lý phản hồi.";
        }
    }

    // Trích xuất token từ chunk JSON trong streaming response
    @SuppressWarnings("unchecked")
    private String extractTokenFromChunk(String jsonChunk) {
        try {
            Map<String, Object> chunk = objectMapper.readValue(jsonChunk, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
            if (delta == null) return null;
            return (String) delta.get("content");
        } catch (Exception e) {
            return null; // Bỏ qua chunk lỗi
        }
    }

    // Groq sử dụng OpenAI roles: user / assistant / system
    private String mapRole(String role) {
        return "ASSISTANT".equalsIgnoreCase(role) ? "assistant" : "user";
    }
}
