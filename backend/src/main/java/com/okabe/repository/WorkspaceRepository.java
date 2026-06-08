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

    // Tìm tất cả workspace mà user là thành viên, kèm owner, sắp xếp theo thời gian tạo giảm dần
    @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE EXISTS (SELECT 1 FROM WorkspaceMember wm WHERE wm.workspaceId = w.id AND wm.userId = :userId) ORDER BY w.createdAt DESC")
    List<Workspace> findAllByMemberUserId(@Param("userId") Long userId);

    // Tìm workspace theo slug
    Optional<Workspace> findBySlug(String slug);

    // Kiểm tra slug đã tồn tại hay chưa
    boolean existsBySlug(String slug);

    // Tìm workspace theo danh sách id
    List<Workspace> findByIdIn(List<Long> ids);
}
