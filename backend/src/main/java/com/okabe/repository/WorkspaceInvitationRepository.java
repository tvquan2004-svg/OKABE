package com.okabe.repository;

import com.okabe.entity.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    Optional<WorkspaceInvitation> findByToken(String token);
    List<WorkspaceInvitation> findByEmailAndStatus(String email, String status);
    boolean existsByWorkspaceIdAndEmailAndStatus(Long workspaceId, String email, String status);
}
