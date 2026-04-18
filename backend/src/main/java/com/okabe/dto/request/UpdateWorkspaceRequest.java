package com.okabe.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @Size(max = 255, message = "Workspace name must not exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description
) {}
