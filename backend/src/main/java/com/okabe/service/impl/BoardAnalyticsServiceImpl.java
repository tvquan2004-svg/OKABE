package com.okabe.service.impl;

import com.okabe.dto.response.BoardAnalyticsResponse;
import com.okabe.entity.Activity;
import com.okabe.entity.Card;
import com.okabe.entity.TaskList;
import com.okabe.entity.User;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.repository.ActivityRepository;
import com.okabe.repository.BoardRepository;
import com.okabe.repository.CardRepository;
import com.okabe.repository.TaskListRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.BoardAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardAnalyticsServiceImpl implements BoardAnalyticsService {

    private final BoardRepository boardRepository;
    private final CardRepository cardRepository;
    private final TaskListRepository taskListRepository;
    private final ActivityRepository activityRepository;

    @Override
    public BoardAnalyticsResponse getBoardAnalytics(Long boardId, UserPrincipal currentUser) {
        if (!boardRepository.existsById(boardId)) {
            throw new ResourceNotFoundException("Board", boardId);
        }

        List<TaskList> lists = taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId);
        List<Card> allCards = cardRepository.findByTaskListBoardIdAndIsArchivedFalse(boardId);
        List<Activity> allActivities = activityRepository.findByCardTaskListBoardIdOrderByCreatedAtDesc(boardId);

        // Define completed lists
        Set<Long> completedListIds = lists.stream()
                .filter(l -> isCompletedList(l.getName()))
                .map(TaskList::getId)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusDays(7);
        LocalDate today = LocalDate.now();

        // 1. Cards by Status
        List<BoardAnalyticsResponse.StatusStats> statusStats = lists.stream().map(l -> {
            List<Card> listCards = allCards.stream()
                    .filter(c -> c.getTaskList().getId().equals(l.getId()))
                    .collect(Collectors.toList());

            long overdue = listCards.stream()
                    .filter(c -> c.getDueDate() != null && c.getDueDate().isBefore(now) && !completedListIds.contains(l.getId()))
                    .count();

            long completedThisWeek = listCards.stream()
                    .filter(c -> completedListIds.contains(l.getId()) && c.getUpdatedAt() != null && c.getUpdatedAt().isAfter(oneWeekAgo))
                    .count();

            return BoardAnalyticsResponse.StatusStats.builder()
                    .listId(l.getId())
                    .listName(l.getName())
                    .total(listCards.size())
                    .overdue((int) overdue)
                    .completedThisWeek((int) completedThisWeek)
                    .build();
        }).collect(Collectors.toList());

        // 2. Cards by Priority
        Map<String, Long> priorityMap = allCards.stream()
                .collect(Collectors.groupingBy(c -> c.getPriority().name(), Collectors.counting()));
        
        List<BoardAnalyticsResponse.PriorityStats> priorityStats = priorityMap.entrySet().stream()
                .map(e -> new BoardAnalyticsResponse.PriorityStats(e.getKey(), e.getValue().intValue()))
                .collect(Collectors.toList());

        // 3. Cards by Member
        Map<User, List<Card>> memberCardsMap = new HashMap<>();
        allCards.forEach(c -> c.getMembers().forEach(m -> {
            memberCardsMap.computeIfAbsent(m, k -> new ArrayList<>()).add(c);
        }));

        List<BoardAnalyticsResponse.MemberStats> memberStats = memberCardsMap.entrySet().stream()
                .map(e -> {
                    User u = e.getKey();
                    List<Card> uCards = e.getValue();
                    long overdue = uCards.stream()
                            .filter(c -> c.getDueDate() != null && c.getDueDate().isBefore(now) && !completedListIds.contains(c.getTaskList().getId()))
                            .count();
                    return BoardAnalyticsResponse.MemberStats.builder()
                            .userId(u.getId())
                            .username(u.getUsername())
                            .avatarUrl(u.getAvatarUrl())
                            .assignedCount(uCards.size())
                            .overdueCount((int) overdue)
                            .build();
                }).collect(Collectors.toList());

        // 4. Burndown (Last 30 days)
        List<BoardAnalyticsResponse.BurndownData> burndownData = new ArrayList<>();
        for (int i = 30; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            long totalAtDate = allCards.stream()
                    .filter(c -> c.getCreatedAt().isBefore(endOfDay))
                    .count();
            
            // This is a simplification: we assume if it's currently completed and was created before that date, 
            // and its updatedAt is before endOfDay, it was completed then.
            // A better way would be tracking historical list movements.
            long completedAtDate = allCards.stream()
                    .filter(c -> completedListIds.contains(c.getTaskList().getId()) 
                            && c.getUpdatedAt() != null && c.getUpdatedAt().isBefore(endOfDay))
                    .count();

            burndownData.add(new BoardAnalyticsResponse.BurndownData(date, (int) (totalAtDate - completedAtDate), (int) completedAtDate));
        }

        // 5. Avg Completion Days
        List<Card> completedCards = allCards.stream()
                .filter(c -> completedListIds.contains(c.getTaskList().getId()))
                .collect(Collectors.toList());
        
        float avgDays = 0;
        if (!completedCards.isEmpty()) {
            long totalDays = completedCards.stream()
                    .mapToLong(c -> ChronoUnit.DAYS.between(c.getCreatedAt(), c.getUpdatedAt()))
                    .sum();
            avgDays = (float) totalDays / completedCards.size();
        }

        // 6. Activity Heatmap (Last 90 days)
        Map<LocalDate, Long> activityMap = allActivities.stream()
                .filter(a -> a.getCreatedAt().isAfter(now.minusDays(90)))
                .collect(Collectors.groupingBy(a -> a.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<BoardAnalyticsResponse.HeatmapData> heatmapData = new ArrayList<>();
        for (int i = 90; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            heatmapData.add(new BoardAnalyticsResponse.HeatmapData(date, activityMap.getOrDefault(date, 0L).intValue()));
        }

        return BoardAnalyticsResponse.builder()
                .cardsByStatus(statusStats)
                .cardsByPriority(priorityStats)
                .cardsByMember(memberStats)
                .burndown(burndownData)
                .avgCompletionDays(avgDays)
                .activityHeatmap(heatmapData)
                .build();
    }

    private boolean isCompletedList(String name) {
        String lower = name.toLowerCase();
        return lower.contains("done") || lower.contains("completed") || lower.contains("closed") || lower.contains("hoàn thành");
    }
}
