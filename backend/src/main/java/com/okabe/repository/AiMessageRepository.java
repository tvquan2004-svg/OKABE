package com.okabe.repository;

import com.okabe.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    // Tìm tất cả tin nhắn trong cuộc hội thoại, sắp xếp theo thời gian tạo tăng dần
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    // Native query: lấy N tin nhắn gần nhất để làm context (tránh overflow context window)
    @Query(value = """
            SELECT * FROM ai_messages
            WHERE conversation_id = :conversationId
            ORDER BY created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<AiMessage> findRecentMessages(@Param("conversationId") Long conversationId,
                                       @Param("limit") int limit);
}
