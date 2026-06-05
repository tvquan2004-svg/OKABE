package com.okabe.service;

import com.okabe.security.UserPrincipal;

public interface ReportService {

    byte[] exportBoardReport(Long boardId, String format, UserPrincipal currentUser);

    byte[] exportWorkspaceReport(Long workspaceId, String from, String to, String format, UserPrincipal currentUser);
}
