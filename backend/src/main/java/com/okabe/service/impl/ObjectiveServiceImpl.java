package com.okabe.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okabe.dto.request.KeyResultRequest;
import com.okabe.dto.request.ObjectiveRequest;
import com.okabe.dto.response.KeyResultResponse;
import com.okabe.dto.response.ObjectiveResponse;
import com.okabe.entity.Card;
import com.okabe.entity.Checklist;
import com.okabe.entity.ChecklistItem;
import com.okabe.entity.KeyResult;
import com.okabe.entity.Objective;
import com.okabe.repository.CardRepository;
import com.okabe.repository.KeyResultRepository;
import com.okabe.repository.ObjectiveRepository;
import com.okabe.repository.WorkspaceMemberRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.ObjectiveService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObjectiveServiceImpl implements ObjectiveService {

    private final ObjectiveRepository objectiveRepository;
    private final KeyResultRepository keyResultRepository;
    private final CardRepository cardRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ObjectiveResponse createObjective(Long workspaceId, ObjectiveRequest request, UserPrincipal currentUser) {
        validateMembership(workspaceId, currentUser.getId());

        Objective objective = Objective.builder()
                .workspaceId(workspaceId)
                .title(request.title())
                .description(request.description())
                .quarter(request.quarter())
                .createdBy(currentUser.getId())
                .progress(BigDecimal.ZERO)
                .build();
        objective = objectiveRepository.save(objective);
        return toResponse(objective, List.of());
    }

    @Override
    public List<ObjectiveResponse> getObjectivesByQuarter(Long workspaceId, String quarter, UserPrincipal currentUser) {
        validateMembership(workspaceId, currentUser.getId());

        List<Objective> objectives;
        if (quarter != null && !quarter.isBlank()) {
            objectives = objectiveRepository.findByWorkspaceIdAndQuarterOrderByCreatedAtDesc(workspaceId, quarter);
        } else {
            objectives = objectiveRepository.findByWorkspaceId(workspaceId);
        }

        return objectives.stream()
                .map(obj -> {
                    List<KeyResultResponse> krs = keyResultRepository.findByObjectiveId(obj.getId())
                            .stream().map(this::toKrResponse).collect(Collectors.toList());
                    return toResponse(obj, krs);
                })
                .collect(Collectors.toList());
    }

    @Override
    public ObjectiveResponse getObjective(Long id, UserPrincipal currentUser) {
        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Objective not found"));
        validateMembership(obj.getWorkspaceId(), currentUser.getId());

        List<KeyResultResponse> krs = keyResultRepository.findByObjectiveId(id)
                .stream().map(this::toKrResponse).collect(Collectors.toList());
        return toResponse(obj, krs);
    }

    @Override
    @Transactional
    public KeyResultResponse addKeyResult(Long objectiveId, KeyResultRequest request, UserPrincipal currentUser) {
        Objective obj = objectiveRepository.findById(objectiveId)
                .orElseThrow(() -> new EntityNotFoundException("Objective not found"));
        validateMembership(obj.getWorkspaceId(), currentUser.getId());

        BigDecimal tv = request.targetValue() != null
                ? BigDecimal.valueOf(request.targetValue())
                : null;
        KeyResult kr = KeyResult.builder()
                .objectiveId(objectiveId)
                .title(request.title())
                .targetValue(tv)
                .unit(request.unit() != null ? request.unit() : "percent")
                .currentValue(BigDecimal.ZERO)
                .build();
        kr = keyResultRepository.save(kr);
        return toKrResponse(kr);
    }

    @Override
    @Transactional
    public void linkCardsToKeyResult(Long keyResultId, List<Long> cardIds, UserPrincipal currentUser) {
        KeyResult kr = keyResultRepository.findById(keyResultId)
                .orElseThrow(() -> new EntityNotFoundException("KeyResult not found"));
        Objective obj = objectiveRepository.findById(kr.getObjectiveId())
                .orElseThrow(() -> new EntityNotFoundException("Objective not found"));
        validateMembership(obj.getWorkspaceId(), currentUser.getId());

        try {
            String json = objectMapper.writeValueAsString(cardIds);
            kr.setLinkedCards(json);
            keyResultRepository.save(kr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize card IDs", e);
        }
    }

    @Override
    @Transactional
    public void deleteObjective(Long id, UserPrincipal currentUser) {
        Objective obj = objectiveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Objective not found"));
        validateMembership(obj.getWorkspaceId(), currentUser.getId());
        objectiveRepository.delete(obj);
    }

    @Override
    @Transactional
    public ObjectiveResponse recalculateProgress(Long objectiveId, UserPrincipal currentUser) {
        Objective obj = objectiveRepository.findById(objectiveId)
                .orElseThrow(() -> new EntityNotFoundException("Objective not found"));
        validateMembership(obj.getWorkspaceId(), currentUser.getId());

        List<KeyResult> krs = keyResultRepository.findByObjectiveId(objectiveId);
        if (krs.isEmpty()) {
            obj.setProgress(BigDecimal.ZERO);
            objectiveRepository.save(obj);
            return toResponse(obj, List.of());
        }

        double totalProgress = 0.0;
        for (KeyResult kr : krs) {
            totalProgress += calculateKrProgress(kr);
        }
        double avgProgress = totalProgress / krs.size();
        obj.setProgress(BigDecimal.valueOf(Math.round(avgProgress * 100.0) / 100.0));
        objectiveRepository.save(obj);

        List<KeyResultResponse> krResponses = krs.stream().map(this::toKrResponse).collect(Collectors.toList());
        return toResponse(obj, krResponses);
    }

    @Override
    public List<ObjectiveResponse> getOkrTree(Long workspaceId, String quarter, UserPrincipal currentUser) {
        return getObjectivesByQuarter(workspaceId, quarter, currentUser);
    }

    private double calculateKrProgress(KeyResult kr) {
        List<Long> cardIds = parseCardIds(kr.getLinkedCards());
        if (cardIds == null || cardIds.isEmpty()) return 0.0;

        List<Card> cards = cardRepository.findAllById(cardIds);
        if (cards.isEmpty()) return 0.0;

        double totalCompletion = 0.0;
        int counted = 0;
        for (Card card : cards) {
            double completion = calculateCardCompletion(card);
            totalCompletion += completion;
            counted++;
        }
        return counted > 0 ? totalCompletion / counted : 0.0;
    }

    private double calculateCardCompletion(Card card) {
        if (card.getIsArchived()) return 100.0;

        List<Checklist> checklists = card.getChecklists();
        if (checklists == null || checklists.isEmpty()) {
            if (card.getDescription() != null && !card.getDescription().isBlank()) return 100.0;
            return 0.0;
        }

        long totalItems = checklists.stream()
                .filter(cl -> cl.getItems() != null)
                .flatMap(cl -> cl.getItems().stream())
                .count();
        long completedItems = checklists.stream()
                .filter(cl -> cl.getItems() != null)
                .flatMap(cl -> cl.getItems().stream())
                .filter(item -> Boolean.TRUE.equals(item.getIsCompleted()))
                .count();

        if (totalItems == 0) return 0.0;
        return (double) completedItems / totalItems * 100.0;
    }

    private List<Long> parseCardIds(String linkedCards) {
        if (linkedCards == null || linkedCards.isBlank()) return List.of();
        try {
            return objectMapper.readValue(linkedCards, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse linked_cards JSON: {}", linkedCards, e);
            return List.of();
        }
    }

    private KeyResultResponse toKrResponse(KeyResult kr) {
        double completion = calculateKrProgress(kr);
        return KeyResultResponse.builder()
                .id(kr.getId())
                .title(kr.getTitle())
                .targetValue(kr.getTargetValue())
                .currentValue(BigDecimal.valueOf(completion))
                .unit(kr.getUnit())
                .linkedCards(parseCardIds(kr.getLinkedCards()))
                .createdAt(kr.getCreatedAt())
                .build();
    }

    private ObjectiveResponse toResponse(Objective obj, List<KeyResultResponse> krs) {
        double total = 0.0;
        for (KeyResultResponse kr : krs) {
            total += kr.getCurrentValue().doubleValue();
        }
        BigDecimal avgProgress = krs.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(Math.round((total / krs.size()) * 100.0) / 100.0);
        return ObjectiveResponse.builder()
                .id(obj.getId())
                .title(obj.getTitle())
                .description(obj.getDescription())
                .quarter(obj.getQuarter())
                .progress(avgProgress)
                .createdBy(obj.getCreatedBy())
                .createdAt(obj.getCreatedAt())
                .updatedAt(obj.getUpdatedAt())
                .keyResults(krs)
                .build();
    }

    private void validateMembership(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new com.okabe.exception.UnauthorizedException("You are not a member of this workspace");
        }
    }
}
