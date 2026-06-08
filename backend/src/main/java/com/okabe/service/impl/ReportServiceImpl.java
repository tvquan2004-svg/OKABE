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
        Board board = boardRepository.findById(boardId) // Tìm bảng
                .orElseThrow(() -> new ResourceNotFoundException("Board", boardId));
        validateAccess(board.getWorkspace().getId(), currentUser.getId()); // Kiểm tra quyền

        List<TaskList> lists = taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId); // Lấy danh sách cột
        List<Card> cards = cardRepository.findByTaskListBoardIdAndIsArchivedFalse(boardId); // Lấy danh sách card
        List<ActivityResponse> activities = activityRepository.findByCardTaskListBoardIdOrderByCreatedAtDesc(boardId, PageRequest.of(0, 200)) // Lấy hoạt động
                .stream().map(this::toActivityResponse).toList();

        if ("excel".equalsIgnoreCase(format)) { // Nếu định dạng Excel
            return generateBoardExcel(board, lists, cards, activities); // Xuất Excel
        }
        return generateBoardPdf(board, lists, cards, activities); // Xuất PDF
    }

    @Override
    public byte[] exportWorkspaceReport(Long workspaceId, String from, String to, String format, UserPrincipal currentUser) {
        Workspace workspace = workspaceRepository.findById(workspaceId) // Tìm workspace
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        validateAccess(workspaceId, currentUser.getId()); // Kiểm tra quyền

        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusMonths(1); // Ngày bắt đầu
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now(); // Ngày kết thúc

        List<Board> boards = boardRepository.findByWorkspaceIdOrderByPositionAscCreatedAtAsc(workspaceId); // Lấy danh sách bảng
        List<Card> cards = cardRepository.findAllWithMembersByWorkspace(workspaceId).stream() // Lấy card trong khoảng ngày
                .filter(c -> {
                    LocalDate d = c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : LocalDate.now(); // Lấy ngày tạo
                    return !d.isBefore(fromDate) && !d.isAfter(toDate); // Lọc trong khoảng
                }).toList();
        List<ActivityResponse> activities = activityRepository.findByWorkspaceAndDateRange(workspaceId, fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay()) // Lấy hoạt động
                .stream().map(this::toActivityResponse).toList();

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId); // Lấy thành viên

        if ("excel".equalsIgnoreCase(format)) { // Nếu định dạng Excel
            return generateWorkspaceExcel(workspace, boards, cards, activities, members, fromDate, toDate); // Xuất Excel
        }
        return generateWorkspacePdf(workspace, boards, cards, activities, members, fromDate, toDate); // Xuất PDF
    }

    // ==================== EXCEL ====================

    private byte[] generateBoardExcel(Board board, List<TaskList> lists, List<Card> cards, List<ActivityResponse> activities) {
        try (Workbook wb = new XSSFWorkbook()) { // Tạo workbook Excel
            Sheet overviewSheet = wb.createSheet("Tổng quan"); // Sheet tổng quan
            Sheet detailSheet = wb.createSheet("Chi tiết thẻ"); // Sheet chi tiết
            Sheet activitySheet = wb.createSheet("Hoạt động"); // Sheet hoạt động

            CellStyle headerStyle = createHeaderStyle(wb); // Tạo style header
            CellStyle dateStyle = createDateStyle(wb); // Tạo style ngày tháng

            // Sheet 1: Overview
            fillBoardOverview(overviewSheet, board, lists, cards, headerStyle); // Điền tổng quan

            // Sheet 2: Card details
            fillCardDetails(detailSheet, cards, headerStyle, dateStyle); // Điền chi tiết thẻ

            // Sheet 3: Activities
            fillActivities(activitySheet, activities, headerStyle, dateStyle); // Điền hoạt động

            for (int i = 0; i < wb.getNumberOfSheets(); i++) { // Duyệt các sheet
                Sheet s = wb.getSheetAt(i); // Lấy sheet
                for (int j = 0; j < s.getRow(0).getLastCellNum(); j++) { // Duyệt cột
                    s.autoSizeColumn(j); // Tự động điều chỉnh độ rộng
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream(); // Tạo output stream
            wb.write(out); // Ghi workbook
            return out.toByteArray(); // Trả về mảng byte
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report", e); // Ném lỗi
        }
    }

    private byte[] generateWorkspaceExcel(Workspace workspace, List<Board> boards, List<Card> cards,
                                           List<ActivityResponse> activities, List<WorkspaceMember> members,
                                           LocalDate from, LocalDate to) {
        try (Workbook wb = new XSSFWorkbook()) { // Tạo workbook Excel
            Sheet overviewSheet = wb.createSheet("Tổng quan"); // Sheet tổng quan
            Sheet detailSheet = wb.createSheet("Chi tiết thẻ"); // Sheet chi tiết
            Sheet activitySheet = wb.createSheet("Hoạt động"); // Sheet hoạt động

            CellStyle headerStyle = createHeaderStyle(wb); // Tạo style header
            CellStyle dateStyle = createDateStyle(wb); // Tạo style ngày tháng

            fillWorkspaceOverview(overviewSheet, workspace, boards, cards, members, from, to, headerStyle); // Điền tổng quan
            fillCardDetails(detailSheet, cards, headerStyle, dateStyle); // Điền chi tiết thẻ
            fillActivities(activitySheet, activities, headerStyle, dateStyle); // Điền hoạt động

            for (int i = 0; i < wb.getNumberOfSheets(); i++) { // Duyệt sheet
                Sheet s = wb.getSheetAt(i); // Lấy sheet
                for (int j = 0; j < s.getRow(0).getLastCellNum(); j++) { // Duyệt cột
                    s.autoSizeColumn(j); // Tự động điều chỉnh
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream(); // Tạo output stream
            wb.write(out); // Ghi workbook
            return out.toByteArray(); // Trả về mảng byte
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report", e); // Ném lỗi
        }
    }

    private void fillBoardOverview(Sheet sheet, Board board, List<TaskList> lists, List<Card> cards, CellStyle headerStyle) {
        int r = 0; // Dòng hiện tại
        Row titleRow = sheet.createRow(r++); // Tạo dòng tiêu đề
        titleRow.createCell(0).setCellValue("Báo cáo bảng: " + board.getName()); // Gán tên bảng

        sheet.createRow(r++).createCell(0).setCellValue("Tổng số cột: " + lists.size()); // Số cột
        sheet.createRow(r++).createCell(0).setCellValue("Tổng số thẻ: " + cards.size()); // Số thẻ

        long completed = cards.stream().filter(c -> Boolean.TRUE.equals(c.getIsArchived()) || "DONE".equalsIgnoreCase(getListName(c))).count(); // Đếm đã hoàn thành
        double rate = cards.isEmpty() ? 0 : (completed * 100.0 / cards.size()); // Tính tỷ lệ
        sheet.createRow(r++).createCell(0).setCellValue(String.format("Tỷ lệ hoàn thành: %.1f%%", rate)); // Ghi tỷ lệ

        r++; // Dòng trống
        Row statusHeader = sheet.createRow(r++); // Tạo header trạng thái
        statusHeader.createCell(0).setCellValue("Thẻ theo trạng thái"); // Gán nhãn
        statusHeader.getCell(0).setCellStyle(headerStyle); // Áp dụng style

        Map<String, Long> byList = cards.stream() // Nhóm card theo cột
                .collect(Collectors.groupingBy(c -> getListName(c), Collectors.counting()));
        for (Map.Entry<String, Long> e : byList.entrySet()) { // Duyệt kết quả
            Row row = sheet.createRow(r++); // Tạo dòng
            row.createCell(0).setCellValue(e.getKey()); // Tên cột
            row.createCell(1).setCellValue(e.getValue()); // Số lượng
        }

        r++; // Dòng trống
        Row priorityHeader = sheet.createRow(r++); // Tạo header ưu tiên
        priorityHeader.createCell(0).setCellValue("Thẻ theo mức ưu tiên"); // Gán nhãn
        priorityHeader.getCell(0).setCellStyle(headerStyle); // Áp dụng style

        Map<String, Long> byPriority = cards.stream() // Nhóm theo mức ưu tiên
                .collect(Collectors.groupingBy(c -> c.getPriority() != null ? c.getPriority().name() : "NONE", Collectors.counting()));
        for (Map.Entry<String, Long> e : byPriority.entrySet()) { // Duyệt kết quả
            Row row = sheet.createRow(r++); // Tạo dòng
            row.createCell(0).setCellValue(e.getKey()); // Mức ưu tiên
            row.createCell(1).setCellValue(e.getValue()); // Số lượng
        }
    }

    private void fillWorkspaceOverview(Sheet sheet, Workspace workspace, List<Board> boards, List<Card> cards,
                                        List<WorkspaceMember> members, LocalDate from, LocalDate to, CellStyle headerStyle) {
        int r = 0; // Dòng hiện tại
        Row titleRow = sheet.createRow(r++); // Tạo dòng tiêu đề
        titleRow.createCell(0).setCellValue("Báo cáo không gian làm việc: " + workspace.getName()); // Gán tên workspace

        sheet.createRow(r++).createCell(0).setCellValue("Khoảng thời gian: " + from + " → " + to); // Khoảng thời gian
        sheet.createRow(r++).createCell(0).setCellValue("Tổng số bảng: " + boards.size()); // Số bảng
        sheet.createRow(r++).createCell(0).setCellValue("Tổng số thành viên: " + members.size()); // Số thành viên
        sheet.createRow(r++).createCell(0).setCellValue("Tổng số thẻ: " + cards.size()); // Số thẻ

        long completed = cards.stream().filter(c -> Boolean.TRUE.equals(c.getIsArchived())).count(); // Đếm đã hoàn thành
        double rate = cards.isEmpty() ? 0 : (completed * 100.0 / cards.size()); // Tính tỷ lệ
        sheet.createRow(r++).createCell(0).setCellValue(String.format("Tỷ lệ hoàn thành: %.1f%%", rate)); // Ghi tỷ lệ

        r++; // Dòng trống
        Row boardHeader = sheet.createRow(r++); // Tạo header bảng
        boardHeader.createCell(0).setCellValue("Thẻ theo bảng"); // Gán nhãn
        boardHeader.getCell(0).setCellStyle(headerStyle); // Áp dụng style

        Map<Long, String> boardNames = boards.stream().collect(Collectors.toMap(Board::getId, Board::getName)); // Map tên bảng
        Map<String, Long> byBoard = cards.stream() // Nhóm card theo bảng
                .collect(Collectors.groupingBy(c -> getBoardName(c, boardNames), Collectors.counting()));
        for (Map.Entry<String, Long> e : byBoard.entrySet()) { // Duyệt kết quả
            Row row = sheet.createRow(r++); // Tạo dòng
            row.createCell(0).setCellValue(e.getKey()); // Tên bảng
            row.createCell(1).setCellValue(e.getValue()); // Số lượng
        }
    }

    private void fillCardDetails(Sheet sheet, List<Card> cards, CellStyle headerStyle, CellStyle dateStyle) {
        String[] headers = {"Tiêu đề", "Cột", "Người được giao", "Hạn chót", "Mức ưu tiên", "Nhãn", "Ngày tạo"}; // Header
        Row headerRow = sheet.createRow(0); // Tạo dòng header
        for (int i = 0; i < headers.length; i++) { // Duyệt header
            Cell cell = headerRow.createCell(i); // Tạo ô
            cell.setCellValue(headers[i]); // Gán giá trị
            cell.setCellStyle(headerStyle); // Áp dụng style
        }

        int r = 1; // Dòng dữ liệu bắt đầu từ 1
        for (Card c : cards) { // Duyệt card
            Row row = sheet.createRow(r++); // Tạo dòng
            row.createCell(0).setCellValue(c.getTitle()); // Tiêu đề
            row.createCell(1).setCellValue(c.getTaskList() != null ? c.getTaskList().getName() : ""); // Tên cột
            row.createCell(2).setCellValue(c.getMembers() != null // Người được giao
                    ? c.getMembers().stream().map(u -> u.getUsername()).collect(Collectors.joining(", "))
                    : "");
            if (c.getDueDate() != null) { // Nếu có hạn chót
                Cell dateCell = row.createCell(3); // Tạo ô
                dateCell.setCellValue(c.getDueDate().format(DATE_FMT)); // Gán ngày
                dateCell.setCellStyle(dateStyle); // Áp dụng style
            } else {
                row.createCell(3).setCellValue(""); // Để trống
            }
            row.createCell(4).setCellValue(c.getPriority() != null ? c.getPriority().name() : ""); // Mức ưu tiên
            row.createCell(5).setCellValue(c.getLabels() != null // Nhãn
                    ? c.getLabels().stream().map(l -> l.getName()).collect(Collectors.joining(", "))
                    : "");
            if (c.getCreatedAt() != null) { // Nếu có ngày tạo
                Cell dateCell = row.createCell(6); // Tạo ô
                dateCell.setCellValue(c.getCreatedAt().format(DATE_FMT)); // Gán ngày
                dateCell.setCellStyle(dateStyle); // Áp dụng style
            } else {
                row.createCell(6).setCellValue(""); // Để trống
            }
        }
    }

    private void fillActivities(Sheet sheet, List<ActivityResponse> activities, CellStyle headerStyle, CellStyle dateStyle) {
        String[] headers = {"Thời gian", "Người dùng", "Hành động", "Mô tả"}; // Header
        Row headerRow = sheet.createRow(0); // Tạo dòng header
        for (int i = 0; i < headers.length; i++) { // Duyệt header
            Cell cell = headerRow.createCell(i); // Tạo ô
            cell.setCellValue(headers[i]); // Gán giá trị
            cell.setCellStyle(headerStyle); // Áp dụng style
        }

        int r = 1; // Dòng dữ liệu
        for (ActivityResponse a : activities) { // Duyệt hoạt động
            Row row = sheet.createRow(r++); // Tạo dòng
            if (a.createdAt() != null) { // Nếu có thời gian
                Cell dateCell = row.createCell(0); // Tạo ô
                dateCell.setCellValue(a.createdAt().format(DATETIME_FMT)); // Gán thời gian
                dateCell.setCellStyle(dateStyle); // Áp dụng style
            }
            row.createCell(1).setCellValue(a.username() != null ? a.username() : ""); // Người dùng
            row.createCell(2).setCellValue(a.actionType() != null ? a.actionType() : ""); // Hành động
            row.createCell(3).setCellValue(a.description() != null ? a.description() : ""); // Mô tả
        }
    }

    // ==================== PDF ====================

    private byte[] generateBoardPdf(Board board, List<TaskList> lists, List<Card> cards, List<ActivityResponse> activities) {
        long completed = cards.stream().filter(c -> Boolean.TRUE.equals(c.getIsArchived()) || "DONE".equalsIgnoreCase(getListName(c))).count(); // Đếm đã hoàn thành
        double rate = cards.isEmpty() ? 0 : (completed * 100.0 / cards.size()); // Tính tỷ lệ

        Map<String, Long> byList = cards.stream() // Nhóm theo cột
                .collect(Collectors.groupingBy(c -> getListName(c), Collectors.counting()));
        Map<String, Long> byPriority = cards.stream() // Nhóm theo ưu tiên
                .collect(Collectors.groupingBy(c -> c.getPriority() != null ? c.getPriority().name() : "NONE", Collectors.counting()));

        String html = buildBoardPdfHtml(board.getName(), lists.size(), cards.size(), rate, byList, byPriority, cards, activities); // Xây HTML
        return renderPdf(html); // Render PDF
    }

    private byte[] generateWorkspacePdf(Workspace workspace, List<Board> boards, List<Card> cards,
                                          List<ActivityResponse> activities, List<WorkspaceMember> members,
                                          LocalDate from, LocalDate to) {
        long completed = cards.stream().filter(c -> Boolean.TRUE.equals(c.getIsArchived())).count(); // Đếm đã hoàn thành
        double rate = cards.isEmpty() ? 0 : (completed * 100.0 / cards.size()); // Tính tỷ lệ

        Map<Long, String> boardNames = boards.stream().collect(Collectors.toMap(Board::getId, Board::getName)); // Map tên bảng
        Map<String, Long> byBoard = cards.stream() // Nhóm theo bảng
                .collect(Collectors.groupingBy(c -> getBoardName(c, boardNames), Collectors.counting()));

        String html = buildWorkspacePdfHtml(workspace.getName(), from, to, boards.size(), members.size(), cards.size(), rate, byBoard, cards, activities); // Xây HTML
        return renderPdf(html); // Render PDF
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer(); // Tạo renderer
            try {
                java.net.URL fontUrl = getClass().getResource("/fonts/NotoSans-Regular.ttf"); // Tìm font
                if (fontUrl != null) { // Nếu có font
                    try (InputStream is = fontUrl.openStream()) { // Mở stream
                        Path tempFile = Files.createTempFile("noto-", ".ttf"); // Tạo file tạm
                        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING); // Copy font
                        renderer.getFontResolver().addFont(tempFile.toAbsolutePath().toString(), "Identity-H", true); // Đăng ký font
                    }
                }
            } catch (Exception e) {
                log.warn("Could not load NotoSans font: {}", e.getMessage()); // Ghi log cảnh báo
            }
            renderer.setDocumentFromString(html); // Đặt HTML
            renderer.layout(); // Layout
            renderer.createPDF(out); // Tạo PDF
            return out.toByteArray(); // Trả về byte
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e); // Ném lỗi
        }
    }

    private String buildBoardPdfHtml(String boardName, int listCount, int cardCount, double rate,
                                      Map<String, Long> byList, Map<String, Long> byPriority,
                                      List<Card> cards, List<ActivityResponse> activities) {
        StringBuilder sb = new StringBuilder(); // Tạo StringBuilder
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
            <h1>Báo cáo bảng: """ + escapeHtml(boardName) + "</h1>\n"); // Tiêu đề

        sb.append("<p>Ngày xuất: ").append(LocalDateTime.now().format(DATETIME_FMT)).append("</p>\n"); // Ngày xuất
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(cardCount).append("</div><div class=\"stat-label\">Tổng thẻ</div></div>\n"); // Tổng thẻ
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(listCount).append("</div><div class=\"stat-label\">Tổng cột</div></div>\n"); // Tổng cột
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(String.format("%.1f%%", rate)).append("</div><div class=\"stat-label\">Hoàn thành</div></div>\n"); // Tỷ lệ

        sb.append("<h2>Thẻ theo trạng thái</h2>\n<table><tr><th>Trạng thái</th><th>Số lượng</th></tr>\n"); // Header bảng
        for (Map.Entry<String, Long> e : byList.entrySet()) { // Duyệt theo cột
            sb.append("<tr><td>").append(escapeHtml(e.getKey())).append("</td><td>").append(e.getValue()).append("</td></tr>\n"); // Dòng
        }
        sb.append("</table>\n");

        sb.append("<h2>Thẻ theo mức ưu tiên</h2>\n<table><tr><th>Mức ưu tiên</th><th>Số lượng</th></tr>\n"); // Header ưu tiên
        for (Map.Entry<String, Long> e : byPriority.entrySet()) { // Duyệt ưu tiên
            sb.append("<tr><td>").append(escapeHtml(e.getKey())).append("</td><td>").append(e.getValue()).append("</td></tr>\n"); // Dòng
        }
        sb.append("</table>\n");

        sb.append("<h2>Chi tiết thẻ</h2>\n<table><tr><th>Tiêu đề</th><th>Cột</th><th>Người được giao</th><th>Hạn chót</th><th>Ưu tiên</th></tr>\n"); // Header chi tiết
        for (Card c : cards) { // Duyệt card
            sb.append("<tr>");
            sb.append("<td>").append(escapeHtml(c.getTitle())).append("</td>"); // Tiêu đề
            sb.append("<td>").append(escapeHtml(getListName(c))).append("</td>"); // Cột
            sb.append("<td>").append(c.getMembers() != null // Người được giao
                    ? c.getMembers().stream().map(User::getUsername).collect(Collectors.joining(", "))
                    : "").append("</td>");
            sb.append("<td>").append(c.getDueDate() != null ? c.getDueDate().format(DATE_FMT) : "").append("</td>"); // Hạn chót
            sb.append("<td>").append(c.getPriority() != null ? c.getPriority().name() : "").append("</td>"); // Ưu tiên
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<h2>Hoạt động gần đây</h2>\n<table><tr><th>Thời gian</th><th>Người dùng</th><th>Hành động</th></tr>\n"); // Header hoạt động
        for (ActivityResponse a : activities.stream().limit(50).toList()) { // Duyệt 50 hoạt động gần nhất
            sb.append("<tr>");
            sb.append("<td>").append(a.createdAt() != null ? a.createdAt().format(DATETIME_FMT) : "").append("</td>"); // Thời gian
            sb.append("<td>").append(escapeHtml(a.username())).append("</td>"); // Người dùng
            sb.append("<td>").append(escapeHtml(a.actionType())).append("</td>"); // Hành động
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<div class=\"footer\">OKABE - Báo cáo được tạo tự động</div>\n"); // Footer
        sb.append("</body></html>"); // Kết thúc HTML
        return sb.toString(); // Trả về HTML
    }

    private String buildWorkspacePdfHtml(String wsName, LocalDate from, LocalDate to, int boardCount,
                                          int memberCount, int cardCount, double rate,
                                          Map<String, Long> byBoard, List<Card> cards, List<ActivityResponse> activities) {
        StringBuilder sb = new StringBuilder(); // Tạo StringBuilder
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
            <h1>Báo cáo không gian làm việc: """ + escapeHtml(wsName) + "</h1>\n"); // Tiêu đề

        sb.append("<p>Khoảng thời gian: ").append(from).append(" → ").append(to).append("</p>\n"); // Khoảng thời gian
        sb.append("<p>Ngày xuất: ").append(LocalDateTime.now().format(DATETIME_FMT)).append("</p>\n"); // Ngày xuất
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(boardCount).append("</div><div class=\"stat-label\">Bảng</div></div>\n"); // Số bảng
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(memberCount).append("</div><div class=\"stat-label\">Thành viên</div></div>\n"); // Số thành viên
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(cardCount).append("</div><div class=\"stat-label\">Thẻ</div></div>\n"); // Số thẻ
        sb.append("<div class=\"stat\"><div class=\"stat-value\">").append(String.format("%.1f%%", rate)).append("</div><div class=\"stat-label\">Hoàn thành</div></div>\n"); // Tỷ lệ

        sb.append("<h2>Thẻ theo bảng</h2>\n<table><tr><th>Bảng</th><th>Số lượng</th></tr>\n"); // Header bảng
        for (Map.Entry<String, Long> e : byBoard.entrySet()) { // Duyệt theo bảng
            sb.append("<tr><td>").append(escapeHtml(e.getKey())).append("</td><td>").append(e.getValue()).append("</td></tr>\n"); // Dòng
        }
        sb.append("</table>\n");

        sb.append("<h2>Chi tiết thẻ</h2>\n<table><tr><th>Tiêu đề</th><th>Hạn chót</th><th>Ưu tiên</th></tr>\n"); // Header chi tiết
        for (Card c : cards) { // Duyệt card
            sb.append("<tr>");
            sb.append("<td>").append(escapeHtml(c.getTitle())).append("</td>"); // Tiêu đề
            sb.append("<td>").append(c.getDueDate() != null ? c.getDueDate().format(DATE_FMT) : "").append("</td>"); // Hạn chót
            sb.append("<td>").append(c.getPriority() != null ? c.getPriority().name() : "").append("</td>"); // Ưu tiên
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<h2>Hoạt động gần đây</h2>\n<table><tr><th>Thời gian</th><th>Người dùng</th><th>Hành động</th></tr>\n"); // Header hoạt động
        for (ActivityResponse a : activities.stream().limit(50).toList()) { // Duyệt 50 hoạt động
            sb.append("<tr>");
            sb.append("<td>").append(a.createdAt() != null ? a.createdAt().format(DATETIME_FMT) : "").append("</td>"); // Thời gian
            sb.append("<td>").append(escapeHtml(a.username())).append("</td>"); // Người dùng
            sb.append("<td>").append(escapeHtml(a.actionType())).append("</td>"); // Hành động
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<div class=\"footer\">OKABE - Báo cáo được tạo tự động</div>\n"); // Footer
        sb.append("</body></html>"); // Kết thúc HTML
        return sb.toString(); // Trả về HTML
    }

    // ==================== HELPERS ====================

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle(); // Tạo style
        Font font = wb.createFont(); // Tạo font
        font.setBold(true); // Đậm
        font.setColor(IndexedColors.WHITE.getIndex()); // Màu trắng
        style.setFont(font); // Gán font
        style.setFillForegroundColor(IndexedColors.INDIGO.getIndex()); // Màu nền indigo
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND); // Kiểu tô
        style.setBorderBottom(BorderStyle.THIN); // Viền dưới
        style.setBorderTop(BorderStyle.THIN); // Viền trên
        style.setBorderLeft(BorderStyle.THIN); // Viền trái
        style.setBorderRight(BorderStyle.THIN); // Viền phải
        return style; // Trả về style
    }

    private CellStyle createDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle(); // Tạo style
        style.setDataFormat(wb.createDataFormat().getFormat("dd/MM/yyyy")); // Định dạng ngày
        return style; // Trả về style
    }

    private String getListName(Card card) {
        return card.getTaskList() != null ? card.getTaskList().getName() : ""; // Lấy tên cột
    }

    private String getBoardName(Card card, Map<Long, String> boardNames) {
        if (card.getTaskList() != null && card.getTaskList().getBoard() != null) { // Nếu có board
            return boardNames.getOrDefault(card.getTaskList().getBoard().getId(), ""); // Lấy tên bảng
        }
        return ""; // Trả về rỗng
    }

    private void validateAccess(Long workspaceId, Long userId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId) // Kiểm tra quyền
                .orElseThrow(() -> new com.okabe.exception.UnauthorizedException("You are not a member of this workspace")); // Ném lỗi
    }

    private String escapeHtml(String s) {
        if (s == null) return ""; // Nếu null trả về rỗng
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") // Escape HTML
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private ActivityResponse toActivityResponse(Activity a) {
        return ActivityResponse.builder() // Xây dựng phản hồi
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
