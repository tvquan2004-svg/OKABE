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
        validateMembership(workspaceId, currentUser.getId()); // Kiểm tra quyền thành viên trong workspace

        Objective objective = Objective.builder()
                .workspaceId(workspaceId) // Gán ID workspace
                .title(request.title()) // Gán tiêu đề mục tiêu
                .description(request.description()) // Gán mô tả
                .quarter(request.quarter()) // Gán quý
                .createdBy(currentUser.getId()) // Gán người tạo
                .progress(BigDecimal.ZERO) // Khởi tạo tiến độ = 0
                .build(); // Xây dựng đối tượng Objective
        objective = objectiveRepository.save(objective); // Lưu mục tiêu vào CSDL
        return toResponse(objective, List.of()); // Trả về phản hồi
    }

    @Override
    public List<ObjectiveResponse> getObjectivesByQuarter(Long workspaceId, String quarter, UserPrincipal currentUser) {
        validateMembership(workspaceId, currentUser.getId()); // Kiểm tra quyền thành viên

        List<Objective> objectives;
        if (quarter != null && !quarter.isBlank()) { // Nếu có quý cụ thể
            objectives = objectiveRepository.findByWorkspaceIdAndQuarterOrderByCreatedAtDesc(workspaceId, quarter); // Lọc theo quý
        } else { // Nếu không lọc theo quý
            objectives = objectiveRepository.findByWorkspaceId(workspaceId); // Lấy tất cả mục tiêu
        }

        return objectives.stream() // Xử lý danh sách mục tiêu
                .map(obj -> {
                    List<KeyResultResponse> krs = keyResultRepository.findByObjectiveId(obj.getId()) // Lấy key results của mục tiêu
                            .stream().map(this::toKrResponse).collect(Collectors.toList()); // Chuyển đổi sang response
                    return toResponse(obj, krs); // Xây dựng phản hồi
                })
                .collect(Collectors.toList()); // Thu thập thành danh sách
    }

    @Override
    public ObjectiveResponse getObjective(Long id, UserPrincipal currentUser) {
        Objective obj = objectiveRepository.findById(id) // Tìm mục tiêu theo ID
                .orElseThrow(() -> new EntityNotFoundException("Objective not found")); // Ném lỗi nếu không tìm thấy
        validateMembership(obj.getWorkspaceId(), currentUser.getId()); // Kiểm tra quyền

        List<KeyResultResponse> krs = keyResultRepository.findByObjectiveId(id) // Lấy key results
                .stream().map(this::toKrResponse).collect(Collectors.toList()); // Chuyển đổi
        return toResponse(obj, krs); // Trả về phản hồi
    }

    @Override
    @Transactional
    public KeyResultResponse addKeyResult(Long objectiveId, KeyResultRequest request, UserPrincipal currentUser) {
        Objective obj = objectiveRepository.findById(objectiveId) // Tìm mục tiêu
                .orElseThrow(() -> new EntityNotFoundException("Objective not found")); // Ném lỗi nếu không tìm thấy
        validateMembership(obj.getWorkspaceId(), currentUser.getId()); // Kiểm tra quyền

        BigDecimal tv = request.targetValue() != null // Nếu có giá trị mục tiêu
                ? BigDecimal.valueOf(request.targetValue()) // Chuyển đổi thành BigDecimal
                : null; // null nếu không có
        KeyResult kr = KeyResult.builder()
                .objectiveId(objectiveId) // Gán ID mục tiêu
                .title(request.title()) // Gán tiêu đề
                .targetValue(tv) // Gán giá trị mục tiêu
                .unit(request.unit() != null ? request.unit() : "percent") // Gán đơn vị (mặc định percent)
                .currentValue(BigDecimal.ZERO) // Khởi tạo giá trị hiện tại = 0
                .build(); // Xây dựng KeyResult
        kr = keyResultRepository.save(kr); // Lưu key result
        return toKrResponse(kr); // Trả về phản hồi
    }

    @Override
    @Transactional
    public void linkCardsToKeyResult(Long keyResultId, List<Long> cardIds, UserPrincipal currentUser) {
        KeyResult kr = keyResultRepository.findById(keyResultId) // Tìm key result theo ID
                .orElseThrow(() -> new EntityNotFoundException("KeyResult not found")); // Ném lỗi nếu không tìm thấy
        Objective obj = objectiveRepository.findById(kr.getObjectiveId()) // Tìm mục tiêu cha
                .orElseThrow(() -> new EntityNotFoundException("Objective not found")); // Ném lỗi nếu không tìm thấy
        validateMembership(obj.getWorkspaceId(), currentUser.getId()); // Kiểm tra quyền

        try {
            String json = objectMapper.writeValueAsString(cardIds); // Chuyển đổi danh sách card IDs thành JSON
            kr.setLinkedCards(json); // Gán JSON vào key result
            keyResultRepository.save(kr); // Lưu thay đổi
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize card IDs", e); // Ném lỗi nếu serialize thất bại
        }
    }

    @Override
    @Transactional
    public void deleteObjective(Long id, UserPrincipal currentUser) {
        Objective obj = objectiveRepository.findById(id) // Tìm mục tiêu theo ID
                .orElseThrow(() -> new EntityNotFoundException("Objective not found")); // Ném lỗi nếu không tìm thấy
        validateMembership(obj.getWorkspaceId(), currentUser.getId()); // Kiểm tra quyền
        objectiveRepository.delete(obj); // Xóa mục tiêu
    }

    @Override
    @Transactional
    public ObjectiveResponse recalculateProgress(Long objectiveId, UserPrincipal currentUser) {
        Objective obj = objectiveRepository.findById(objectiveId) // Tìm mục tiêu
                .orElseThrow(() -> new EntityNotFoundException("Objective not found")); // Ném lỗi nếu không tìm thấy
        validateMembership(obj.getWorkspaceId(), currentUser.getId()); // Kiểm tra quyền

        List<KeyResult> krs = keyResultRepository.findByObjectiveId(objectiveId); // Lấy danh sách key results
        if (krs.isEmpty()) { // Nếu không có key result
            obj.setProgress(BigDecimal.ZERO); // Đặt tiến độ = 0
            objectiveRepository.save(obj); // Lưu thay đổi
            return toResponse(obj, List.of()); // Trả về phản hồi
        }

        double totalProgress = 0.0; // Khởi tạo tổng tiến độ
        for (KeyResult kr : krs) { // Duyệt từng key result
            totalProgress += calculateKrProgress(kr); // Cộng dồn tiến độ
        }
        double avgProgress = totalProgress / krs.size(); // Tính tiến độ trung bình
        obj.setProgress(BigDecimal.valueOf(Math.round(avgProgress * 100.0) / 100.0)); // Cập nhật tiến độ mục tiêu
        objectiveRepository.save(obj); // Lưu thay đổi

        List<KeyResultResponse> krResponses = krs.stream().map(this::toKrResponse).collect(Collectors.toList()); // Chuyển đổi key results
        return toResponse(obj, krResponses); // Trả về phản hồi
    }

    @Override
    public List<ObjectiveResponse> getOkrTree(Long workspaceId, String quarter, UserPrincipal currentUser) {
        return getObjectivesByQuarter(workspaceId, quarter, currentUser); // Lấy danh sách mục tiêu theo quý
    }

    private double calculateKrProgress(KeyResult kr) {
        List<Long> cardIds = parseCardIds(kr.getLinkedCards()); // Lấy danh sách card IDs từ JSON
        if (cardIds == null || cardIds.isEmpty()) return 0.0; // Trả về 0 nếu không có card

        List<Card> cards = cardRepository.findAllById(cardIds); // Tìm các card theo IDs
        if (cards.isEmpty()) return 0.0; // Trả về 0 nếu không tìm thấy

        double totalCompletion = 0.0; // Tổng mức độ hoàn thành
        int counted = 0; // Đếm số card
        for (Card card : cards) { // Duyệt từng card
            double completion = calculateCardCompletion(card); // Tính % hoàn thành của card
            totalCompletion += completion; // Cộng dồn
            counted++; // Tăng biến đếm
        }
        return counted > 0 ? totalCompletion / counted : 0.0; // Trả về trung bình hoàn thành
    }

    private double calculateCardCompletion(Card card) {
        if (card.getIsArchived()) return 100.0; // Nếu card đã archive, coi như hoàn thành 100%

        List<Checklist> checklists = card.getChecklists(); // Lấy danh sách checklist
        if (checklists == null || checklists.isEmpty()) { // Nếu không có checklist
            if (card.getDescription() != null && !card.getDescription().isBlank()) return 100.0; // Có mô tả => hoàn thành
            return 0.0; // Không có gì => 0%
        }

        long totalItems = checklists.stream() // Đếm tổng số item trong tất cả checklist
                .filter(cl -> cl.getItems() != null) // Lọc checklist có item
                .flatMap(cl -> cl.getItems().stream()) // Làm phẳng danh sách items
                .count(); // Đếm
        long completedItems = checklists.stream() // Đếm số item đã hoàn thành
                .filter(cl -> cl.getItems() != null) // Lọc checklist có item
                .flatMap(cl -> cl.getItems().stream()) // Làm phẳng danh sách items
                .filter(item -> Boolean.TRUE.equals(item.getIsCompleted())) // Lọc item đã hoàn thành
                .count(); // Đếm

        if (totalItems == 0) return 0.0; // Nếu không có item nào
        return (double) completedItems / totalItems * 100.0; // Tính % hoàn thành
    }

    private List<Long> parseCardIds(String linkedCards) {
        if (linkedCards == null || linkedCards.isBlank()) return List.of(); // Trả về danh sách rỗng nếu null
        try {
            return objectMapper.readValue(linkedCards, new TypeReference<List<Long>>() {}); // Parse JSON thành danh sách Long
        } catch (Exception e) {
            log.warn("Failed to parse linked_cards JSON: {}", linkedCards, e); // Ghi log cảnh báo
            return List.of(); // Trả về danh sách rỗng nếu lỗi
        }
    }

    private KeyResultResponse toKrResponse(KeyResult kr) {
        double completion = calculateKrProgress(kr); // Tính tiến độ của key result
        return KeyResultResponse.builder()
                .id(kr.getId()) // Gán ID
                .title(kr.getTitle()) // Gán tiêu đề
                .targetValue(kr.getTargetValue()) // Gán giá trị mục tiêu
                .currentValue(BigDecimal.valueOf(completion)) // Gán giá trị hiện tại
                .unit(kr.getUnit()) // Gán đơn vị
                .linkedCards(parseCardIds(kr.getLinkedCards())) // Gán danh sách card liên kết
                .createdAt(kr.getCreatedAt()) // Gán thời gian tạo
                .build(); // Xây dựng KeyResultResponse
    }

    private ObjectiveResponse toResponse(Objective obj, List<KeyResultResponse> krs) {
        double total = 0.0; // Tổng giá trị các key results
        for (KeyResultResponse kr : krs) { // Duyệt danh sách key results
            total += kr.getCurrentValue().doubleValue(); // Cộng dồn giá trị
        }
        BigDecimal avgProgress = krs.isEmpty() // Tính tiến độ trung bình
                ? BigDecimal.ZERO // 0 nếu không có key result
                : BigDecimal.valueOf(Math.round((total / krs.size()) * 100.0) / 100.0); // Tính trung bình
        return ObjectiveResponse.builder()
                .id(obj.getId()) // Gán ID
                .title(obj.getTitle()) // Gán tiêu đề
                .description(obj.getDescription()) // Gán mô tả
                .quarter(obj.getQuarter()) // Gán quý
                .progress(avgProgress) // Gán tiến độ
                .createdBy(obj.getCreatedBy()) // Gán người tạo
                .createdAt(obj.getCreatedAt()) // Gán thời gian tạo
                .updatedAt(obj.getUpdatedAt()) // Gán thời gian cập nhật
                .keyResults(krs) // Gán danh sách key results
                .build(); // Xây dựng ObjectiveResponse
    }

    private void validateMembership(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) { // Nếu không phải thành viên
            throw new com.okabe.exception.UnauthorizedException("You are not a member of this workspace"); // Ném lỗi
        }
    }
}
