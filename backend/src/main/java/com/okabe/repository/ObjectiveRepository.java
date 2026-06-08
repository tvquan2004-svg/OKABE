package com.okabe.repository;

import com.okabe.entity.Objective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObjectiveRepository extends JpaRepository<Objective, Long> {

    // Tìm tất cả objective của workspace
    List<Objective> findByWorkspaceId(Long workspaceId);

    // Tìm objective của workspace theo quý, sắp xếp theo thời gian tạo giảm dần
    List<Objective> findByWorkspaceIdAndQuarterOrderByCreatedAtDesc(Long workspaceId, String quarter);
}
