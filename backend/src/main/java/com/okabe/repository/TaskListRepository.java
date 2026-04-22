package com.okabe.repository;

import com.okabe.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {

    List<TaskList> findByBoardIdAndIsArchivedFalseOrderByPositionAsc(Long boardId);

    List<TaskList> findByBoardIdAndIsArchivedTrueOrderByPositionAsc(Long boardId);

    int countByBoardIdAndIsArchivedFalse(Long boardId);

    TaskList findTopByBoardIdAndIsArchivedFalseOrderByPositionDesc(Long boardId);
}
