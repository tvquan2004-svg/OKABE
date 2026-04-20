package com.okabe.dto.response;

public record NotificationPreferenceResponse(
    boolean emailAssigned,
    boolean emailMentioned,
    boolean emailDueSoon,
    boolean emailInvited
) {}
