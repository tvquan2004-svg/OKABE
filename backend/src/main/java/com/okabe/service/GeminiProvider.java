package com.okabe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for Groq API (OpenAI-compatible format).
 * Free tier: 14,400 req/day, no credit card required.
 * Docs: https://console.groq.com/docs/openai
 */
@Service
@Slf4j
public class GeminiProvider {

    private final RestClient restClient;

    @Value("${app.ai.groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${app.ai.groq.max-tokens:1000}")
    private int maxTokens;

    public GeminiProvider(@Value("${app.ai.groq.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Calls Groq Chat Completion API (OpenAI-compatible).
     *
     * @param systemPrompt System instruction for the AI
     * @param messages     Conversation history [{role, content}, ...]
     * @return AI generated text reply
     */
    public String generateContent(String systemPrompt, List<Map<String, String>> messages) {
        // Build message list: system prompt first, then conversation history
        List<Map<String, String>> allMessages = new ArrayList<>();
        allMessages.add(Map.of("role", "system", "content", systemPrompt));

        messages.forEach(m -> allMessages.add(Map.of(
                "role", mapRole(m.get("role")),
                "content", m.get("content")
        )));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", allMessages,
                "max_tokens", maxTokens,
                "temperature", 0.7
        );

        try {
            log.debug("Calling Groq API with model: {}", model);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return extractText(response);

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Groq API error: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) {
            return "Xin lỗi, không nhận được phản hồi từ AI.";
        }
        try {
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("Failed to parse Groq response: {} | Body: {}", e.getMessage(), response);
            return "Xin lỗi, tôi gặp lỗi khi xử lý phản hồi.";
        }
    }

    /** Groq uses OpenAI roles: user / assistant / system */
    private String mapRole(String role) {
        return "ASSISTANT".equalsIgnoreCase(role) ? "assistant" : "user";
    }
}
