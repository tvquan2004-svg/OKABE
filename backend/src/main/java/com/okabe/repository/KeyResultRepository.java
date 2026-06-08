package com.okabe.repository;

import com.okabe.entity.KeyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeyResultRepository extends JpaRepository<KeyResult, Long> {

    // Tìm tất cả key result của một objective
    List<KeyResult> findByObjectiveId(Long objectiveId);

    // Xoá tất cả key result của một objective
    void deleteByObjectiveId(Long objectiveId);
}
