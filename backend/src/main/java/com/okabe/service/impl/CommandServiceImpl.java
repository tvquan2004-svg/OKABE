package com.okabe.service.impl;

import com.okabe.dto.response.CommandResponse;
import com.okabe.entity.Card;
import com.okabe.entity.TaskList;
import com.okabe.entity.WorkspaceMember;
import com.okabe.repository.*;
import com.okabe.security.UserPrincipal;
import com.okabe.service.CommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommandServiceImpl implements CommandService {

    private final CardRepository cardRepository;
    private final TaskListRepository taskListRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional
    public CommandResponse execute(String command, UserPrincipal currentUser) {
        if (command == null || command.isBlank()) { // Nếu lệnh trống
            return CommandResponse.builder().type("error").message("Command cannot be empty").build(); // Trả về lỗi
        }

        String trimmed = command.trim(); // Loại bỏ khoảng trắng

        if (trimmed.startsWith("/tao")) return handleCreate(trimmed, currentUser); // Xử lý lệnh tạo
        if (trimmed.startsWith("/di")) return handleNavigate(trimmed, currentUser); // Xử lý lệnh điều hướng
        if (trimmed.startsWith("/move")) return handleMove(trimmed, currentUser); // Xử lý lệnh di chuyển
        if (trimmed.startsWith("/search")) return handleSearch(trimmed, currentUser); // Xử lý lệnh tìm kiếm

        return CommandResponse.builder() // Nếu lệnh không xác định
                .type("error") // Đặt loại là lỗi
                .message("Unknown command. Available: /tao, /di, /move, /search") // Thông báo lệnh khả dụng
                .build(); // Xây dựng CommandResponse
    }

    private CommandResponse handleCreate(String command, UserPrincipal currentUser) {
        Pattern p = Pattern.compile("/tao\\s+card\\s+\"(.+?)\"", Pattern.CASE_INSENSITIVE); // Định nghĩa pattern cho lệnh tạo card
        Matcher m = p.matcher(command); // So khớp pattern
        if (m.find()) { // Nếu tìm thấy
            String title = m.group(1); // Lấy tiêu đề card
            return CommandResponse.builder()
                    .type("create_card") // Đặt loại là tạo card
                    .message("Ready to create card: " + title) // Thông báo sẵn sàng tạo
                    .data(Map.of("title", title, "action", "open_create_modal")) // Dữ liệu kèm theo
                    .build(); // Xây dựng CommandResponse
        }
        return CommandResponse.builder().type("error").message("Usage: /tao card \"title\"").build(); // Hướng dẫn cú pháp
    }

    private CommandResponse handleNavigate(String command, UserPrincipal currentUser) {
        Pattern p = Pattern.compile("/di\\s+board\\s+\"(.+?)\"", Pattern.CASE_INSENSITIVE); // Pattern cho lệnh điều hướng
        Matcher m = p.matcher(command); // So khớp pattern
        if (m.find()) { // Nếu tìm thấy
            String name = m.group(1).toLowerCase(); // Lấy tên bảng
            List<Long> workspaceIds = workspaceMemberRepository.findByUserId(currentUser.getId()).stream() // Lấy danh sách workspace ID
                    .map(WorkspaceMember::getWorkspaceId).toList(); // Ánh xạ thành danh sách ID
            if (workspaceIds.isEmpty()) { // Nếu không có workspace
                return CommandResponse.builder().type("error").message("No workspaces found").build(); // Trả về lỗi
            }
            var boards = boardRepository.findByWorkspaceIdIn(workspaceIds).stream() // Tìm bảng trong các workspace
                    .filter(b -> b.getName().toLowerCase().contains(name)) // Lọc theo tên
                    .toList(); // Thu thập thành danh sách
            if (boards.isEmpty()) { // Nếu không tìm thấy bảng
                return CommandResponse.builder().type("error").message("No board found matching: " + name).build(); // Trả về lỗi
            }
            var board = boards.get(0); // Lấy bảng đầu tiên
            return CommandResponse.builder()
                    .type("navigate") // Đặt loại điều hướng
                    .message("Navigating to board: " + board.getName()) // Thông báo
                    .data(Map.of("url", "/board/" + board.getId())) // Dữ liệu URL
                    .build(); // Xây dựng CommandResponse
        }
        return CommandResponse.builder().type("error").message("Usage: /di board \"name\"").build(); // Hướng dẫn cú pháp
    }

    private CommandResponse handleMove(String command, UserPrincipal currentUser) {
        Pattern p = Pattern.compile("/move\\s+card\\s+#?(\\d+)\\s+to\\s+\"(.+?)\"", Pattern.CASE_INSENSITIVE); // Pattern di chuyển card
        Matcher m = p.matcher(command); // So khớp pattern
        if (m.find()) { // Nếu tìm thấy
            Long cardId = Long.parseLong(m.group(1)); // Lấy ID card
            String targetListName = m.group(2); // Lấy tên danh sách đích
            Optional<Card> optCard = cardRepository.findById(cardId); // Tìm card theo ID
            if (optCard.isEmpty()) { // Nếu không tìm thấy
                return CommandResponse.builder().type("error").message("Card #" + cardId + " not found").build(); // Trả về lỗi
            }
            Card card = optCard.get(); // Lấy đối tượng card
            List<TaskList> lists = taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(
                    card.getTaskList().getBoard().getId()); // Lấy danh sách các cột
            Optional<TaskList> target = lists.stream() // Tìm cột đích
                    .filter(l -> l.getName().equalsIgnoreCase(targetListName)) // Lọc theo tên
                    .findFirst(); // Lấy cột đầu tiên khớp
            if (target.isEmpty()) { // Nếu không tìm thấy cột
                return CommandResponse.builder().type("error")
                        .message("List \"" + targetListName + "\" not found on this board").build(); // Trả về lỗi
            }
            card.setTaskList(target.get()); // Gán cột mới cho card
            cardRepository.save(card); // Lưu card
            return CommandResponse.builder()
                    .type("move_card") // Đặt loại di chuyển
                    .message("Moved card #" + cardId + " to \"" + targetListName + "\"") // Thông báo
                    .data(Map.of("cardId", cardId, "listName", targetListName)) // Dữ liệu
                    .build(); // Xây dựng CommandResponse
        }
        return CommandResponse.builder().type("error").message("Usage: /move card #id to \"list name\"").build(); // Hướng dẫn cú pháp
    }

    private CommandResponse handleSearch(String command, UserPrincipal currentUser) {
        Pattern p = Pattern.compile("/search\\s+\"(.+?)\"", Pattern.CASE_INSENSITIVE); // Pattern tìm kiếm
        Matcher m = p.matcher(command); // So khớp pattern
        if (m.find()) { // Nếu tìm thấy
            String keyword = m.group(1); // Lấy từ khóa
            return CommandResponse.builder()
                    .type("search") // Đặt loại tìm kiếm
                    .message("Searching for: " + keyword) // Thông báo
                    .data(Map.of("keyword", keyword)) // Dữ liệu từ khóa
                    .build(); // Xây dựng CommandResponse
        }
        return CommandResponse.builder().type("error").message("Usage: /search \"keyword\"").build(); // Hướng dẫn cú pháp
    }
}
