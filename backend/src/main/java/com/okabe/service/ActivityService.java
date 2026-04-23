package com.okabe.service;

import com.okabe.dto.response.ActivityResponse;
import com.okabe.entity.Card;
import com.okabe.entity.User;
import java.util.List;

public interface ActivityService {
    void logActivity(Card card, User user, String actionType, String description);
    List<ActivityResponse> getCardActivities(Long cardId);
    List<ActivityResponse> getBoardActivities(Long boardId);
}
