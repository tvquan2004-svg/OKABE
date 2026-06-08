package com.okabe.repository;

import com.okabe.entity.BoardTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardTemplateRepository extends JpaRepository<BoardTemplate, Long> {
    
    // Tìm template hệ thống hoặc template thuộc workspace cụ thể
    @Query("SELECT t FROM BoardTemplate t WHERE t.isSystem = true OR (:workspaceId IS NOT NULL AND t.workspace IS NOT NULL AND t.workspace.id = :workspaceId)")
    List<BoardTemplate> findAllSystemOrByWorkspace(@Param("workspaceId") Long workspaceId);

    // Tìm tất cả template hệ thống
    List<BoardTemplate> findAllByIsSystemTrue();

    // Kiểm tra template hệ thống đã tồn tại theo tên hay chưa
    boolean existsByNameAndIsSystemTrue(String name);
}
