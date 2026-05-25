package com.okabe.dto.response;

import java.time.LocalDate;

public record StandupSummary(
    Long userId,
    String userName,
    String avatarUrl,
    LocalDate date,
    String done,
    String inProgress,
    String blocked
) {
}
