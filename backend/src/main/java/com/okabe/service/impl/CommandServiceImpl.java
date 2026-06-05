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
        if (command == null || command.isBlank()) {
            return CommandResponse.builder().type("error").message("Command cannot be empty").build();
        }

        String trimmed = command.trim();

        if (trimmed.startsWith("/tao")) return handleCreate(trimmed, currentUser);
        if (trimmed.startsWith("/di")) return handleNavigate(trimmed, currentUser);
        if (trimmed.startsWith("/move")) return handleMove(trimmed, currentUser);
        if (trimmed.startsWith("/search")) return handleSearch(trimmed, currentUser);

        return CommandResponse.builder()
                .type("error")
                .message("Unknown command. Available: /tao, /di, /move, /search")
                .build();
    }

    private CommandResponse handleCreate(String command, UserPrincipal currentUser) {
        Pattern p = Pattern.compile("/tao\\s+card\\s+\"(.+?)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(command);
        if (m.find()) {
            String title = m.group(1);
            return CommandResponse.builder()
                    .type("create_card")
                    .message("Ready to create card: " + title)
                    .data(Map.of("title", title, "action", "open_create_modal"))
                    .build();
        }
        return CommandResponse.builder().type("error").message("Usage: /tao card \"title\"").build();
    }

    private CommandResponse handleNavigate(String command, UserPrincipal currentUser) {
        Pattern p = Pattern.compile("/di\\s+board\\s+\"(.+?)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(command);
        if (m.find()) {
            String name = m.group(1).toLowerCase();
            List<Long> workspaceIds = workspaceMemberRepository.findByUserId(currentUser.getId()).stream()
                    .map(WorkspaceMember::getWorkspaceId).toList();
            if (workspaceIds.isEmpty()) {
                return CommandResponse.builder().type("error").message("No workspaces found").build();
            }
            var boards = boardRepository.findByWorkspaceIdIn(workspaceIds).stream()
                    .filter(b -> b.getName().toLowerCase().contains(name))
                    .toList();
            if (boards.isEmpty()) {
                return CommandResponse.builder().type("error").message("No board found matching: " + name).build();
            }
            var board = boards.get(0);
            return CommandResponse.builder()
                    .type("navigate")
                    .message("Navigating to board: " + board.getName())
                    .data(Map.of("url", "/board/" + board.getId()))
                    .build();
        }
        return CommandResponse.builder().type("error").message("Usage: /di board \"name\"").build();
    }

    private CommandResponse handleMove(String command, UserPrincipal currentUser) {
        Pattern p = Pattern.compile("/move\\s+card\\s+#?(\\d+)\\s+to\\s+\"(.+?)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(command);
        if (m.find()) {
            Long cardId = Long.parseLong(m.group(1));
            String targetListName = m.group(2);
            Optional<Card> optCard = cardRepository.findById(cardId);
            if (optCard.isEmpty()) {
                return CommandResponse.builder().type("error").message("Card #" + cardId + " not found").build();
            }
            Card card = optCard.get();
            List<TaskList> lists = taskListRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(
                    card.getTaskList().getBoard().getId());
            Optional<TaskList> target = lists.stream()
                    .filter(l -> l.getName().equalsIgnoreCase(targetListName))
                    .findFirst();
            if (target.isEmpty()) {
                return CommandResponse.builder().type("error")
                        .message("List \"" + targetListName + "\" not found on this board").build();
            }
            card.setTaskList(target.get());
            cardRepository.save(card);
            return CommandResponse.builder()
                    .type("move_card")
                    .message("Moved card #" + cardId + " to \"" + targetListName + "\"")
                    .data(Map.of("cardId", cardId, "listName", targetListName))
                    .build();
        }
        return CommandResponse.builder().type("error").message("Usage: /move card #id to \"list name\"").build();
    }

    private CommandResponse handleSearch(String command, UserPrincipal currentUser) {
        Pattern p = Pattern.compile("/search\\s+\"(.+?)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(command);
        if (m.find()) {
            String keyword = m.group(1);
            return CommandResponse.builder()
                    .type("search")
                    .message("Searching for: " + keyword)
                    .data(Map.of("keyword", keyword))
                    .build();
        }
        return CommandResponse.builder().type("error").message("Usage: /search \"keyword\"").build();
    }
}
