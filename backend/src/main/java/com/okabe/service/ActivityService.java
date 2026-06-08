package com.okabe.service;

import com.okabe.dto.response.ActivityResponse;
import com.okabe.entity.Card;
import com.okabe.entity.User;
import java.util.List;

public interface ActivityService {
    // Ghi lại hoạt động của user trên card (vd: tạo, cập nhật, di chuyển, xoá)
    void logActivity(Card card, User user, String actionType, String description);
    // Lấy danh sách hoạt động của card
    List<ActivityResponse> getCardActivities(Long cardId);
    // Lấy danh sách hoạt động của board
    List<ActivityResponse> getBoardActivities(Long boardId);
}
