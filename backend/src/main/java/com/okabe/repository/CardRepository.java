package com.okabe.repository;

import com.okabe.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    List<Card> findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(Long listId);

    List<Card> findByTaskListIdAndIsArchivedTrueOrderByPositionAsc(Long listId);

    int countByTaskListIdAndIsArchivedFalse(Long listId);

    Card findTopByTaskListIdAndIsArchivedFalseOrderByPositionDesc(Long listId);

    Page<Card> findByTaskListBoardIdAndIsArchivedTrueOrderByUpdatedAtDesc(Long boardId, Pageable pageable);

    List<Card> findByTaskListId(Long listId);

    List<Card> findByIsArchivedFalseAndDueDateBeforeAndNotificationSentFalse(LocalDateTime now);

    List<Card> findByIsArchivedFalseAndDueDateBetweenAndNotificationSentFalse(LocalDateTime start, LocalDateTime end);

    List<Card> findByTaskListBoardIdAndIsArchivedFalse(Long boardId);

    @Query("SELECT c FROM Card c JOIN c.members m WHERE m.id = :userId AND c.taskList.board.workspace.id = :workspaceId AND c.dueDate < :now AND c.isArchived = false")
    List<Card> findOverdueByUserAndWorkspace(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId, @Param("now") LocalDateTime now);

    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false")
    List<Card> findByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false AND c.updatedAt < :threshold")
    List<Card> findStaleCardsByWorkspace(@Param("workspaceId") Long workspaceId, @Param("threshold") LocalDateTime threshold);

    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false AND c.dueDate IS NOT NULL AND c.dueDate BETWEEN :now AND :end")
    List<Card> findDueSoonCardsByWorkspace(@Param("workspaceId") Long workspaceId, @Param("now") LocalDateTime now, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM Card c LEFT JOIN FETCH c.members WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false")
    List<Card> findAllWithMembersByWorkspace(@Param("workspaceId") Long workspaceId);
}
