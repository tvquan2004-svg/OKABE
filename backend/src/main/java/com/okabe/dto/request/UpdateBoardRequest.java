package com.okabe.dto.request;

public record UpdateBoardRequest(
        String name,
        String description,
        String background,
        Boolean isStarred,
        Boolean isArchived
) {}
