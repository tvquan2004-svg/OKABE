package com.okabe.dto.request;

public record UpdateNotificationPreferenceRequest(
    boolean emailAssigned,
    boolean emailMentioned,
    boolean emailDueSoon,
    boolean emailInvited
) {}
