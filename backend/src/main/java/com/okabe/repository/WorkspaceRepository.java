package com.okabe.repository;

import com.okabe.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE EXISTS (SELECT 1 FROM WorkspaceMember wm WHERE wm.workspaceId = w.id AND wm.userId = :userId) ORDER BY w.createdAt DESC")
    List<Workspace> findAllByMemberUserId(@Param("userId") Long userId);

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Workspace> findByIdIn(List<Long> ids);
}
