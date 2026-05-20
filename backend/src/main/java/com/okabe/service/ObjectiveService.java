package com.okabe.service;

import com.okabe.dto.request.KeyResultRequest;
import com.okabe.dto.request.ObjectiveRequest;
import com.okabe.dto.response.ObjectiveResponse;
import com.okabe.dto.response.KeyResultResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface ObjectiveService {

    ObjectiveResponse createObjective(Long workspaceId, ObjectiveRequest request, UserPrincipal currentUser);

    List<ObjectiveResponse> getObjectivesByQuarter(Long workspaceId, String quarter, UserPrincipal currentUser);

    ObjectiveResponse getObjective(Long id, UserPrincipal currentUser);

    KeyResultResponse addKeyResult(Long objectiveId, KeyResultRequest request, UserPrincipal currentUser);

    void linkCardsToKeyResult(Long keyResultId, List<Long> cardIds, UserPrincipal currentUser);

    void deleteObjective(Long id, UserPrincipal currentUser);

    ObjectiveResponse recalculateProgress(Long objectiveId, UserPrincipal currentUser);

    List<ObjectiveResponse> getOkrTree(Long workspaceId, String quarter, UserPrincipal currentUser);
}
