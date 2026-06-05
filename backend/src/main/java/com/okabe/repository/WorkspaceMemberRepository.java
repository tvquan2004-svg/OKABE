package com.okabe.repository;

import com.okabe.entity.WorkspaceMember;
import com.okabe.entity.WorkspaceMemberId;
import com.okabe.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {

    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(wm) > 0 FROM WorkspaceMember wm WHERE wm.workspaceId = :workspaceId AND wm.user.email = :email")
    boolean existsByWorkspaceIdAndEmail(@org.springframework.data.repository.query.Param("workspaceId") Long workspaceId, @org.springframework.data.repository.query.Param("email") String email);

    boolean existsByWorkspaceIdAndUserIdAndRoleIn(Long workspaceId, Long userId, List<Role> roles);

    @org.springframework.data.jpa.repository.Query("SELECT wm FROM WorkspaceMember wm JOIN FETCH wm.user WHERE wm.userId = :userId")
    List<WorkspaceMember> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT wm FROM WorkspaceMember wm JOIN FETCH wm.user WHERE wm.workspaceId IN :workspaceIds")
    List<WorkspaceMember> findByWorkspaceIdIn(@org.springframework.data.repository.query.Param("workspaceIds") List<Long> workspaceIds);
}
