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

    Optional<FocusSession> findByUserIdAndEndedAtIsNull(Long userId);

    List<FocusSession> findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(Long userId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(f.durationMinutes), 0) FROM FocusSession f WHERE f.userId = :userId AND f.completed = true AND f.startedAt BETWEEN :from AND :to")
    int sumCompletedMinutesByUserAndDateRange(@Param("userId") Long userId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT f.cardId, COUNT(f), COALESCE(SUM(f.durationMinutes), 0) FROM FocusSession f WHERE f.userId = :userId AND f.completed = true GROUP BY f.cardId ORDER BY SUM(f.durationMinutes) DESC")
    List<Object[]> findTopFocusedCardsByUser(@Param("userId") Long userId);
}
