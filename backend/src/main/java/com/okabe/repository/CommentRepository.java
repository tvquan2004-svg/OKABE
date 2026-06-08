package com.okabe.repository;

import com.okabe.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // Tìm bình luận theo cardId, sắp xếp theo thời gian tạo tăng dần (có phân trang)
    Page<Comment> findByCardIdOrderByCreatedAtAsc(Long cardId, Pageable pageable);

    // Tìm bình luận cần hỗ trợ (chứa từ khóa help/blocked/cần giúp/bị chặn/hỗ trợ/cần gấp) trong workspace theo khoảng thời gian, kèm card
    @Query("SELECT c FROM Comment c JOIN FETCH c.card WHERE c.card.taskList.board.workspace.id = :workspaceId AND c.createdAt BETWEEN :from AND :to AND (LOWER(c.content) LIKE '%help%' OR LOWER(c.content) LIKE '%blocked%' OR LOWER(c.content) LIKE '%cần giúp%' OR LOWER(c.content) LIKE '%bị chặn%' OR LOWER(c.content) LIKE '%hỗ trợ%' OR LOWER(c.content) LIKE '%cần gấp%')")
    List<Comment> findHelpCommentsByWorkspaceAndDateRange(@Param("workspaceId") Long workspaceId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
