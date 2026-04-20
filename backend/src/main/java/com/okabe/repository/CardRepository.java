package com.okabe.repository;

import com.okabe.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    List<Card> findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(Long listId);

    int countByTaskListIdAndIsArchivedFalse(Long listId);

    List<Card> findByIsArchivedFalseAndDueDateBeforeAndNotificationSentFalse(LocalDateTime now);

    List<Card> findByIsArchivedFalseAndDueDateBetweenAndNotificationSentFalse(LocalDateTime start, LocalDateTime end);
}
