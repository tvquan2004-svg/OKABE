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
                .card(card)
                .user(user)
                .actionType(actionType)
                .description(description)
                .build();
        activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getCardActivities(Long cardId) {
        // Get last 50 activities for now
        return activityRepository.findByCardIdOrderByCreatedAtDesc(cardId, PageRequest.of(0, 50)).stream()
                .map(a -> ActivityResponse.builder()
                        .id(a.getId())
                        .userId(a.getUser() != null ? a.getUser().getId() : null)
                        .username(a.getUser() != null ? a.getUser().getUsername() : "Unknown")
                        .avatarUrl(a.getUser() != null ? a.getUser().getAvatarUrl() : null)
                        .actionType(a.getActionType())
                        .description(a.getDescription())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
