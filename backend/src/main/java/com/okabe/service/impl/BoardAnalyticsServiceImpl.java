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
import org.springframework.data.domain.PageRequest;
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
        if (!boardRepository.existsById(boardId)) { // Kiểm tra bảng tồn tại
            throw new ResourceNotFoundException("Board", boardId); // Ném lỗi nếu không tìm thấy
        }

        List<TaskList> lists = taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId); // Lấy danh sách cột
        List<Card> allCards = cardRepository.findByTaskListBoardIdAndIsArchivedFalse(boardId); // Lấy tất cả card
        List<Activity> allActivities = activityRepository.findByCardTaskListBoardIdOrderByCreatedAtDesc(boardId, PageRequest.of(0, 1000)); // Lấy hoạt động

        // Define completed lists
        Set<Long> completedListIds = lists.stream() // Lọc danh sách cột "hoàn thành"
                .filter(l -> isCompletedList(l.getName())) // Kiểm tra tên cột
                .map(TaskList::getId) // Lấy ID
                .collect(Collectors.toSet()); // Thu thập thành Set

        LocalDateTime now = LocalDateTime.now(); // Thời gian hiện tại
        LocalDateTime oneWeekAgo = now.minusDays(7); // 1 tuần trước
        LocalDate today = LocalDate.now(); // Ngày hiện tại

        // 1. Cards by Status
        List<BoardAnalyticsResponse.StatusStats> statusStats = lists.stream().map(l -> { // Duyệt từng cột
            List<Card> listCards = allCards.stream() // Lọc card thuộc cột
                    .filter(c -> c.getTaskList().getId().equals(l.getId()))
                    .collect(Collectors.toList()); // Thu thập thành danh sách

            long overdue = listCards.stream() // Đếm card quá hạn
                    .filter(c -> c.getDueDate() != null && c.getDueDate().isBefore(now) && !completedListIds.contains(l.getId()))
                    .count(); // Đếm

            long completedThisWeek = listCards.stream() // Đếm card hoàn thành trong tuần
                    .filter(c -> completedListIds.contains(l.getId()) && c.getUpdatedAt() != null && c.getUpdatedAt().isAfter(oneWeekAgo))
                    .count(); // Đếm

            return BoardAnalyticsResponse.StatusStats.builder()
                    .listId(l.getId()) // Gán ID cột
                    .listName(l.getName()) // Gán tên cột
                    .total(listCards.size()) // Tổng số card
                    .overdue((int) overdue) // Số card quá hạn
                    .completedThisWeek((int) completedThisWeek) // Số card hoàn thành trong tuần
                    .build(); // Xây dựng StatusStats
        }).collect(Collectors.toList()); // Thu thập thành danh sách

        // 2. Cards by Priority
        Map<String, Long> priorityMap = allCards.stream() // Nhóm card theo độ ưu tiên
                .collect(Collectors.groupingBy(c -> c.getPriority().name(), Collectors.counting()));
        
        List<BoardAnalyticsResponse.PriorityStats> priorityStats = priorityMap.entrySet().stream() // Chuyển đổi sang dạng thống kê
                .map(e -> new BoardAnalyticsResponse.PriorityStats(e.getKey(), e.getValue().intValue()))
                .collect(Collectors.toList()); // Thu thập thành danh sách

        // 3. Cards by Member
        Map<User, List<Card>> memberCardsMap = new HashMap<>(); // Map lưu card theo thành viên
        allCards.forEach(c -> c.getMembers().forEach(m -> { // Duyệt từng card và thành viên
            memberCardsMap.computeIfAbsent(m, k -> new ArrayList<>()).add(c); // Thêm card vào danh sách
        }));

        List<BoardAnalyticsResponse.MemberStats> memberStats = memberCardsMap.entrySet().stream() // Xử lý map
                .map(e -> { // Chuyển đổi từng entry
                    User u = e.getKey(); // Lấy thông tin user
                    List<Card> uCards = e.getValue(); // Lấy danh sách card
                    long overdue = uCards.stream() // Đếm card quá hạn
                            .filter(c -> c.getDueDate() != null && c.getDueDate().isBefore(now) && !completedListIds.contains(c.getTaskList().getId()))
                            .count(); // Đếm
                    return BoardAnalyticsResponse.MemberStats.builder()
                            .userId(u.getId()) // Gán ID user
                            .username(u.getUsername()) // Gán tên
                            .avatarUrl(u.getAvatarUrl()) // Gán avatar
                            .assignedCount(uCards.size()) // Số card được giao
                            .overdueCount((int) overdue) // Số card quá hạn
                            .build(); // Xây dựng MemberStats
                }).collect(Collectors.toList()); // Thu thập thành danh sách

        // 4. Burndown (Last 30 days)
        List<BoardAnalyticsResponse.BurndownData> burndownData = new ArrayList<>(); // Danh sách dữ liệu burndown
        for (int i = 30; i >= 0; i--) { // Duyệt 30 ngày
            LocalDate date = today.minusDays(i); // Ngày trong quá khứ
            LocalDateTime endOfDay = date.atTime(23, 59, 59); // Cuối ngày

            long totalAtDate = allCards.stream() // Đếm card tồn tại trước ngày đó
                    .filter(c -> c.getCreatedAt().isBefore(endOfDay))
                    .count();
            
            // This is a simplification: we assume if it's currently completed and was created before that date, 
            // and its updatedAt is before endOfDay, it was completed then.
            // A better way would be tracking historical list movements.
            long completedAtDate = allCards.stream() // Đếm card hoàn thành trước ngày đó
                    .filter(c -> completedListIds.contains(c.getTaskList().getId()) 
                            && c.getUpdatedAt() != null && c.getUpdatedAt().isBefore(endOfDay))
                    .count();

            burndownData.add(new BoardAnalyticsResponse.BurndownData(date, (int) (totalAtDate - completedAtDate), (int) completedAtDate)); // Thêm dữ liệu
        }

        // 5. Avg Completion Days
        List<Card> completedCards = allCards.stream() // Lọc card đã hoàn thành
                .filter(c -> completedListIds.contains(c.getTaskList().getId()))
                .collect(Collectors.toList());
        
        float avgDays = 0; // Số ngày trung bình
        if (!completedCards.isEmpty()) { // Nếu có card hoàn thành
            long totalDays = completedCards.stream() // Tính tổng số ngày
                    .mapToLong(c -> ChronoUnit.DAYS.between(c.getCreatedAt(), c.getUpdatedAt()))
                    .sum(); // Tổng
            avgDays = (float) totalDays / completedCards.size(); // Tính trung bình
        }

        // 6. Activity Heatmap (Last 90 days)
        Map<LocalDate, Long> activityMap = allActivities.stream() // Nhóm hoạt động theo ngày
                .filter(a -> a.getCreatedAt().isAfter(now.minusDays(90))) // Lọc 90 ngày gần đây
                .collect(Collectors.groupingBy(a -> a.getCreatedAt().toLocalDate(), Collectors.counting())); // Đếm

        List<BoardAnalyticsResponse.HeatmapData> heatmapData = new ArrayList<>(); // Dữ liệu heatmap
        for (int i = 90; i >= 0; i--) { // Duyệt 90 ngày
            LocalDate date = today.minusDays(i); // Ngày trong quá khứ
            heatmapData.add(new BoardAnalyticsResponse.HeatmapData(date, activityMap.getOrDefault(date, 0L).intValue())); // Thêm dữ liệu
        }

        return BoardAnalyticsResponse.builder()
                .cardsByStatus(statusStats) // Gán thống kê theo trạng thái
                .cardsByPriority(priorityStats) // Gán thống kê theo độ ưu tiên
                .cardsByMember(memberStats) // Gán thống kê theo thành viên
                .burndown(burndownData) // Gán dữ liệu burndown
                .avgCompletionDays(avgDays) // Gán số ngày hoàn thành trung bình
                .activityHeatmap(heatmapData) // Gán heatmap hoạt động
                .build(); // Xây dựng BoardAnalyticsResponse
    }

    private boolean isCompletedList(String name) {
        String lower = name.toLowerCase(); // Chuyển thành chữ thường
        return lower.contains("done") || lower.contains("completed") || lower.contains("closed") || lower.contains("hoàn thành"); // Kiểm tra tên cột
    }
}
