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
        Card card = cardRepository.findById(cardId) // Tìm thẻ theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId)); // Ném lỗi nếu không tìm thấy

        requireCardMember(card, currentUser.getId()); // Kiểm tra quyền thành viên của thẻ

        if (durationMinutes < 1 || durationMinutes > 120) { // Nếu thời lượng không hợp lệ
            durationMinutes = 25; // Đặt mặc định 25 phút
        }

        // Auto-stop any previous active session (e.g. from page crash/refresh)
        var existing = focusSessionRepository.findByUserIdAndEndedAtIsNull(currentUser.getId()); // Tìm phiên focus đang chạy
        if (existing.isPresent()) { // Nếu có phiên cũ đang chạy
            FocusSession oldSession = existing.get(); // Lấy phiên cũ
            LocalDateTime now = LocalDateTime.now(); // Lấy thời gian hiện tại
            long elapsed = java.time.Duration.between(oldSession.getStartedAt(), now).toMinutes(); // Tính thời gian đã trôi qua
            oldSession.setEndedAt(now); // Đặt thời gian kết thúc
            oldSession.setCompleted(elapsed >= oldSession.getDurationMinutes()); // Đánh dấu hoàn thành nếu đủ thời gian
            focusSessionRepository.save(oldSession); // Lưu phiên cũ

            Card oldCard = cardRepository.findById(oldSession.getCardId()).orElse(null); // Tìm thẻ của phiên cũ
            if (oldCard != null) { // Nếu tìm thấy thẻ
                int total = oldCard.getTotalFocusMinutes() != null ? oldCard.getTotalFocusMinutes() : 0; // Lấy tổng phút tập trung
                total += (int) Math.min(elapsed, oldSession.getDurationMinutes()); // Cộng dồn thời gian
                oldCard.setTotalFocusMinutes(total); // Cập nhật tổng phút
                cardRepository.save(oldCard); // Lưu thẻ
            }
            log.info("Auto-stopped stale focus session: id={}, elapsed={}min", oldSession.getId(), elapsed); // Ghi log
        }

        User user = userRepository.findById(currentUser.getId()) // Tìm người dùng
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId())); // Ném lỗi nếu không tìm thấy

        FocusSession session = FocusSession.builder()
                .cardId(cardId) // Gán ID thẻ
                .userId(user.getId()) // Gán ID người dùng
                .startedAt(LocalDateTime.now()) // Gán thời gian bắt đầu
                .durationMinutes(durationMinutes) // Gán thời lượng
                .completed(false) // Đặt trạng thái chưa hoàn thành
                .build(); // Xây dựng đối tượng FocusSession

        session = focusSessionRepository.save(session); // Lưu phiên focus mới
        log.info("Focus session started: id={}, card={}, user={}", session.getId(), cardId, user.getId()); // Ghi log

        return toResponse(session, card.getTotalFocusMinutes() != null ? card.getTotalFocusMinutes() : 0); // Trả về phản hồi
    }

    @Override
    @Transactional
    public FocusSessionResponse stopFocus(Long cardId, UserPrincipal currentUser) {
        Card card = cardRepository.findById(cardId) // Tìm thẻ theo ID
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardId)); // Ném lỗi nếu không tìm thấy

        requireCardMember(card, currentUser.getId()); // Kiểm tra quyền thành viên

        FocusSession session = focusSessionRepository.findByUserIdAndEndedAtIsNull(currentUser.getId()) // Tìm phiên focus đang chạy
                .orElseThrow(() -> new IllegalStateException("Không có phiên focus nào đang chạy.")); // Ném lỗi nếu không có

        if (!session.getCardId().equals(cardId)) { // Nếu phiên không thuộc card này
            throw new IllegalStateException("Phiên focus đang chạy không thuộc card này."); // Ném lỗi
        }

        LocalDateTime now = LocalDateTime.now(); // Lấy thời gian hiện tại
        long elapsed = java.time.Duration.between(session.getStartedAt(), now).toMinutes(); // Tính thời gian đã focus
        boolean completed = elapsed >= session.getDurationMinutes(); // Kiểm tra đã đủ thời gian

        session.setEndedAt(now); // Đặt thời gian kết thúc
        session.setCompleted(completed); // Đánh dấu hoàn thành
        focusSessionRepository.save(session); // Lưu phiên

        int totalMinutes = card.getTotalFocusMinutes() != null ? card.getTotalFocusMinutes() : 0; // Lấy tổng phút hiện tại
        totalMinutes += (int) Math.min(elapsed, session.getDurationMinutes()); // Cộng dồn thời gian
        card.setTotalFocusMinutes(totalMinutes); // Cập nhật tổng phút
        cardRepository.save(card); // Lưu thẻ

        log.info("Focus session ended: id={}, elapsed={}min, completed={}", session.getId(), elapsed, completed); // Ghi log

        return toResponse(session, totalMinutes); // Trả về phản hồi
    }

    @Override
    @Transactional(readOnly = true)
    public FocusStatsResponse getStats(String from, String to, UserPrincipal currentUser) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().withDayOfMonth(1); // Ngày bắt đầu
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now(); // Ngày kết thúc

        LocalDateTime fromDt = fromDate.atStartOfDay(); // Chuyển thành LocalDateTime
        LocalDateTime toDt = toDate.atTime(LocalTime.MAX); // Chuyển thành cuối ngày

        LocalDate today = LocalDate.now(); // Ngày hiện tại
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); // Đầu tuần (Thứ 2)
        LocalDate prevWeekStart = weekStart.minusWeeks(1); // Đầu tuần trước

        int todayMinutes = focusSessionRepository.sumCompletedMinutesByUserAndDateRange( // Phút focus hôm nay
                currentUser.getId(), today.atStartOfDay(), today.atTime(LocalTime.MAX));
        int weekMinutes = focusSessionRepository.sumCompletedMinutesByUserAndDateRange( // Phút focus tuần này
                currentUser.getId(), weekStart.atStartOfDay(), today.atTime(LocalTime.MAX));
        int monthMinutes = focusSessionRepository.sumCompletedMinutesByUserAndDateRange(currentUser.getId(), fromDt, toDt); // Phút focus tháng này
        int prevWeekMinutes = focusSessionRepository.sumCompletedMinutesByUserAndDateRange( // Phút focus tuần trước
                currentUser.getId(), prevWeekStart.atStartOfDay(), weekStart.atTime(LocalTime.MAX));

        int weekChangePercent = prevWeekMinutes > 0 // Tính % thay đổi so với tuần trước
                ? ((weekMinutes - prevWeekMinutes) * 100 / prevWeekMinutes)
                : (weekMinutes > 0 ? 100 : 0);

        // Daily breakdown for the current week
        List<FocusSession> weekSessions = focusSessionRepository
                .findByUserIdAndStartedAtBetweenOrderByStartedAtAsc(currentUser.getId(), // Lấy phiên focus trong tuần
                        weekStart.atStartOfDay(), today.atTime(LocalTime.MAX));

        Map<LocalDate, Integer> dailyMap = weekSessions.stream() // Nhóm theo ngày
                .filter(FocusSession::getCompleted) // Chỉ lấy phiên hoàn thành
                .collect(Collectors.groupingBy(
                        s -> s.getStartedAt().toLocalDate(), // Nhóm theo ngày
                        Collectors.summingInt(s -> Math.min( // Tính tổng phút mỗi ngày
                                (int) java.time.Duration.between(s.getStartedAt(),
                                        s.getEndedAt() != null ? s.getEndedAt() : s.getStartedAt()).toMinutes(),
                                s.getDurationMinutes()))
                ));

        List<FocusStatsResponse.DailyFocus> dailyBreakdown = weekStart.datesUntil(today.plusDays(1)) // Tạo dữ liệu từng ngày
                .map(d -> FocusStatsResponse.DailyFocus.builder()
                        .date(d.toString()) // Gán ngày
                        .minutes(dailyMap.getOrDefault(d, 0)) // Gán phút focus (mặc định 0)
                        .build()) // Xây dựng DailyFocus
                .toList(); // Thu thập thành danh sách

        // Top cards
        List<Object[]> topRaw = focusSessionRepository.findTopFocusedCardsByUser(currentUser.getId()); // Lấy top card focus
        List<FocusStatsResponse.TopCard> topCards = topRaw.stream() // Xử lý kết quả
                .limit(5) // Giới hạn 5 card
                .map(row -> { // Chuyển đổi từng dòng
                    Long cardId = (Long) row[0]; // Lấy ID card
                    int sessions = ((Number) row[1]).intValue(); // Lấy số phiên
                    int totalMin = ((Number) row[2]).intValue(); // Lấy tổng phút
                    String title = cardRepository.findById(cardId) // Tìm tên thẻ
                            .map(Card::getTitle) // Lấy tiêu đề
                            .orElse("Unknown"); // Mặc định nếu không tìm thấy
                    return FocusStatsResponse.TopCard.builder()
                            .cardId(cardId) // Gán ID card
                            .cardTitle(title) // Gán tiêu đề
                            .sessions(sessions) // Gán số phiên
                            .totalMinutes(totalMin) // Gán tổng phút
                            .build(); // Xây dựng TopCard
                })
                .toList(); // Thu thập thành danh sách

        return FocusStatsResponse.builder()
                .todayMinutes(todayMinutes) // Gán phút hôm nay
                .weekMinutes(weekMinutes) // Gán phút tuần này
                .monthMinutes(monthMinutes) // Gán phút tháng này
                .weekChangePercent(weekChangePercent) // Gán % thay đổi
                .dailyBreakdown(dailyBreakdown) // Gán dữ liệu từng ngày
                .topCards(topCards) // Gán top card
                .build(); // Xây dựng FocusStatsResponse
    }

    private void requireCardMember(Card card, Long userId) {
        boolean isMember = card.getMembers().stream() // Kiểm tra người dùng có trong danh sách thành viên
                .anyMatch(m -> m.getId().equals(userId));
        if (!isMember) { // Nếu không phải thành viên
            throw new UnauthorizedException("Bạn không phải là thành viên của card này."); // Ném lỗi
        }
    }

    private FocusSessionResponse toResponse(FocusSession session, int totalFocusMinutes) {
        String userName = userRepository.findById(session.getUserId()) // Lấy tên người dùng
                .map(User::getUsername) // Lấy username
                .orElse("Unknown"); // Mặc định nếu không tìm thấy

        return FocusSessionResponse.builder()
                .id(session.getId()) // Gán ID phiên
                .cardId(session.getCardId()) // Gán ID thẻ
                .userId(session.getUserId()) // Gán ID người dùng
                .userName(userName) // Gán tên người dùng
                .startedAt(session.getStartedAt()) // Gán thời gian bắt đầu
                .endedAt(session.getEndedAt()) // Gán thời gian kết thúc
                .durationMinutes(session.getDurationMinutes()) // Gán thời lượng
                .completed(session.getCompleted()) // Gán trạng thái hoàn thành
                .totalFocusMinutes(totalFocusMinutes) // Gán tổng phút focus
                .build(); // Xây dựng FocusSessionResponse
    }
}
