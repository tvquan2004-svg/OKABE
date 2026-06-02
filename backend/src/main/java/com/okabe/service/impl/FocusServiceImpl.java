package com.okabe.service.impl;

import com.okabe.dto.response.FocusSessionResponse;
import com.okabe.dto.response.FocusStatsResponse;
import com.okabe.entity.Card;
import com.okabe.entity.FocusSession;
import com.okabe.entity.User;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.exception.UnauthorizedException;
import com.okabe.repository.CardRepository;
import com.okabe.repository.FocusSessionRepository;
import com.okabe.repository.UserRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.FocusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FocusServiceImpl implements FocusService {

    private final FocusSessionRepository focusSessionRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FocusSessionResponse startFocus(Long cardId, int durationMinutes, UserPrincipal currentUser) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        requireCardMember(card, currentUser.getId());

        if (durationMinutes < 1 || durationMinutes > 120) {
            durationMinutes = 25;
        }

        // Auto-stop any previous active session (e.g. from page crash/refresh)
        var existing = focusSessionRepository.findByUserIdAndEndedAtIsNull(currentUser.getId());
        if (existing.isPresent()) {
            FocusSession oldSession = existing.get();
            LocalDateTime now = LocalDateTime.now();
            long elapsed = java.time.Duration.between(oldSession.getStartedAt(), now).toMinutes();
            oldSession.setEndedAt(now);
            oldSession.setCompleted(elapsed >= oldSession.getDurationMinutes());
            focusSessionRepository.save(oldSession);

            Card oldCard = cardRepository.findById(oldSession.getCardId()).orElse(null);
            if (oldCard != null) {
                int total = oldCard.getTotalFocusMinutes() != null ? oldCard.getTotalFocusMinutes() : 0;
                total += (int) Math.min(elapsed, oldSession.getDurationMinutes());
                oldCard.setTotalFocusMinutes(total);
                cardRepository.save(oldCard);
            }
            log.info("Auto-stopped stale focus session: id={}, elapsed={}min", oldSession.getId(), elapsed);
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        FocusSession session = FocusSession.builder()
                .cardId(cardId)
                .userId(user.getId())
                .startedAt(LocalDateTime.now())
                .durationMinutes(durationMinutes)
                .completed(false)
                .build();

        session = focusSessionRepository.save(session);
        log.info("Focus session started: id={}, card={}, user={}", session.getId(), cardId, user.getId());

        return toResponse(session, card.getTotalFocusMinutes() != null ? card.getTotalFocusMinutes() : 0);
    }

    @Override
    @Transactional
    public FocusSessionResponse stopFocus(Long cardId, UserPrincipal currentUser) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId));

        requireCardMember(card, currentUser.getId());

        FocusSession session = focusSessionRepository.findByUserIdAndEndedAtIsNull(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Không có phiên focus nào đang chạy."));

        if (!session.getCardId().equals(cardId)) {
            throw new IllegalStateException("Phiên focus đang chạy không thuộc card này.");
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsed = java.time.Duration.between(session.getStartedAt(), now).toMinutes();
        boolean completed = elapsed >= session.getDurationMinutes();

        session.setEndedAt(now);
        session.setCompleted(completed);
        focusSessionRepository.save(session);

        int totalMinutes = card.getTotalFocusMinutes() != null ? card.getTotalFocusMinutes() : 0;
        totalMinutes += (int) Math.min(elapsed, session.getDurationMinutes());
        card.setTotalFocusMinutes(totalMinutes);
        cardRepository.save(card);

        log.info("Focus session ended: id={}, elapsed={}min, completed={}", session.getId(), elapsed, completed);

        return toResponse(session, totalMinutes);
    }

    @Override
    @Transactional(readOnly = true)
    public FocusStatsResponse getStats(String from, String to, UserPrincipal currentUser) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().withDayOfMonth(1);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate prevWeekStart = weekStart.minusWeeks(1);

        int todayMinutes = focusSessionRepository.sumCompletedMinutesByUserAndDateRange(
                currentUser.getId(), today.atStartOfDay(), today.atTime(LocalTime.MAX));
        int weekMinutes = focusSessionRepository.sumCompletedMinutesByUserAndDateRange(
                currentUser.getId(), weekStart.atStartOfDay(), today.atTime(LocalTime.MAX));
        int monthMinutes = focusSessionRepository.sumCompletedMinutesByUserAndDateRange(currentUser.getId(), fromDt, toDt);
        int prevWeekMinutes = focusSessionRepository.sumCompletedMinutesByUserAndDateRange(
                currentUser.getId(), prevWeekStart.atStartOfDay(), weekStart.atTime(LocalTime.MAX));

        int weekChangePercent = prevWeekMinutes > 0
                ? ((weekMinutes - prevWeekMinutes) * 100 / prevWeekMinutes)
                : (weekMinutes > 0 ? 100 : 0);

        // Daily breakdown for the current week
        List<FocusSession> weekSessions = focusSessionRepository
                .findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(currentUser.getId(),
                        weekStart.atStartOfDay(), today.atTime(LocalTime.MAX));

        Map<LocalDate, Integer> dailyMap = weekSessions.stream()
                .filter(FocusSession::getCompleted)
                .collect(Collectors.groupingBy(
                        s -> s.getStartedAt().toLocalDate(),
                        Collectors.summingInt(s -> Math.min(
                                (int) java.time.Duration.between(s.getStartedAt(),
                                        s.getEndedAt() != null ? s.getEndedAt() : s.getStartedAt()).toMinutes(),
                                s.getDurationMinutes()))
                ));

        List<FocusStatsResponse.DailyFocus> dailyBreakdown = weekStart.datesUntil(today.plusDays(1))
                .map(d -> FocusStatsResponse.DailyFocus.builder()
                        .date(d.toString())
                        .minutes(dailyMap.getOrDefault(d, 0))
                        .build())
                .toList();

        // Top cards
        List<Object[]> topRaw = focusSessionRepository.findTopFocusedCardsByUser(currentUser.getId());
        List<FocusStatsResponse.TopCard> topCards = topRaw.stream()
                .limit(5)
                .map(row -> {
                    Long cardId = (Long) row[0];
                    int sessions = ((Number) row[1]).intValue();
                    int totalMin = ((Number) row[2]).intValue();
                    String title = cardRepository.findById(cardId)
                            .map(Card::getTitle)
                            .orElse("Unknown");
                    return FocusStatsResponse.TopCard.builder()
                            .cardId(cardId)
                            .cardTitle(title)
                            .sessions(sessions)
                            .totalMinutes(totalMin)
                            .build();
                })
                .toList();

        return FocusStatsResponse.builder()
                .todayMinutes(todayMinutes)
                .weekMinutes(weekMinutes)
                .monthMinutes(monthMinutes)
                .weekChangePercent(weekChangePercent)
                .dailyBreakdown(dailyBreakdown)
                .topCards(topCards)
                .build();
    }

    private void requireCardMember(Card card, Long userId) {
        boolean isMember = card.getMembers().stream()
                .anyMatch(m -> m.getId().equals(userId));
        if (!isMember) {
            throw new UnauthorizedException("Bạn không phải là thành viên của card này.");
        }
    }

    private FocusSessionResponse toResponse(FocusSession session, int totalFocusMinutes) {
        String userName = userRepository.findById(session.getUserId())
                .map(User::getUsername)
                .orElse("Unknown");

        return FocusSessionResponse.builder()
                .id(session.getId())
                .cardId(session.getCardId())
                .userId(session.getUserId())
                .userName(userName)
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .durationMinutes(session.getDurationMinutes())
                .completed(session.getCompleted())
                .totalFocusMinutes(totalFocusMinutes)
                .build();
    }
}
