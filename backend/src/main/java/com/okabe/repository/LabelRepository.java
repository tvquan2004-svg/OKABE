package com.okabe.repository;

import com.okabe.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    // Tìm tất cả nhãn của board
    List<Label> findByBoardId(Long boardId);
}
