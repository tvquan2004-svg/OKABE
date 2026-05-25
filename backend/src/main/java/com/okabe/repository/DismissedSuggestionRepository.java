package com.okabe.repository;

import com.okabe.entity.DismissedSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DismissedSuggestionRepository extends JpaRepository<DismissedSuggestion, Long> {

    List<DismissedSuggestion> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);

    boolean existsByUserIdAndWorkspaceIdAndTypeAndCardId(Long userId, Long workspaceId, String type, Long cardId);
}
