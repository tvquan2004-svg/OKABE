package com.okabe.service;

import com.okabe.dto.request.*;
import com.okabe.dto.response.*;
import com.okabe.security.UserPrincipal;

import org.springframework.data.domain.Page;

import java.util.List;

public interface CardService {

    // Tìm kiếm thẻ trong board theo nhiều tiêu chí (keyword, assignee, label, priority, ngày, quá hạn)
    Page<CardResponse> searchCards(Long boardId, CardSearchRequest request, UserPrincipal currentUser);

    // Lấy thông tin thẻ theo id
    CardResponse getCard(Long cardId, UserPrincipal currentUser);

    // Tạo thẻ mới trong danh sách
    CardResponse createCard(Long listId, CreateCardRequest request, UserPrincipal currentUser);

    // Cập nhật thông tin thẻ
    CardResponse updateCard(Long cardId, UpdateCardRequest request, UserPrincipal currentUser);
    
    // Di chuyển thẻ sang danh sách khác (hoặc thay đổi vị trí)
    CardResponse moveCard(Long cardId, MoveCardRequest request, UserPrincipal currentUser);

    // Xoá thẻ
    void deleteCard(Long cardId, UserPrincipal currentUser);

    // Tạo checklist mới trong thẻ
    ChecklistResponse createChecklist(Long cardId, CreateChecklistRequest request, UserPrincipal currentUser);
    // Cập nhật checklist
    ChecklistResponse updateChecklist(Long checklistId, CreateChecklistRequest request, UserPrincipal currentUser);
    // Xoá checklist
    void deleteChecklist(Long checklistId, UserPrincipal currentUser);
    // Tạo item mới trong checklist
    ChecklistItemResponse createChecklistItem(Long checklistId, CreateChecklistItemRequest request, UserPrincipal currentUser);
    // Cập nhật item trong checklist
    ChecklistItemResponse updateChecklistItem(Long itemId, UpdateChecklistItemRequest request, UserPrincipal currentUser);
    // Xoá item trong checklist
    void deleteChecklistItem(Long itemId, UserPrincipal currentUser);
    
    // Tạo nhãn mới trong board
    LabelResponse createLabel(Long boardId, CreateLabelRequest request, UserPrincipal currentUser);
    // Lấy danh sách nhãn của board
    List<LabelResponse> getBoardLabels(Long boardId, UserPrincipal currentUser);
    // Cập nhật nhãn
    LabelResponse updateLabel(Long labelId, UpdateLabelRequest request, UserPrincipal currentUser);
    // Xoá nhãn
    void deleteLabel(Long labelId, UserPrincipal currentUser);
    // Thêm nhãn vào thẻ
    void addLabelToCard(Long cardId, Long labelId, UserPrincipal currentUser);
    // Xoá nhãn khỏi thẻ
    void removeLabelFromCard(Long cardId, Long labelId, UserPrincipal currentUser);

    // Gán thành viên vào thẻ
    void assignMember(Long cardId, Long userId, UserPrincipal currentUser);
    // Huỷ gán thành viên khỏi thẻ
    void unassignMember(Long cardId, Long userId, UserPrincipal currentUser);

    // Lưu trữ thẻ
    CardResponse archiveCard(Long cardId, UserPrincipal currentUser);

    // Khôi phục thẻ đã lưu trữ
    CardResponse restoreCard(Long cardId, UserPrincipal currentUser);

    // Lấy danh sách thẻ đã lưu trữ trong board (có phân trang)
    Page<CardResponse> getArchivedCards(Long boardId, int page, int size, UserPrincipal currentUser);

    // Lấy danh sách thẻ trong workspace (dùng để chọn thẻ)
    List<CardSelectionResponse> getWorkspaceCards(Long workspaceId, UserPrincipal currentUser);

    // Lấy đồ thị phụ thuộc của thẻ
    DependencyGraphResponse getDependencyGraph(Long cardId, UserPrincipal currentUser);

    // Thêm thẻ phụ thuộc
    void addDependencies(Long cardId, CardDependencyRequest request, UserPrincipal currentUser);

    // Xoá thẻ phụ thuộc
    void removeDependency(Long cardId, Long parentCardId, UserPrincipal currentUser);
}
