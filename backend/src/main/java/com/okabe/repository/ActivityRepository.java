package com.okabe.repository;

import com.okabe.entity.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    // Tìm hoạt động theo cardId, kèm user để tránh LazyInitializationException, sắp xếp theo thời gian tạo giảm dần
    @Query("SELECT a FROM Activity a JOIN FETCH a.user WHERE a.card.id = :cardId ORDER BY a.createdAt DESC")
    List<Activity> findByCardIdOrderByCreatedAtDesc(@Param("cardId") Long cardId, Pageable pageable);

    // Tìm hoạt động theo boardId, kèm user và card, sắp xếp theo thời gian tạo giảm dần
    @Query("SELECT a FROM Activity a JOIN FETCH a.user JOIN FETCH a.card WHERE a.card.taskList.board.id = :boardId ORDER BY a.createdAt DESC")
    List<Activity> findByCardTaskListBoardIdOrderByCreatedAtDesc(@Param("boardId") Long boardId, Pageable pageable);

    // Tìm hoạt động của user trong workspace theo khoảng thời gian, kèm user và card
    @Query("SELECT a FROM Activity a JOIN FETCH a.user JOIN FETCH a.card WHERE a.user.id = :userId AND a.card.taskList.board.workspace.id = :workspaceId AND a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    List<Activity> findByUserAndWorkspaceAndDateRange(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Tìm tất cả hoạt động trong workspace theo khoảng thời gian, kèm user và card
    @Query("SELECT a FROM Activity a JOIN FETCH a.user JOIN FETCH a.card WHERE a.card.taskList.board.workspace.id = :workspaceId AND a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    List<Activity> findByWorkspaceAndDateRange(@Param("workspaceId") Long workspaceId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
