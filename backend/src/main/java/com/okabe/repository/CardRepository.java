package com.okabe.repository;

import com.okabe.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(Long listId);

    int countByTaskListIdAndIsArchivedFalse(Long listId);
}
