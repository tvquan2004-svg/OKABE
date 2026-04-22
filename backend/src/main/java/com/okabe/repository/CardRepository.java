package com.okabe.repository;

import com.okabe.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
