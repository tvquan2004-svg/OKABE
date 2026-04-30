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
            
            // Workspace members context
            if (workspaceId != null) {
                appendWorkspaceMembers(ctx, workspaceId);
            }

        } catch (Exception e) {
            log.warn("Could not build AI context for user {}: {}", userId, e.getMessage());
        }

        return ctx.toString();
    }

    private void appendBoardContext(StringBuilder ctx, Long boardId) {
        try {
            // Fetch list names and card titles
            List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT l.name as list_name, c.title as card_title
                    FROM lists l
                    LEFT JOIN cards c ON l.id = c.list_id AND c.is_archived = false
                    WHERE l.board_id = ? AND l.is_archived = false
                    ORDER BY l.position ASC, c.position ASC
                    """, boardId);

            if (!rows.isEmpty()) {
                ctx.append("\n## 📋 Trạng thái board hiện tại (Các cột và danh sách Card):\n");
                
                java.util.Map<String, java.util.List<String>> listMap = new java.util.LinkedHashMap<>();
                for (java.util.Map<String, Object> row : rows) {
                    String listName = (String) row.get("list_name");
                    String cardTitle = (String) row.get("card_title");
                    
                    listMap.putIfAbsent(listName, new java.util.ArrayList<>());
                    if (cardTitle != null) {
                        listMap.get(listName).add(cardTitle);
                    }
                }
                
                for (java.util.Map.Entry<String, java.util.List<String>> entry : listMap.entrySet()) {
                    String listName = entry.getKey();
                    java.util.List<String> cards = entry.getValue();
                    if (cards.isEmpty()) {
                        ctx.append("- Cột \"").append(listName).append("\": (Trống)\n");
                    } else {
                        ctx.append("- Cột \"").append(listName).append("\": ").append(String.join(", ", cards)).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not build board context for board {}: {}", boardId, e.getMessage());
        }
    }

    private void appendWorkspaceMembers(StringBuilder ctx, Long workspaceId) {
        if (workspaceId == null) return;
        try {
            List<String> members = jdbcTemplate.queryForList("""
                    SELECT u.username
                    FROM users u
                    JOIN workspace_members wm ON u.id = wm.user_id
                    WHERE wm.workspace_id = ?
                    """, String.class, workspaceId);

            if (!members.isEmpty()) {
                ctx.append("\n## 👥 Thành viên trong Workspace (có thể giao việc):\n");
                ctx.append(String.join(", ", members)).append("\n");
            }
        } catch (Exception e) {
            log.warn("Could not build workspace members context: {}", e.getMessage());
        }
    }
}
