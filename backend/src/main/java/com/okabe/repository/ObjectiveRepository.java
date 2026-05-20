package com.okabe.repository;

import com.okabe.entity.Objective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjectiveRepository extends JpaRepository<Objective, Long> {

    List<Objective> findByWorkspaceId(Long workspaceId);

    List<Objective> findByWorkspaceIdAndQuarterOrderByCreatedAtDesc(Long workspaceId, String quarter);
}
