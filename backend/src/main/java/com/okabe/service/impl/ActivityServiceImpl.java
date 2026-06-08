package com.okabe.service.impl;

import com.okabe.dto.response.ActivityResponse;
import com.okabe.entity.Activity;
import com.okabe.entity.Card;
import com.okabe.entity.User;
import com.okabe.repository.ActivityRepository;
import com.okabe.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;

    @Override
    @Transactional
    public void logActivity(Card card, User user, String actionType, String description) {
        Activity activity = Activity.builder()
                .card(card) // Gán thẻ liên quan đến hoạt động
                .user(user) // Gán người dùng thực hiện hành động
                .actionType(actionType) // Gán loại hành động (VD: CREATE, UPDATE)
                .description(description) // Gán mô tả chi tiết hành động
                .build(); // Xây dựng đối tượng Activity
        activityRepository.save(activity); // Lưu hoạt động vào cơ sở dữ liệu
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getCardActivities(Long cardId) {
        // Get last 50 activities for now
        return activityRepository.findByCardIdOrderByCreatedAtDesc(cardId, PageRequest.of(0, 50)).stream() // Truy vấn 50 hoạt động gần nhất của thẻ
                .map(a -> ActivityResponse.builder()
                        .id(a.getId()) // Gán ID hoạt động
                        .userId(a.getUser() != null ? a.getUser().getId() : null) // Gán ID người dùng nếu tồn tại
                        .username(a.getUser() != null ? a.getUser().getUsername() : "Unknown") // Gán tên người dùng hoặc "Unknown"
                        .avatarUrl(a.getUser() != null ? a.getUser().getAvatarUrl() : null) // Gán URL avatar nếu có
                        .actionType(a.getActionType()) // Gán loại hành động
                        .description(a.getDescription()) // Gán mô tả hành động
                        .cardId(a.getCard() != null ? a.getCard().getId() : null) // Gán ID thẻ nếu tồn tại
                        .createdAt(a.getCreatedAt()) // Gán thời gian tạo
                        .build()) // Xây dựng đối tượng ActivityResponse
                .collect(Collectors.toList()); // Thu thập kết quả thành danh sách
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getBoardActivities(Long boardId) {
        // Get last 100 activities for board
        return activityRepository.findByCardTaskListBoardIdOrderByCreatedAtDesc(boardId, PageRequest.of(0, 100)).stream() // Truy vấn 100 hoạt động gần nhất của bảng
                .map(a -> ActivityResponse.builder()
                        .id(a.getId()) // Gán ID hoạt động
                        .userId(a.getUser() != null ? a.getUser().getId() : null) // Gán ID người dùng nếu tồn tại
                        .username(a.getUser() != null ? a.getUser().getUsername() : "Unknown") // Gán tên người dùng hoặc "Unknown"
                        .avatarUrl(a.getUser() != null ? a.getUser().getAvatarUrl() : null) // Gán URL avatar nếu có
                        .actionType(a.getActionType()) // Gán loại hành động
                        .description(a.getDescription()) // Gán mô tả hành động
                        .cardId(a.getCard() != null ? a.getCard().getId() : null) // Gán ID thẻ nếu tồn tại
                        .createdAt(a.getCreatedAt()) // Gán thời gian tạo
                        .build()) // Xây dựng đối tượng ActivityResponse
                .collect(Collectors.toList()); // Thu thập kết quả thành danh sách
    }
}
