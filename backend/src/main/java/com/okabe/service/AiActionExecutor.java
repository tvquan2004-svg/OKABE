package com.okabe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okabe.dto.request.CreateCardRequest;
import com.okabe.dto.request.MoveCardRequest;
import com.okabe.entity.Card;
import com.okabe.entity.TaskList;
import com.okabe.entity.User;
import com.okabe.repository.BoardRepository;
import com.okabe.repository.CardRepository;
import com.okabe.repository.TaskListRepository;
import com.okabe.repository.UserRepository;
import com.okabe.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiActionExecutor {

    private final CardRepository cardRepository;
    private final TaskListRepository listRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final CardService cardService;

    // Pattern to extract action JSON from AI response
    // Format expected: [ACTION]{ "type": "MOVE_CARD", ... }[/ACTION]
    private static final Pattern ACTION_PATTERN = Pattern.compile("\\[ACTION\\](.*?)\\[/ACTION\\]", Pattern.DOTALL);

    @Transactional
    public void processActions(String aiResponse, Long boardId, UserPrincipal currentUser) {
        if (boardId == null) {
            log.warn("Cannot execute AI actions without a board context");
            return;
        }

        Matcher matcher = ACTION_PATTERN.matcher(aiResponse);
        while (matcher.find()) {
            String jsonStr = matcher.group(1).trim();
            try {
                JsonNode actionNode = objectMapper.readTree(jsonStr);
                String type = actionNode.path("type").asText();
                log.info("Executing AI Action: {} on Board: {}", type, boardId);

                switch (type) {
                    case "CREATE_CARD" -> handleCreateCard(actionNode, boardId, currentUser);
                    case "MOVE_CARD" -> handleMoveCard(actionNode, boardId, currentUser);
                    case "ASSIGN_MEMBER" -> handleAssignMember(actionNode, boardId, currentUser);
                    default -> log.warn("Unknown AI action type: {}", type);
                }
            } catch (Exception e) {
                log.error("Failed to parse or execute AI action: {}", jsonStr, e);
            }
        }
    }

    private void handleCreateCard(JsonNode actionNode, Long boardId, UserPrincipal currentUser) {
        String title = actionNode.path("title").asText();
        String listName = actionNode.path("listName").asText();

        if (title.isBlank() || listName.isBlank()) return;

        TaskList list = listRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId).stream()
                .filter(l -> l.getName().equalsIgnoreCase(listName))
                .findFirst()
                .orElse(null);

        if (list == null) {
            log.warn("Target list '{}' not found for CREATE_CARD", listName);
            return;
        }

        CreateCardRequest request = new CreateCardRequest(title, null, null, null, null);
        cardService.createCard(list.getId(), request, currentUser);
        log.info("AI created card '{}' in list '{}'", title, listName);
    }

    private void handleMoveCard(JsonNode actionNode, Long boardId, UserPrincipal currentUser) {
        String cardTitle = actionNode.path("cardTitle").asText();
        String targetListName = actionNode.path("targetList").asText();

        if (cardTitle.isBlank() || targetListName.isBlank()) return;

        TaskList targetList = listRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId).stream()
                .filter(l -> l.getName().equalsIgnoreCase(targetListName))
                .findFirst()
                .orElse(null);

        if (targetList == null) return;

        // Find the card by title within the board
        Card card = findCardByTitleInBoard(cardTitle, boardId);
        if (card == null) return;

        MoveCardRequest request = new MoveCardRequest(targetList.getId(), null);
        cardService.moveCard(card.getId(), request, currentUser);
        log.info("AI moved card '{}' to list '{}'", cardTitle, targetListName);
    }

    private void handleAssignMember(JsonNode actionNode, Long boardId, UserPrincipal currentUser) {
        String cardTitle = actionNode.path("cardTitle").asText();
        String memberName = actionNode.path("memberName").asText();

        if (cardTitle.isBlank() || memberName.isBlank()) return;

        Card card = findCardByTitleInBoard(cardTitle, boardId);
        if (card == null) return;

        User targetUser = userRepository.findByUsername(memberName)
                .orElse(null);

        if (targetUser == null) {
            log.warn("User '{}' not found for ASSIGN_MEMBER", memberName);
            return;
        }

        // Check if already assigned
        boolean alreadyAssigned = card.getMembers().stream()
                .anyMatch(u -> u.getId().equals(targetUser.getId()));

        if (!alreadyAssigned) {
            cardService.assignMember(card.getId(), targetUser.getId(), currentUser);
            log.info("AI assigned member '{}' to card '{}'", memberName, cardTitle);
        }
    }

    private Card findCardByTitleInBoard(String cardTitle, Long boardId) {
        // Fetch all lists for the board
        java.util.List<TaskList> lists = listRepository.findByBoardIdAndIsArchivedFalseOrderByPositionAsc(boardId);
        for (TaskList list : lists) {
            java.util.List<Card> cards = cardRepository.findByTaskListIdAndIsArchivedFalseOrderByPositionAsc(list.getId());
            for (Card c : cards) {
                if (c.getTitle().equalsIgnoreCase(cardTitle) || c.getTitle().toLowerCase().contains(cardTitle.toLowerCase())) {
                    return c;
                }
            }
        }
        return null;
    }
}
