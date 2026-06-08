package com.okabe.dto.response;

public record NotificationPreferenceResponse(
    boolean emailAssigned, // Nhận email khi được gán việc
    boolean emailMentioned, // Nhận email khi được đề cập
    boolean emailDueSoon, // Nhận email nhắc nhở đến hạn
    boolean emailInvited // Nhận email khi được mời
) {}
