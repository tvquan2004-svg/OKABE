package com.okabe.repository;

import com.okabe.entity.DismissedSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DismissedSuggestionRepository extends JpaRepository<DismissedSuggestion, Long> {

    // Tìm gợi ý đã bỏ qua của user trong workspace
    List<DismissedSuggestion> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);

    // Kiểm tra gợi ý đã bị bỏ qua hay chưa dựa trên user, workspace, type và cardId
    boolean existsByUserIdAndWorkspaceIdAndTypeAndCardId(Long userId, Long workspaceId, String type, Long cardId);
}
