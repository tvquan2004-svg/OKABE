package com.okabe.service;

import com.okabe.dto.request.CreateListRequest;
import com.okabe.dto.request.ReorderListRequest;
import com.okabe.dto.request.UpdateListRequest;
import com.okabe.dto.response.ListResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface TaskListService {

    // Lấy danh sách các list trong board
    List<ListResponse> getListsByBoard(Long boardId, UserPrincipal currentUser);

    // Tạo list mới trong board
    ListResponse createList(Long boardId, CreateListRequest request, UserPrincipal currentUser);

    // Cập nhật thông tin list
    ListResponse updateList(Long listId, UpdateListRequest request, UserPrincipal currentUser);

    // Sắp xếp lại thứ tự các list trong board
    void reorderLists(Long boardId, ReorderListRequest request, UserPrincipal currentUser);

    // Xoá list
    void deleteList(Long listId, UserPrincipal currentUser);

    // Lưu trữ list
    ListResponse archiveList(Long listId, UserPrincipal currentUser);

    // Khôi phục list đã lưu trữ
    ListResponse restoreList(Long listId, UserPrincipal currentUser);

    // Lấy danh sách list đã lưu trữ
    List<ListResponse> getArchivedLists(Long boardId, UserPrincipal currentUser);
}
