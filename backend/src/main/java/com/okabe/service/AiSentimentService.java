package com.okabe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okabe.dto.response.SentimentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSentimentService {

    private final GeminiProvider geminiProvider;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are a sentiment analysis assistant. Analyze the sentiment of the given text.

        Rules:
        - Return ONLY a JSON object with keys: sentiment, score, reason
        - sentiment must be one of: POSITIVE, NEUTRAL, NEGATIVE
        - score is a number from 0.0 to 1.0 indicating confidence
        - reason is a short Vietnamese explanation of the analysis

        Examples:
        Text: "Tuyệt vời! Cảm ơn bạn rất nhiều"
        Response: {"sentiment": "POSITIVE", "score": 0.95, "reason": "Thể hiện sự cảm kích và hài lòng"}

        Text: "Cái này không đúng như tôi yêu cầu"
        Response: {"sentiment": "NEGATIVE", "score": 0.75, "reason": "Thể hiện sự không hài lòng"}

        Text: "Tôi sẽ xem lại sau"
        Response: {"sentiment": "NEUTRAL", "score": 0.6, "reason": "Không thể hiện cảm xúc rõ ràng"}
        """;

    public SentimentResult analyzeSentiment(String text) {
        try {
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", "Text: " + text)
            );
            String response = geminiProvider.generateContent(SYSTEM_PROMPT, messages);
            log.debug("[AI-SENTIMENT] Groq response: {}", response);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("[AI-SENTIMENT] Analysis failed: {}", e.getMessage());
            return new SentimentResult("NEUTRAL", 0.0, "Không thể phân tích cảm xúc");
        }
    }

    private SentimentResult parseResponse(String json) {
        try {
            return objectMapper.readValue(json, SentimentResult.class);
        } catch (Exception e) {
            log.warn("[AI-SENTIMENT] Failed to parse Groq response: {}", json);
            return new SentimentResult("NEUTRAL", 0.0, "Không thể phân tích cảm xúc");
        }
    }
}