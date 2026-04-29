package com.okabe.service;

import com.okabe.repository.AiConversationRepository;
import com.okabe.repository.AiMessageRepository;
import com.okabe.entity.AiMessage;
import com.okabe.entity.AiConversation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds context-aware system prompt data from the database
 * to inject into AI conversations based on the user's current view.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiContextBuilder {

    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Builds a context string for the given user, optionally scoped to a board or workspace.
     */
    public String buildContext(Long userId, Long boardId, Long workspaceId) {
        StringBuilder ctx = new StringBuilder();

        try {
            // Upcoming cards (next 7 days) assigned to user
            List<String> upcoming = jdbcTemplate.queryForList("""
                    SELECT CONCAT(c.title, ' (hạn: ', DATE_FORMAT(c.due_date, '%d/%m/%Y'), ', list: ', l.name, ')')
                    FROM cards c
                    JOIN lists l ON c.list_id = l.id
                    JOIN card_members cm ON c.id = cm.card_id
                    WHERE cm.user_id = ?
                      AND c.is_archived = false
                      AND c.due_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY)
                    ORDER BY c.due_date ASC
                    LIMIT 10
                    """, String.class, userId);

            // Overdue cards assigned to user
            List<String> overdue = jdbcTemplate.queryForList("""
                    SELECT CONCAT(c.title, ' (hạn: ', DATE_FORMAT(c.due_date, '%d/%m/%Y'), ', list: ', l.name, ')')
                    FROM cards c
                    JOIN lists l ON c.list_id = l.id
                    JOIN card_members cm ON c.id = cm.card_id
                    WHERE cm.user_id = ?
                      AND c.is_archived = false
                      AND c.due_date < NOW()
                    ORDER BY c.due_date ASC
                    LIMIT 10
                    """, String.class, userId);

            if (!overdue.isEmpty()) {
                ctx.append("\n## ⚠️ Tasks quá hạn (").append(overdue.size()).append(" tasks):\n");
                overdue.forEach(t -> ctx.append("- ").append(t).append("\n"));
            }

            if (!upcoming.isEmpty()) {
                ctx.append("\n## 📅 Tasks sắp đến hạn trong 7 ngày (").append(upcoming.size()).append(" tasks):\n");
                upcoming.forEach(t -> ctx.append("- ").append(t).append("\n"));
            }

            // Board-specific context
            if (boardId != null) {
                appendBoardContext(ctx, boardId);
            }

        } catch (Exception e) {
            log.warn("Could not build AI context for user {}: {}", userId, e.getMessage());
        }

        return ctx.toString();
    }

    private void appendBoardContext(StringBuilder ctx, Long boardId) {
        try {
            // Board summary: card counts per list
            List<String> listSummaries = jdbcTemplate.queryForList("""
                    SELECT CONCAT(l.name, ': ', COUNT(c.id), ' cards')
                    FROM lists l
                    LEFT JOIN cards c ON l.id = c.list_id AND c.is_archived = false
                    WHERE l.board_id = ? AND l.is_archived = false
                    GROUP BY l.id, l.name, l.position
                    ORDER BY l.position ASC
                    """, String.class, boardId);

            if (!listSummaries.isEmpty()) {
                ctx.append("\n## 📋 Trạng thái board hiện tại:\n");
                listSummaries.forEach(s -> ctx.append("- ").append(s).append("\n"));
            }
        } catch (Exception e) {
            log.warn("Could not build board context for board {}: {}", boardId, e.getMessage());
        }
    }
}
