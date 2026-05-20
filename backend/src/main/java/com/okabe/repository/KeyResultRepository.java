package com.okabe.repository;

import com.okabe.entity.KeyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeyResultRepository extends JpaRepository<KeyResult, Long> {

    List<KeyResult> findByObjectiveId(Long objectiveId);

    void deleteByObjectiveId(Long objectiveId);
}
