package com.okabe.service;

import com.okabe.dto.request.CreateBoardRequest;
import com.okabe.dto.request.ReorderBoardRequest;
import com.okabe.dto.request.UpdateBoardRequest;
import com.okabe.dto.response.BoardPublicDto;
import com.okabe.dto.response.BoardResponse;
import com.okabe.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BoardService {

    // Lấy danh sách board trong workspace
    List<BoardResponse> getBoardsByWorkspace(Long workspaceId, UserPrincipal currentUser);

    // Lấy thông tin board theo id
    BoardResponse getBoard(Long boardId, UserPrincipal currentUser);

    // Tạo board mới trong workspace
    BoardResponse createBoard(Long workspaceId, CreateBoardRequest request, UserPrincipal currentUser);

    // Cập nhật thông tin board
    BoardResponse updateBoard(Long boardId, UpdateBoardRequest request, UserPrincipal currentUser);

    // Sắp xếp lại thứ tự board trong workspace
    void reorderBoards(Long workspaceId, ReorderBoardRequest request, UserPrincipal currentUser);

    // Xoá board
    void deleteBoard(Long boardId, UserPrincipal currentUser);

    // Cập nhật ảnh nền/màu nền của board
    BoardResponse updateBackground(Long boardId, String type, String colorValue, MultipartFile file, UserPrincipal currentUser);

    // Mời thành viên vào board qua email
    void inviteMember(Long boardId, String email, UserPrincipal currentUser);

    // Lưu trữ board
    BoardResponse archiveBoard(Long boardId, UserPrincipal currentUser);

    // Khôi phục board đã lưu trữ
    BoardResponse restoreBoard(Long boardId, UserPrincipal currentUser);

    // Lấy danh sách board đã lưu trữ
    List<BoardResponse> getArchivedBoards(Long workspaceId, UserPrincipal currentUser);

    // Cập nhật trạng thái công khai/riêng tư của board
    BoardResponse updateVisibility(Long boardId, boolean isPublic, UserPrincipal currentUser);

    // Lấy thông tin board công khai bằng token
    BoardPublicDto getPublicBoard(String token);
}
