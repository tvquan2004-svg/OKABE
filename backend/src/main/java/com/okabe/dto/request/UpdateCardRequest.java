package com.okabe.dto.request;

public record UpdateCardRequest(
        String title,
        String description,
        String priority,
        String dueDate,
        Boolean isArchived
) {}
