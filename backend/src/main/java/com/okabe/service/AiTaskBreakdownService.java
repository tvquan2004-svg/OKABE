package com.okabe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.okabe.dto.response.SubtaskSuggestion;
import com.okabe.entity.Card;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTaskBreakdownService {

    private final GeminiProvider geminiProvider;
    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý phân rã công việc. Nhiệm vụ của bạn là chia một task lớn thành các subtask nhỏ hơn, có thể hành động được, kèm ước lượng thời gian bằng giờ.
        
        Yêu cầu:
        - Chia task thành 3-8 subtask cụ thể
        - Mỗi subtask phải có estimatedHours (số giờ ước lượng, kiểu double)
        - Ước lượng thực tế, không phóng đại
        - Tiếng Việt cho tiêu đề subtask
        
        Trả về STRICT định dạng JSON array, KHÔNG có markdown, KHÔNG có giải thích:
        [{"title": "Tên subtask", "estimatedHours": 2.0}]
        """;

    public List<SubtaskSuggestion> breakdownTask(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        String userMessage = "Task: " + card.getTitle()
                + "\nMô tả: " + (card.getDescription() != null ? card.getDescription() : "Không có mô tả");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", userMessage));

        log.info("[AI-BREAKDOWN] Calling Groq for card: {} ({})", card.getId(), card.getTitle());
        String response = geminiProvider.generateContent(SYSTEM_PROMPT, messages);
        log.info("[AI-BREAKDOWN] Groq response: {}", response);

        return parseSubtasks(response);
    }

    private List<SubtaskSuggestion> parseSubtasks(String response) {
        String json = extractJsonArray(response);
        if (json == null) {
            log.warn("[AI-BREAKDOWN] No JSON array found in response, returning empty list");
            return List.of();
        }

        CollectionType collectionType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, SubtaskSuggestion.class);
        try {
            List<SubtaskSuggestion> suggestions = objectMapper.readValue(json, collectionType);
            log.info("[AI-BREAKDOWN] Parsed {} subtasks", suggestions.size());
            return suggestions;
        } catch (JsonProcessingException e) {
            log.error("[AI-BREAKDOWN] Failed to parse JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractJsonArray(String text) {
        if (text == null) return null;

        Pattern pattern = Pattern.compile("\\[.*\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String json = matcher.group();
            json = json.replace("```json", "").replace("```", "").trim();
            return json;
        }
        return null;
    }
}
