package com.okabe.service;

import com.okabe.security.UserPrincipal;

public interface ReportService {

    // Xuất báo cáo board dưới dạng file (pdf/excel)
    byte[] exportBoardReport(Long boardId, String format, UserPrincipal currentUser);

    // Xuất báo cáo workspace theo khoảng thời gian dưới dạng file (pdf/excel)
    byte[] exportWorkspaceReport(Long workspaceId, String from, String to, String format, UserPrincipal currentUser);
}
