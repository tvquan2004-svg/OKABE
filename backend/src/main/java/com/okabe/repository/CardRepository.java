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

    // Tìm thẻ chưa lưu trữ trong danh sách, sắp xếp theo vị trí tăng dần
    List<Card> findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(Long listId);

    // Tìm thẻ đã lưu trữ trong danh sách, sắp xếp theo vị trí tăng dần
    List<Card> findByTaskListIdAndIsArchivedTrueOrderByPositionAsc(Long listId);

    // Đếm số lượng thẻ chưa lưu trữ trong danh sách
    int countByTaskListIdAndIsArchivedFalse(Long listId);

    // Tìm thẻ cuối cùng (vị trí lớn nhất) chưa lưu trữ trong danh sách
    Card findTopByTaskListIdAndIsArchivedFalseOrderByPositionDesc(Long listId);

    // Tìm thẻ đã lưu trữ trong board, sắp xếp theo thời gian cập nhật giảm dần (có phân trang)
    Page<Card> findByTaskListBoardIdAndIsArchivedTrueOrderByUpdatedAtDesc(Long boardId, Pageable pageable);

    // Tìm tất cả thẻ trong danh sách
    List<Card> findByTaskListId(Long listId);

    // Tìm thẻ chưa lưu trữ, quá hạn và chưa gửi thông báo
    List<Card> findByIsArchivedFalseAndDueDateBeforeAndNotificationSentFalse(LocalDateTime now);

    // Tìm thẻ chưa lưu trữ, sắp đến hạn trong khoảng thời gian và chưa gửi thông báo
    List<Card> findByIsArchivedFalseAndDueDateBetweenAndNotificationSentFalse(LocalDateTime start, LocalDateTime end);

    // Tìm thẻ chưa lưu trữ trong board, kèm taskList và members để tránh LazyInitializationException
    @Query("SELECT c FROM Card c JOIN FETCH c.taskList LEFT JOIN FETCH c.members WHERE c.taskList.board.id = :boardId AND c.isArchived = false")
    List<Card> findByTaskListBoardIdAndIsArchivedFalse(@Param("boardId") Long boardId);

    // Tìm thẻ quá hạn của user trong workspace
    @Query("SELECT c FROM Card c JOIN c.members m WHERE m.id = :userId AND c.taskList.board.workspace.id = :workspaceId AND c.dueDate < :now AND c.isArchived = false")
    List<Card> findOverdueByUserAndWorkspace(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId, @Param("now") LocalDateTime now);

    // Tìm tất cả thẻ chưa lưu trữ trong workspace
    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false")
    List<Card> findByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // Tìm thẻ chưa lưu trữ trong danh sách workspace
    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id IN :workspaceIds AND c.isArchived = false")
    List<Card> findByWorkspaceIdIn(@Param("workspaceIds") List<Long> workspaceIds);

    // Tìm thẻ theo từ khóa, kèm taskList + board để tránh N+1, lọc ngay trong database
    @Query("SELECT c FROM Card c JOIN FETCH c.taskList tl JOIN FETCH tl.board WHERE c.taskList.board.workspace.id IN :workspaceIds AND c.isArchived = false AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Card> searchByKeyword(@Param("workspaceIds") List<Long> workspaceIds, @Param("keyword") String keyword);

    // Tìm thẻ chưa được cập nhật trong một khoảng thời gian (thẻ cũ/ngưng trệ)
    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false AND c.updatedAt < :threshold")
    List<Card> findStaleCardsByWorkspace(@Param("workspaceId") Long workspaceId, @Param("threshold") LocalDateTime threshold);

    // Tìm thẻ sắp đến hạn trong workspace trong khoảng thời gian
    @Query("SELECT c FROM Card c WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false AND c.dueDate IS NOT NULL AND c.dueDate BETWEEN :now AND :end")
    List<Card> findDueSoonCardsByWorkspace(@Param("workspaceId") Long workspaceId, @Param("now") LocalDateTime now, @Param("end") LocalDateTime end);

    // Tìm tất cả thẻ trong workspace kèm taskList và members
    @Query("SELECT c FROM Card c JOIN FETCH c.taskList LEFT JOIN FETCH c.members WHERE c.taskList.board.workspace.id = :workspaceId AND c.isArchived = false")
    List<Card> findAllWithMembersByWorkspace(@Param("workspaceId") Long workspaceId);

    // Native query: tìm thẻ phụ thuộc (có chứa cardId trong parent_ids JSON)
    @Query(value = "SELECT * FROM cards WHERE parent_ids IS NOT NULL AND JSON_CONTAINS(parent_ids, CAST(:cardId AS CHAR))", nativeQuery = true)
    List<Card> findDependentCards(@Param("cardId") Long cardId);

    // Native query: tính khối lượng công việc theo user và ngày trong workspace (dùng cho workload heatmap)
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
