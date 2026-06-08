package com.okabe.service.impl;

import com.okabe.dto.response.WorkloadResponse;
import com.okabe.entity.User;
import com.okabe.entity.WorkspaceMember;
import com.okabe.repository.CardRepository;
import com.okabe.repository.UserRepository;
import com.okabe.repository.WorkspaceMemberRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.WorkloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkloadServiceImpl implements WorkloadService {

    private final CardRepository cardRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Override
    public WorkloadResponse getWorkload(Long workspaceId, LocalDate from, LocalDate to, UserPrincipal currentUser) {
        List<Object[]> rawData = cardRepository.findWorkloadByWorkspaceAndDateRange( // Lấy dữ liệu workload từ repository
                workspaceId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        Map<Long, Map<LocalDate, WorkloadResponse.DayWorkload>> workloadMap = new HashMap<>(); // Map lưu workload theo user và ngày

        for (Object[] row : rawData) { // Duyệt từng dòng dữ liệu
            Long userId = ((Number) row[0]).longValue(); // Lấy user ID
            LocalDate date = ((Date) row[1]).toLocalDate(); // Lấy ngày
            int cardCount = ((Number) row[2]).intValue(); // Lấy số lượng card
            double totalHours = row[3] != null ? ((Number) row[3]).doubleValue() : 0; // Lấy tổng số giờ

            boolean overloaded = totalHours > 8; // Kiểm tra quá tải (>8h)
            workloadMap.computeIfAbsent(userId, k -> new HashMap<>()) // Tạo map mới nếu chưa có
                    .put(date, WorkloadResponse.DayWorkload.builder() // Thêm dữ liệu workload theo ngày
                            .date(date) // Gán ngày
                            .cardCount(cardCount) // Gán số card
                            .totalHours(totalHours) // Gán tổng giờ
                            .isOverloaded(overloaded) // Gán trạng thái quá tải
                            .build()); // Xây dựng DayWorkload
        }

        List<WorkspaceMember> allMembers = workspaceMemberRepository.findByWorkspaceId(workspaceId); // Lấy tất cả thành viên
        Map<Long, User> userCache = new HashMap<>(); // Cache user

        List<WorkloadResponse.MemberWorkload> members = new ArrayList<>(); // Danh sách kết quả
        for (WorkspaceMember member : allMembers) { // Duyệt từng thành viên
            Long userId = member.getUserId(); // Lấy user ID
            User user = userCache.computeIfAbsent(userId, id -> // Lấy user từ cache hoặc truy vấn
                    userRepository.findById(id).orElse(null));
            if (user == null) continue; // Bỏ qua nếu không tìm thấy user

            Map<LocalDate, WorkloadResponse.DayWorkload> userWorkload = workloadMap.get(userId); // Lấy workload của user
            List<WorkloadResponse.DayWorkload> dayWorkloads = new ArrayList<>(); // Danh sách workload theo ngày
            LocalDate current = from; // Bắt đầu từ ngày from
            while (!current.isAfter(to)) { // Duyệt từng ngày trong khoảng
                if (userWorkload != null && userWorkload.containsKey(current)) { // Nếu có dữ liệu
                    dayWorkloads.add(userWorkload.get(current)); // Thêm dữ liệu có sẵn
                } else { // Nếu không có dữ liệu
                    dayWorkloads.add(WorkloadResponse.DayWorkload.builder() // Thêm dữ liệu mặc định
                            .date(current) // Gán ngày
                            .cardCount(0) // 0 card
                            .totalHours(0) // 0 giờ
                            .isOverloaded(false) // Không quá tải
                            .build()); // Xây dựng DayWorkload
                }
                current = current.plusDays(1); // Tăng ngày lên 1
            }

            members.add(WorkloadResponse.MemberWorkload.builder() // Thêm vào danh sách
                    .userId(userId) // Gán user ID
                    .userName(user.getUsername()) // Gán tên
                    .avatarUrl(user.getAvatarUrl()) // Gán avatar
                    .workload(dayWorkloads) // Gán dữ liệu workload
                    .build()); // Xây dựng MemberWorkload
        }

        return WorkloadResponse.builder() // Xây dựng phản hồi
                .members(members) // Gán danh sách thành viên
                .build(); // Xây dựng WorkloadResponse
    }

}
