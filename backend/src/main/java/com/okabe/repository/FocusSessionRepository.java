package com.okabe.repository;

import com.okabe.entity.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {

    // Tìm phiên tập trung đang hoạt động của user (chưa kết thúc)
    Optional<FocusSession> findByUserIdAndEndedAtIsNull(Long userId);

    // Tìm phiên tập trung của user trong khoảng thời gian, sắp xếp theo thời gian bắt đầu tăng dần
    List<FocusSession> findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(Long userId, LocalDateTime from, LocalDateTime to);

    // Tính tổng số phút tập trung đã hoàn thành của user trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(f.durationMinutes), 0) FROM FocusSession f WHERE f.userId = :userId AND f.completed = true AND f.startedAt BETWEEN :from AND :to")
    int sumCompletedMinutesByUserAndDateRange(@Param("userId") Long userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Tìm các card được tập trung nhiều nhất của user (sắp xếp theo tổng thời gian giảm dần)
    @Query("SELECT f.cardId, COUNT(f), COALESCE(SUM(f.durationMinutes), 0) FROM FocusSession f WHERE f.userId = :userId AND f.completed = true GROUP BY f.cardId ORDER BY SUM(f.durationMinutes) DESC")
    List<Object[]> findTopFocusedCardsByUser(@Param("userId") Long userId);
}
