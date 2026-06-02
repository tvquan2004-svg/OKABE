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
        List<Object[]> rawData = cardRepository.findWorkloadByWorkspaceAndDateRange(
                workspaceId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        Map<Long, Map<LocalDate, WorkloadResponse.DayWorkload>> workloadMap = new HashMap<>();

        for (Object[] row : rawData) {
            Long userId = ((Number) row[0]).longValue();
            LocalDate date = ((Date) row[1]).toLocalDate();
            int cardCount = ((Number) row[2]).intValue();
            double totalHours = row[3] != null ? ((Number) row[3]).doubleValue() : 0;

            boolean overloaded = totalHours > 8;
            workloadMap.computeIfAbsent(userId, k -> new HashMap<>())
                    .put(date, WorkloadResponse.DayWorkload.builder()
                            .date(date)
                            .cardCount(cardCount)
                            .totalHours(totalHours)
                            .isOverloaded(overloaded)
                            .build());
        }

        List<WorkspaceMember> allMembers = workspaceMemberRepository.findByWorkspaceId(workspaceId);
        Map<Long, User> userCache = new HashMap<>();

        List<WorkloadResponse.MemberWorkload> members = new ArrayList<>();
        for (WorkspaceMember member : allMembers) {
            Long userId = member.getUserId();
            User user = userCache.computeIfAbsent(userId, id ->
                    userRepository.findById(id).orElse(null));
            if (user == null) continue;

            Map<LocalDate, WorkloadResponse.DayWorkload> userWorkload = workloadMap.get(userId);
            List<WorkloadResponse.DayWorkload> dayWorkloads = new ArrayList<>();
            LocalDate current = from;
            while (!current.isAfter(to)) {
                if (userWorkload != null && userWorkload.containsKey(current)) {
                    dayWorkloads.add(userWorkload.get(current));
                } else {
                    dayWorkloads.add(WorkloadResponse.DayWorkload.builder()
                            .date(current)
                            .cardCount(0)
                            .totalHours(0)
                            .isOverloaded(false)
                            .build());
                }
                current = current.plusDays(1);
            }

            members.add(WorkloadResponse.MemberWorkload.builder()
                    .userId(userId)
                    .userName(user.getUsername())
                    .avatarUrl(user.getAvatarUrl())
                    .workload(dayWorkloads)
                    .build());
        }

        return WorkloadResponse.builder()
                .members(members)
                .build();
    }

}
