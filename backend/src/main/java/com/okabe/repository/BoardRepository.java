package com.okabe.repository;

import com.okabe.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // Tìm tất cả board chưa lưu trữ trong workspace, sắp xếp theo vị trí và thời gian tạo
    List<Board> findByWorkspaceIdAndIsArchivedFalseOrderByPositionAscCreatedAtAsc(Long workspaceId);

    // Tìm tất cả board đã lưu trữ trong workspace, sắp xếp theo vị trí và thời gian tạo
    List<Board> findByWorkspaceIdAndIsArchivedTrueOrderByPositionAscCreatedAtAsc(Long workspaceId);

    // Tìm tất cả board trong workspace (không phân biệt archived), sắp xếp theo vị trí và thời gian tạo
    List<Board> findByWorkspaceIdOrderByPositionAscCreatedAtAsc(Long workspaceId);

    // Tìm board cuối cùng (vị trí lớn nhất) chưa lưu trữ trong workspace
    Board findTopByWorkspaceIdAndIsArchivedFalseOrderByPositionDesc(Long workspaceId);

    // Đếm số lượng board trong workspace
    long countByWorkspaceId(Long workspaceId);

    // Tìm board theo publicToken, kèm workspace để tránh LazyInitializationException
    @org.springframework.data.jpa.repository.Query("SELECT b FROM Board b LEFT JOIN FETCH b.workspace w WHERE b.publicToken = :publicToken")
    Optional<Board> findByPublicToken(@org.springframework.data.repository.query.Param("publicToken") String publicToken);

    // Tìm board theo danh sách workspaceIds, kèm workspace
    @org.springframework.data.jpa.repository.Query("SELECT b FROM Board b JOIN FETCH b.workspace WHERE b.workspace.id IN :workspaceIds")
    List<Board> findByWorkspaceIdIn(@org.springframework.data.repository.query.Param("workspaceIds") List<Long> workspaceIds);
}
