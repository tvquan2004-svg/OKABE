package com.okabe.repository;

import com.okabe.entity.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {
    // Tìm lời mời theo token
    Optional<WorkspaceInvitation> findByToken(String token);
    // Tìm lời mời theo email và trạng thái
    List<WorkspaceInvitation> findByEmailAndStatus(String email, String status);
    // Kiểm tra lời mời đã tồn tại theo workspace, email và trạng thái hay chưa
    boolean existsByWorkspaceIdAndEmailAndStatus(Long workspaceId, String email, String status);
}
