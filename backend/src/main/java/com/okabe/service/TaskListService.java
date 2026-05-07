package com.okabe.service;

import com.okabe.dto.request.CreateListRequest;
import com.okabe.dto.request.ReorderListRequest;
import com.okabe.dto.request.UpdateListRequest;
import com.okabe.dto.response.ListResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface TaskListService {

    List<ListResponse> getListsByBoard(Long boardId, UserPrincipal currentUser);

    ListResponse createList(Long boardId, CreateListRequest request, UserPrincipal currentUser);

    ListResponse updateList(Long listId, UpdateListRequest request, UserPrincipal currentUser);

    void reorderLists(Long boardId, ReorderListRequest request, UserPrincipal currentUser);

    void deleteList(Long listId, UserPrincipal currentUser);

    ListResponse archiveList(Long listId, UserPrincipal currentUser);

    ListResponse restoreList(Long listId, UserPrincipal currentUser);

    List<ListResponse> getArchivedLists(Long boardId, UserPrincipal currentUser);
}
