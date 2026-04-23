package com.okabe.repository;

import com.okabe.entity.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByCardIdOrderByCreatedAtDesc(Long cardId, Pageable pageable);

    List<Activity> findByCardTaskListBoardIdOrderByCreatedAtDesc(Long boardId, Pageable pageable);
}
