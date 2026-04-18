package com.okabe.repository;

import com.okabe.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findByWorkspaceIdAndIsArchivedFalseOrderByPositionAscCreatedAtAsc(Long workspaceId);

    List<Board> findByWorkspaceIdOrderByPositionAscCreatedAtAsc(Long workspaceId);

    Board findTopByWorkspaceIdAndIsArchivedFalseOrderByPositionDesc(Long workspaceId);
}
