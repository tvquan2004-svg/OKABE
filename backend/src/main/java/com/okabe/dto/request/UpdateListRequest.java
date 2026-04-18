package com.okabe.dto.request;

public record UpdateListRequest(
        String name,
        Boolean isArchived
) {}
