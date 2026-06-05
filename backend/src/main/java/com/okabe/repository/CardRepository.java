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

    @Query("SELECT c FROM Card c JOIN FETCH c.taskList LEFT JOIN FETCH c.members WHERE c.taskList.board.id = :boardId AND c.isArchived = false")
    List<Card> findByTaskListBoardIdAndIsArchivedFalse(@Param("boardId") Long boardId);

    @Query("SELECT c FROM Card c JOIN c.members m WHERE m.id = :userId AND c.taskList.board.workspace.id = :workspaceId AND c.dueDate < :now AND c.isArchived = false")
    List<Card> findOverdueByUserAndWorkspace(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId, @Param("now") LocalDateTime now);

    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false")
    List<Card> findByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id IN :workspaceIds AND c.isArchived = false")
    List<Card> findByWorkspaceIdIn(@Param("workspaceIds") List<Long> workspaceIds);

    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false AND c.updatedAt < :threshold")
    List<Card> findStaleCardsByWorkspace(@Param("workspaceId") Long workspaceId, @Param("threshold") LocalDateTime threshold);

    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false AND c.dueDate IS NOT NULL AND c.dueDate BETWEEN :now AND :end")
    List<Card> findDueSoonCardsByWorkspace(@Param("workspaceId") Long workspaceId, @Param("now") LocalDateTime now, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM Card c JOIN FETCH c.taskList LEFT JOIN FETCH c.members WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false")
    List<Card> findAllWithMembersByWorkspace(@Param("workspaceId") Long workspaceId);

    @Query(value = "SELECT * FROM cards WHERE parent_ids IS NOT NULL AND JSON_CONTAINS(parent_ids, CAST(:cardId AS CHAR))", nativeQuery = true)
    List<Card> findDependentCards(@Param("cardId") Long cardId);

    @Query(value = """
            SELECT cm.user_id, DATE(c.due_date) AS work_date,
                   COUNT(DISTINCT c.id) AS card_count,
                   COALESCE(SUM(c.estimated_hours), COUNT(DISTINCT c.id) * 2) AS total_hours
            FROM card_members cm
            JOIN cards c ON cm.card_id = c.id
            JOIN lists l ON c.list_id = l.id
            JOIN boards b ON l.board_id = b.id
            WHERE b.workspace_id = :workspaceId
              AND c.due_date >= :from
              AND c.due_date < :to
              AND c.is_archived = false
            GROUP BY cm.user_id, DATE(c.due_date)
            ORDER BY cm.user_id, work_date
            """, nativeQuery = true)
    List<Object[]> findWorkloadByWorkspaceAndDateRange(
            @Param("workspaceId") Long workspaceId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
