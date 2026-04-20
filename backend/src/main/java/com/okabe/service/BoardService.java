package com.okabe.service;

import com.okabe.dto.request.CreateBoardRequest;
import com.okabe.dto.request.ReorderBoardRequest;
import com.okabe.dto.request.UpdateBoardRequest;
import com.okabe.dto.response.BoardResponse;
import com.okabe.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BoardService {

    List<BoardResponse> getBoardsByWorkspace(Long workspaceId, UserPrincipal currentUser);

    BoardResponse getBoard(Long boardId, UserPrincipal currentUser);

    BoardResponse createBoard(Long workspaceId, CreateBoardRequest request, UserPrincipal currentUser);

    BoardResponse updateBoard(Long boardId, UpdateBoardRequest request, UserPrincipal currentUser);

    void reorderBoards(Long workspaceId, ReorderBoardRequest request, UserPrincipal currentUser);

    void deleteBoard(Long boardId, UserPrincipal currentUser);

    BoardResponse updateBackground(Long boardId, String type, String colorValue, MultipartFile file, UserPrincipal currentUser);
}
