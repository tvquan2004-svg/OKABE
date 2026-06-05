package com.okabe.service.impl;

import com.okabe.dto.response.ActivityResponse;
import com.okabe.entity.*;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.repository.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final BoardRepository boardRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TaskListRepository taskListRepository;
    private final CardRepository cardRepository;
    private final ActivityRepository activityRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public byte[] exportBoardReport(Long boardId, String format, UserPrincipal currentUser) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
        validateAccess(board.getWorkspace().getId(), currentUser.getId());

        List<TaskList> lists = taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId);
        List<Card> cards = cardRepository.findByTaskListBoardIdAndIsArchivedFalse(boardId);
        List<ActivityResponse> activities = activityRepository.findByCardTaskListBoardIdOrderByCreatedAtDesc(boardId, PageRequest.of(0, 200))
                .stream().map(this::toActivityResponse).toList();

        if ("excel".equalsIgnoreCase(format)) {
            return generateBoardExcel(board, lists, cards, activities);
        }
        return generateBoardPdf(board, lists, cards, activities);
    }

    @Override
    public byte[] exportWorkspaceReport(Long workspaceId, String from, String to, String format, UserPrincipal currentUser) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        validateAccess(workspaceId, currentUser.getId());

        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusMonths(1);
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();

        List<Board> boards = boardRepository.findByWorkspaceIdOrderByPositionAscCreatedAtAsc(workspaceId);
        List<Card> cards = cardRepository.findAllWithMembersByWorkspace(workspaceId).stream()
                .filter(c -> {
                    LocalDate d = c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : LocalDate.now();
                    return !d.isBefore(fromDate) && !d.isAfter(toDate);
                }).toList();
        List<ActivityResponse> activities = activityRepository.findByWorkspaceAndDateRange(workspaceId, fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay())
                .stream().map(this::toActivityResponse).toList();

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);

        if ("excel".equalsIgnoreCase(format)) {
            return generateWorkspaceExcel(workspace, boards, cards, activities, members, fromDate, toDate);
        }
        return generateWorkspacePdf(workspace, boards, cards, activities, members, fromDate, toDate);
    }

    // ==================== EXCEL ====================

    private byte[] generateBoardExcel(Board board, List<TaskList> lists, List<Card> cards, List<ActivityResponse> activities) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet overviewSheet = wb.createSheet("Tổng quan");
            Sheet detailSheet = wb.createSheet("Chi tiết thẻ");
            Sheet activitySheet = wb.createSheet("Hoạt động");

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dateStyle = createDateStyle(wb);

            // Sheet 1: Overview
            fillBoardOverview(overviewSheet, board, lists, cards, headerStyle);

            // Sheet 2: Card details
            fillCardDetails(detailSheet, cards, headerStyle, dateStyle);

            // Sheet 3: Activities
            fillActivities(activitySheet, activities, headerStyle, dateStyle);

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet s = wb.getSheetAt(i);
                for (int j = 0; j < s.getRow(0).getLastCellNum(); j++) {
                    s.autoSizeColumn(j);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private byte[] generateWorkspaceExcel(Workspace workspace, List<Board> boards, List<Card> cards,
                                           List<ActivityResponse> activities, List<WorkspaceMember> members,
                                           LocalDate from, LocalDate to) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet overviewSheet = wb.createSheet("Tổng quan");
            Sheet detailSheet = wb.createSheet("Chi tiết thẻ");
            Sheet activitySheet = wb.createSheet("Hoạt động");

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dateStyle = createDateStyle(wb);

            fillWorkspaceOverview(overviewSheet, workspace, boards, cards, members, from, to, headerStyle);
            fillCardDetails(detailSheet, cards, headerStyle, dateStyle);
            fillActivities(activitySheet, activities, headerStyle, dateStyle);

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet s = wb.getSheetAt(i);
                for (int j = 0; j < s.getRow(0).getLastCellNum(); j++) {
                    s.autoSizeColumn(j);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private void fillBoardOverview(Sheet sheet, Board board, List<TaskList> lists, List<Card> cards, CellStyle headerStyle) {
        int r = 0;
        Row titleRow = sheet.createRow(r++);
        titleRow.createCell(0).setCellValue("Báo cáo bảng: " + board.getName());

        sheet.createRow(r++).createCell(0).setCellValue("Tổng số cột: " + lists.size());
        sheet.createRow(r++).createCell(0).setCellValue("Tổng số thẻ: " + cards.size());

        long completed = cards.stream().filter(c -> Boolean.TRUE.equals(c.getIsArchived()) || "DONE".equalsIgnoreCase(getListName(c))).count();
        double rate = cards.isEmpty() ? 0 : (completed * 100.0 / cards.size());
        sheet.createRow(r++).createCell(0).setCellValue(String.format("Tỷ lệ hoàn thành: %.1f%%", rate));

        r++;
        Row statusHeader = sheet.createRow(r++);
        statusHeader.createCell(0).setCellValue("Thẻ theo trạng thái");
        statusHeader.getCell(0).setCellStyle(headerStyle);

        Map<String, Long> byList = cards.stream()
                .collect(Collectors.groupingBy(c -> getListName(c), Collectors.counting()));
        for (Map.Entry<String, Long> e : byList.entrySet()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }

        r++;
        Row priorityHeader = sheet.createRow(r++);
        priorityHeader.createCell(0).setCellValue("Thẻ theo mức ưu tiên");
        priorityHeader.getCell(0).setCellStyle(headerStyle);

        Map<String, Long> byPriority = cards.stream()
                .collect(Collectors.groupingBy(c -> c.getPriority() != null ? c.getPriority().name() : "NONE", Collectors.counting()));
        for (Map.Entry<String, Long> e : byPriority.entrySet()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }
    }

    private void fillWorkspaceOverview(Sheet sheet, Workspace workspace, List<Board> boards, List<Card> cards,
                                        List<WorkspaceMember> members, LocalDate from, LocalDate to, CellStyle headerStyle) {
        int r = 0;
        Row titleRow = sheet.createRow(r++);
        titleRow.createCell(0).setCellValue("Báo cáo không gian làm việc: " + workspace.getName());

        sheet.createRow(r++).createCell(0).setCellValue("Khoảng thời gian: " + from + " → " + to);
        sheet.createRow(r++).createCell(0).setCellValue("Tổng số bảng: " + boards.size());
        sheet.createRow(r++).createCell(0).setCellValue("Tổng số thành viên: " + members.size());
        sheet.createRow(r++).createCell(0).setCellValue("Tổng số thẻ: " + cards.size());

        long completed = cards.stream().filter(c -> Boolean.TRUE.equals(c.getIsArchived())).count();
        double rate = cards.isEmpty() ? 0 : (completed * 100.0 / cards.size());
        sheet.createRow(r++).createCell(0).setCellValue(String.format("Tỷ lệ hoàn thành: %.1f%%", rate));

        r++;
        Row boardHeader = sheet.createRow(r++);
        boardHeader.createCell(0).setCellValue("Thẻ theo bảng");
        boardHeader.getCell(0).setCellStyle(headerStyle);

        Map<Long, String> boardNames = boards.stream().collect(Collectors.toMap(Board::getId, Board::getName));
        Map<String, Long> byBoard = cards.stream()
                .collect(Collectors.groupingBy(c -> getBoardName(c, boardNames), Collectors.counting()));
        for (Map.Entry<String, Long> e : byBoard.entrySet()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(e.getValue());
        }
    }

    private void fillCardDetails(Sheet sheet, List<Card> cards, CellStyle headerStyle, CellStyle dateStyle) {
        String[] headers = {"Tiêu đề", "Cột", "Người được giao", "Hạn chót", "Mức ưu tiên", "Nhãn", "Ngày tạo"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int r = 1;
        for (Card c : cards) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(c.getTitle());
            row.createCell(1).setCellValue(c.getTaskList() != null ? c.getTaskList().getName() : "");
            row.createCell(2).setCellValue(c.getMembers() != null
                    ? c.getMembers().stream().map(u -> u.getUsername()).collect(Collectors.joining(", "))
                    : "");
            if (c.getDueDate() != null) {
                Cell dateCell = row.createCell(3);
                dateCell.setCellValue(c.getDueDate().format(DATE_FMT));
                dateCell.setCellStyle(dateStyle);
            } else {
                row.createCell(3).setCellValue("");
            }
            row.createCell(4).setCellValue(c.getPriority() != null ? c.getPriority().name() : "");
            row.createCell(5).setCellValue(c.getLabels() != null
                    ? c.getLabels().stream().map(l -> l.getName()).collect(Collectors.joining(", "))
                    : "");
            if (c.getCreatedAt() != null) {
                Cell dateCell = row.createCell(6);
                dateCell.setCellValue(c.getCreatedAt().format(DATE_FMT));
                dateCell.setCellStyle(dateStyle);
            } else {
                row.createCell(6).setCellValue("");
            }
        }
    }

    private void fillActivities(Sheet sheet, List<ActivityResponse> activities, CellStyle headerStyle, CellStyle dateStyle) {
        String[] headers = {"Thời gian", "Người dùng", "Hành động", "Mô tả"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int r = 1;
        for (ActivityResponse a : activities) {
            Row row = sheet.createRow(r++);
            if (a.createdAt() != null) {
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(a.createdAt().format(DATETIME_FMT));
                dateCell.setCellStyle(dateStyle);
            }
            row.createCell(1).setCellValue(a.username() != null ? a.username() : "");
            row.createCell(2).setCellValue(a.actionType() != null ? a.actionType() : "");
            row.createCell(3).setCellValue(a.description() != null ? a.description() : "");
        }
    }

    // ==================== PDF ====================

    private byte[] generateBoardPdf(Board board, List<TaskList> lists, List<Card> cards, List<ActivityResponse> activities) {
        long completed = cards.stream().filter(c -> Boolean.TRUE.equals(c.getIsArchived()) || "DONE".equalsIgnoreCase(getListName(c))).count();
        double rate = cards.isEmpty() ? 0 : (completed * 100.0 / cards.size());

        Map<String, Long> byList = cards.stream()
                .collect(Collectors.groupingBy(c -> getListName(c), Collectors.counting()));
        Map<String, Long> byPriority = cards.stream()
                .collect(Collectors.groupingBy(c -> c.getPriority() != null ? c.getPriority().name() : "NONE", Collectors.counting()));

        String html = buildBoardPdfHtml(board.getName(), lists.size(), cards.size(), rate, byList, byPriority, cards, activities);
        return renderPdf(html);
    }

    private byte[] generateWorkspacePdf(Workspace workspace, List<Board> boards, List<Card> cards,
                                          List<ActivityResponse> activities, List<WorkspaceMember> members,
                                          LocalDate from, LocalDate to) {
        long completed = cards.stream().filter(c -> Boolean.TRUE.equals(c.getIsArchived())).count();
        double rate = cards.isEmpty() ? 0 : (completed * 100.0 / cards.size());

        Map<Long, String> boardNames = boards.stream().collect(Collectors.toMap(Board::getId, Board::getName));
        Map<String, Long> byBoard = cards.stream()
                .collect(Collectors.groupingBy(c -> getBoardName(c, boardNames), Collectors.counting()));

        String html = buildWorkspacePdfHtml(workspace.getName(), from, to, boards.size(), members.size(), cards.size(), rate, byBoard, cards, activities);
        return renderPdf(html);
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            try {
                java.net.URL fontUrl = getClass().getResource("/fonts/NotoSans-Regular.ttf");
                if (fontUrl != null) {
                    try (InputStream is = fontUrl.openStream()) {
                        Path tempFile = Files.createTempFile("noto-", ".ttf");
                        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                        renderer.getFontResolver().addFont(tempFile.toAbsolutePath().toString(), "Identity-H", true);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not load NotoSans font: {}", e.getMessage());
            }
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private String buildBoardPdfHtml(String boardName, int listCount, int cardCount, double rate,
                                      Map<String, Long> byList, Map<String, Long> byPriority,
                                      List<Card> cards, List<ActivityResponse> activities) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html>
            <html><head>                <meta charset="UTF-8"/>
            <style>
                body { font-family: 'Noto Sans', 'Arial Unicode MS', 'DejaVu Sans', sans-serif; font-size: 12px; color: #333; padding: 30px; }
                h1 { color: #6366f1; font-size: 20px; border-bottom: 2px solid #6366f1; padding-bottom: 8px; }
                h2 { color: #444; font-size: 15px; margin-top: 24px; }
                table { width: 100%; border-collapse: collapse; margin: 12px 0; }
                th { background: #6366f1; color: #fff; padding: 8px 10px; text-align: left; font-size: 11px; }
                td { padding: 6px 10px; border-bottom: 1px solid #ddd; font-size: 11px; }
                .stat { display: inline-block; background: #f0f0ff; padding: 12px 20px; margin: 6px; border-radius: 8px; text-align: center; }
                .stat-value { font-size: 24px; font-weight: 700; color: #6366f1; }
                .stat-label { font-size: 11px; color: #666; }
                .footer { margin-top: 40px; font-size: 10px; color: #999; text-align: center; border-top: 1px solid #ddd; padding-top: 12px; }
            </style></head><body>
            <h1>Báo cáo bảng: """ + escapeHtml(boardName) + "</h1>\n");

        sb.append("<p>Ngày xuất: ").append(LocalDateTime.now().format(DATETIME_FMT)).append("</p>\n");
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(cardCount).append("</div><div class=\"stat-label\">Tổng thẻ</div></div>\n");
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(listCount).append("</div><div class=\"stat-label\">Tổng cột</div></div>\n");
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(String.format("%.1f%%", rate)).append("</div><div class=\"stat-label\">Hoàn thành</div></div>\n");

        sb.append("<h2>Thẻ theo trạng thái</h2>\n<table><tr><th>Trạng thái</th><th>Số lượng</th></tr>\n");
        for (Map.Entry<String, Long> e : byList.entrySet()) {
            sb.append("<tr><td>").append(escapeHtml(e.getKey())).append("</td><td>").append(e.getValue()).append("</td></tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<h2>Thẻ theo mức ưu tiên</h2>\n<table><tr><th>Mức ưu tiên</th><th>Số lượng</th></tr>\n");
        for (Map.Entry<String, Long> e : byPriority.entrySet()) {
            sb.append("<tr><td>").append(escapeHtml(e.getKey())).append("</td><td>").append(e.getValue()).append("</td></tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<h2>Chi tiết thẻ</h2>\n<table><tr><th>Tiêu đề</th><th>Cột</th><th>Người được giao</th><th>Hạn chót</th><th>Ưu tiên</th></tr>\n");
        for (Card c : cards) {
            sb.append("<tr>");
            sb.append("<td>").append(escapeHtml(c.getTitle())).append("</td>");
            sb.append("<td>").append(escapeHtml(getListName(c))).append("</td>");
            sb.append("<td>").append(c.getMembers() != null
                    ? c.getMembers().stream().map(User::getUsername).collect(Collectors.joining(", "))
                    : "").append("</td>");
            sb.append("<td>").append(c.getDueDate() != null ? c.getDueDate().format(DATE_FMT) : "").append("</td>");
            sb.append("<td>").append(c.getPriority() != null ? c.getPriority().name() : "").append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<h2>Hoạt động gần đây</h2>\n<table><tr><th>Thời gian</th><th>Người dùng</th><th>Hành động</th></tr>\n");
        for (ActivityResponse a : activities.stream().limit(50).toList()) {
            sb.append("<tr>");
            sb.append("<td>").append(a.createdAt() != null ? a.createdAt().format(DATETIME_FMT) : "").append("</td>");
            sb.append("<td>").append(escapeHtml(a.username())).append("</td>");
            sb.append("<td>").append(escapeHtml(a.actionType())).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<div class=\"footer\">OKABE - Báo cáo được tạo tự động</div>\n");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String buildWorkspacePdfHtml(String wsName, LocalDate from, LocalDate to, int boardCount,
                                          int memberCount, int cardCount, double rate,
                                          Map<String, Long> byBoard, List<Card> cards, List<ActivityResponse> activities) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html>
            <html><head>                <meta charset="UTF-8"/>
            <style>
                body { font-family: 'Noto Sans', 'Arial Unicode MS', 'DejaVu Sans', sans-serif; font-size: 12px; color: #333; padding: 30px; }
                h1 { color: #6366f1; font-size: 20px; border-bottom: 2px solid #6366f1; padding-bottom: 8px; }
                h2 { color: #444; font-size: 15px; margin-top: 24px; }
                table { width: 100%; border-collapse: collapse; margin: 12px 0; }
                th { background: #6366f1; color: #fff; padding: 8px 10px; text-align: left; font-size: 11px; }
                td { padding: 6px 10px; border-bottom: 1px solid #ddd; font-size: 11px; }
                .stat { display: inline-block; background: #f0f0ff; padding: 12px 20px; margin: 6px; border-radius: 8px; text-align: center; }
                .stat-value { font-size: 24px; font-weight: 700; color: #6366f1; }
                .stat-label { font-size: 11px; color: #666; }
                .footer { margin-top: 40px; font-size: 10px; color: #999; text-align: center; border-top: 1px solid #ddd; padding-top: 12px; }
            </style></head><body>
            <h1>Báo cáo không gian làm việc: """ + escapeHtml(wsName) + "</h1>\n");

        sb.append("<p>Khoảng thời gian: ").append(from).append(" → ").append(to).append("</p>\n");
        sb.append("<p>Ngày xuất: ").append(LocalDateTime.now().format(DATETIME_FMT)).append("</p>\n");
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(boardCount).append("</div><div class=\"stat-label\">Bảng</div></div>\n");
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(memberCount).append("</div><div class=\"stat-label\">Thành viên</div></div>\n");
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(cardCount).append("</div><div class=\"stat-label\">Thẻ</div></div>\n");
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(String.format("%.1f%%", rate)).append("</div><div class=\"stat-label\">Hoàn thành</div></div>\n");

        sb.append("<h2>Thẻ theo bảng</h2>\n<table><tr><th>Bảng</th><th>Số lượng</th></tr>\n");
        for (Map.Entry<String, Long> e : byBoard.entrySet()) {
            sb.append("<tr><td>").append(escapeHtml(e.getKey())).append("</td><td>").append(e.getValue()).append("</td></tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<h2>Chi tiết thẻ</h2>\n<table><tr><th>Tiêu đề</th><th>Hạn chót</th><th>Ưu tiên</th></tr>\n");
        for (Card c : cards) {
            sb.append("<tr>");
            sb.append("<td>").append(escapeHtml(c.getTitle())).append("</td>");
            sb.append("<td>").append(c.getDueDate() != null ? c.getDueDate().format(DATE_FMT) : "").append("</td>");
            sb.append("<td>").append(c.getPriority() != null ? c.getPriority().name() : "").append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<h2>Hoạt động gần đây</h2>\n<table><tr><th>Thời gian</th><th>Người dùng</th><th>Hành động</th></tr>\n");
        for (ActivityResponse a : activities.stream().limit(50).toList()) {
            sb.append("<tr>");
            sb.append("<td>").append(a.createdAt() != null ? a.createdAt().format(DATETIME_FMT) : "").append("</td>");
            sb.append("<td>").append(escapeHtml(a.username())).append("</td>");
            sb.append("<td>").append(escapeHtml(a.actionType())).append("</td>");
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<div class=\"footer\">OKABE - Báo cáo được tạo tự động</div>\n");
        sb.append("</body></html>");
        return sb.toString();
    }

    // ==================== HELPERS ====================

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("dd/MM/yyyy"));
        return style;
    }

    private String getListName(Card card) {
        return card.getTaskList() != null ? card.getTaskList().getName() : "";
    }

    private String getBoardName(Card card, Map<Long, String> boardNames) {
        if (card.getTaskList() != null && card.getTaskList().getBoard() != null) {
            return boardNames.getOrDefault(card.getTaskList().getBoard().getId(), "");
        }
        return "";
    }

    private void validateAccess(Long workspaceId, Long userId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new com.okabe.exception.UnauthorizedException("You are not a member of this workspace"));
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private ActivityResponse toActivityResponse(Activity a) {
        return ActivityResponse.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .username(a.getUser().getUsername())
                .avatarUrl(a.getUser().getAvatarUrl())
                .actionType(a.getActionType())
                .description(a.getDescription())
                .cardId(a.getCard() != null ? a.getCard().getId() : null)
                .createdAt(a.getCreatedAt())
                .build();
    }
}
