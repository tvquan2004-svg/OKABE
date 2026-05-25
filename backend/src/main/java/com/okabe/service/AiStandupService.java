package com.okabe.service;

import com.okabe.dto.response.StandupSummary;
import com.okabe.entity.Activity;
import com.okabe.entity.Card;
import com.okabe.entity.Comment;
import com.okabe.entity.User;
import com.okabe.entity.WorkspaceMember;
import com.okabe.repository.ActivityRepository;
import com.okabe.repository.CardRepository;
import com.okabe.repository.CommentRepository;
import com.okabe.repository.UserRepository;
import com.okabe.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiStandupService {

    private final ActivityRepository activityRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CommentRepository commentRepository;
    private final GeminiProvider geminiProvider;

    private static final String SYSTEM_PROMPT = """
        You are a daily standup assistant. Given a list of user activities for the day, summarize them into 3 sections.

        Rules:
        - "done": what the user completed (cards moved, checklists done, completed tasks)
        - "inProgress": what they are working on (cards updated today, cards in progress)
        - "blocked": any blockers, overdue items, or requests for help

        Write in Vietnamese, be concise (2-3 bullet points per section).
        If a section has no items, write "Không có".
        Return ONLY a JSON object with keys: done, inProgress, blocked
        Example: {"done": "- Sửa lỗi đăng nhập\\n- Hoàn thành module JWT", "inProgress": "- Đang làm tính năng search", "blocked": "- Chờ review code từ @nam"}
        """;

    @Transactional(readOnly = true)
    public StandupSummary generateStandup(Long userId, Long workspaceId, LocalDate date) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("[AI-STANDUP] User {} not found", userId);
            return emptySummary(userId, "Unknown", null, date);
        }

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);
        List<Activity> activities = activityRepository.findByUserAndWorkspaceAndDateRange(userId, workspaceId, from, to);

        // Overdue cards
        List<Card> overdueCards = cardRepository.findOverdueByUserAndWorkspace(userId, workspaceId, LocalDateTime.now());

        // Help comments today in this workspace
        List<Comment> helpComments = commentRepository.findHelpCommentsByWorkspaceAndDateRange(workspaceId, from, to);

        if (activities.isEmpty() && overdueCards.isEmpty() && helpComments.isEmpty()) {
            log.info("[AI-STANDUP] No activities for user {} on {}", userId, date);
            return emptySummary(userId, user.getUsername(), user.getAvatarUrl(), date);
        }

        String rawData = classifyActivities(activities, overdueCards, helpComments);
        String aiSummary = summarizeWithGroq(rawData);

        return new StandupSummary(userId, user.getUsername(), user.getAvatarUrl(), date, aiSummary, "", "");
    }

    private String classifyActivities(List<Activity> activities, List<Card> overdueCards, List<Comment> helpComments) {
        List<String> doneItems = new ArrayList<>();
        List<String> inProgressItems = new ArrayList<>();
        List<String> blockedItems = new ArrayList<>();

        // Overdue cards → Cần hỗ trợ
        for (Card c : overdueCards) {
            blockedItems.add("[QUÁ HẠN] " + c.getTitle() + ": Đến hạn " + c.getDueDate());
        }

        // Help comments → Cần hỗ trợ
        for (Comment c : helpComments) {
            String cardTitle = c.getCard() != null ? c.getCard().getTitle() : "Unknown";
            blockedItems.add("[CẦN GIÚP] " + cardTitle + ": \"" + c.getContent() + "\"");
        }

        // Activities classification
        for (Activity a : activities) {
            String type = a.getActionType();
            String desc = a.getDescription() != null ? a.getDescription() : "";
            String cardTitle = a.getCard() != null ? a.getCard().getTitle() : "Unknown";

            if (type.contains("COMPLETE") || type.contains("DONE") || type.contains("CHECKLIST_DONE")
                    || type.contains("MOVE") || type.contains("UPDATE_CARD")) {
                doneItems.add("[" + type + "] " + cardTitle + (desc.isBlank() ? "" : ": " + desc));
            } else if (type.contains("BLOCKED") || type.contains("HELP")
                    || desc.toLowerCase().contains("help") || desc.toLowerCase().contains("blocked")) {
                blockedItems.add("[" + type + "] " + cardTitle + (desc.isBlank() ? "" : ": " + desc));
            } else {
                inProgressItems.add("[" + type + "] " + cardTitle + (desc.isBlank() ? "" : ": " + desc));
            }
        }

        return "=== ĐÃ LÀM ===\n" + String.join("\n", doneItems)
                + "\n\n=== ĐANG LÀM ===\n" + String.join("\n", inProgressItems)
                + "\n\n=== CẦN HỖ TRỢ ===\n" + String.join("\n", blockedItems);
    }

    private String summarizeWithGroq(String rawData) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", "Activities today:\n" + rawData));
            String response = geminiProvider.generateContent(SYSTEM_PROMPT, messages);
            log.debug("[AI-STANDUP] Groq response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("[AI-STANDUP] Groq call failed: {}", e.getMessage());
            return "{\"done\":\"Không thể tạo tổng kết\",\"inProgress\":\"Không có\",\"blocked\":\"Không có\"}";
        }
    }

    @Transactional(readOnly = true)
    public List<StandupSummary> generateWorkspaceStandup(Long workspaceId, LocalDate date) {
        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);
        return members.stream()
                .map(m -> generateStandup(m.getUserId(), workspaceId, date))
                .collect(Collectors.toList());
    }

    private StandupSummary emptySummary(Long userId, String userName, String avatarUrl, LocalDate date) {
        return new StandupSummary(userId, userName, avatarUrl, date, "Không có hoạt động", "Không có", "Không có");
    }
}
