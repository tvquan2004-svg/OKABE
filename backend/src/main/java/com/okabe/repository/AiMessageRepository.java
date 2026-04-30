package com.okabe.repository;

import com.okabe.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Lấy N messages gần nhất để làm context (tránh overflow context window).
     */
    @Query(value = """
            SELECT * FROM ai_messages
            WHERE conversation_id = :conversationId
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<AiMessage> findRecentMessages(@Param("conversationId") Long conversationId,
                                       @Param("limit") int limit);
}
