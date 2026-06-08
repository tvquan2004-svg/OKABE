package com.okabe.service;

import com.okabe.dto.request.KeyResultRequest;
import com.okabe.dto.request.ObjectiveRequest;
import com.okabe.dto.response.ObjectiveResponse;
import com.okabe.dto.response.KeyResultResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface ObjectiveService {

    // Tạo objective mới trong workspace
    ObjectiveResponse createObjective(Long workspaceId, ObjectiveRequest request, UserPrincipal currentUser);

    // Lấy danh sách objective theo quý
    List<ObjectiveResponse> getObjectivesByQuarter(Long workspaceId, String quarter, UserPrincipal currentUser);

    // Lấy thông tin objective theo id
    ObjectiveResponse getObjective(Long id, UserPrincipal currentUser);

    // Thêm key result vào objective
    KeyResultResponse addKeyResult(Long objectiveId, KeyResultRequest request, UserPrincipal currentUser);

    // Liên kết thẻ với key result
    void linkCardsToKeyResult(Long keyResultId, List<Long> cardIds, UserPrincipal currentUser);

    // Xoá objective
    void deleteObjective(Long id, UserPrincipal currentUser);

    // Tính lại tiến độ của objective
    ObjectiveResponse recalculateProgress(Long objectiveId, UserPrincipal currentUser);

    // Lấy cây OKR của workspace theo quý
    List<ObjectiveResponse> getOkrTree(Long workspaceId, String quarter, UserPrincipal currentUser);
}
