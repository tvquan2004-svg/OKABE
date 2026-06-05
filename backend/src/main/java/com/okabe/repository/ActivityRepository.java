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
    @Query("SELECT a FROM Activity a JOIN FETCH a.user WHERE a.card.id = :cardId ORDER BY a.createdAt DESC")
    List<Activity> findByCardIdOrderByCreatedAtDesc(@Param("cardId") Long cardId, Pageable pageable);

    @Query("SELECT a FROM Activity a JOIN FETCH a.user JOIN FETCH a.card WHERE a.card.taskList.board.id = :boardId ORDER BY a.createdAt DESC")
    List<Activity> findByCardTaskListBoardIdOrderByCreatedAtDesc(@Param("boardId") Long boardId, Pageable pageable);

    @Query("SELECT a FROM Activity a JOIN FETCH a.user JOIN FETCH a.card WHERE a.user.id = :userId AND a.card.taskList.board.workspace.id = :workspaceId AND a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    List<Activity> findByUserAndWorkspaceAndDateRange(@Param("userId") Long userId, @Param("workspaceId") Long workspaceId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Activity a JOIN FETCH a.user JOIN FETCH a.card WHERE a.card.taskList.board.workspace.id = :workspaceId AND a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    List<Activity> findByWorkspaceAndDateRange(@Param("workspaceId") Long workspaceId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
