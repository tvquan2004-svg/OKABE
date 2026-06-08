package com.okabe.dto.request;

public record UpdateNotificationPreferenceRequest(
    boolean emailAssigned, // Nhận email khi được gán thẻ
    boolean emailMentioned, // Nhận email khi được đề cập
    boolean emailDueSoon, // Nhận email nhắc nhở đến hạn
    boolean emailInvited // Nhận email khi được mời vào workspace
) {}
