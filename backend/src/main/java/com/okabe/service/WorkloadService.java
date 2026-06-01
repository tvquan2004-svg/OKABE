package com.okabe.service;

import com.okabe.dto.response.WorkloadResponse;
import com.okabe.security.UserPrincipal;

import java.time.LocalDate;

public interface WorkloadService {

    /**
     * Get workload heatmap data for all members in a workspace within a date range.
     * Groups cards by member and due date, calculates total cards and estimated hours per day.
     * Marks days as OVERLOADED when total hours exceed 8.
     *
     * @param workspaceId the workspace ID
     * @param from        start date (inclusive)
     * @param to          end date (inclusive)
     * @param currentUser the authenticated user
     * @return workload data grouped by member
     */
    WorkloadResponse getWorkload(Long workspaceId, LocalDate from, LocalDate to, UserPrincipal currentUser);
}
