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

    List<Board> findByWorkspaceIdAndIsArchivedFalseOrderByPositionAscCreatedAtAsc(Long workspaceId);

    List<Board> findByWorkspaceIdAndIsArchivedTrueOrderByPositionAscCreatedAtAsc(Long workspaceId);

    List<Board> findByWorkspaceIdOrderByPositionAscCreatedAtAsc(Long workspaceId);

    Board findTopByWorkspaceIdAndIsArchivedFalseOrderByPositionDesc(Long workspaceId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Board b LEFT JOIN FETCH b.workspace w WHERE b.publicToken = :publicToken")
    Optional<Board> findByPublicToken(@org.springframework.data.repository.query.Param("publicToken") String publicToken);
}
