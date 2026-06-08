package com.okabe.repository;

import com.okabe.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskListRepository extends JpaRepository<TaskList, Long> {

    // Tìm danh sách chưa lưu trữ trong board, sắp xếp theo vị trí tăng dần
    List<TaskList> findByBoardIdAndIsArchivedFalseOrderByPositionAsc(Long boardId);

    // Tìm danh sách đã lưu trữ trong board, sắp xếp theo vị trí tăng dần
    List<TaskList> findByBoardIdAndIsArchivedTrueOrderByPositionAsc(Long boardId);

    // Đếm số danh sách chưa lưu trữ trong board
    int countByBoardIdAndIsArchivedFalse(Long boardId);

    // Tìm danh sách cuối cùng (vị trí lớn nhất) chưa lưu trữ trong board
    TaskList findTopByBoardIdAndIsArchivedFalseOrderByPositionDesc(Long boardId);
}
