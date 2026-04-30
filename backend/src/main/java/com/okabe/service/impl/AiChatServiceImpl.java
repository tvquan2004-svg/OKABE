package com.okabe.service.impl;

import com.okabe.dto.request.ChatRequest;
import com.okabe.dto.response.ChatResponse;
import com.okabe.dto.response.ConversationResponse;
import com.okabe.dto.response.MessageResponse;
import com.okabe.entity.AiConversation;
import com.okabe.entity.AiMessage;
import com.okabe.entity.AiMessage.MessageRole;
import com.okabe.entity.User;
import com.okabe.exception.ResourceNotFoundException;
import com.okabe.repository.AiConversationRepository;
import com.okabe.repository.AiMessageRepository;
import com.okabe.repository.UserRepository;
import com.okabe.security.UserPrincipal;
import com.okabe.service.AiChatService;
import com.okabe.service.AiContextBuilder;
import com.okabe.service.AiActionExecutor;
import com.okabe.service.GeminiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AiChatServiceImpl implements AiChatService {

    private static final int MAX_CONTEXT_MESSAGES = 20;
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Bạn là OKABE Assistant — AI hỗ trợ quản lý công việc thông minh cho hệ thống OKABE.
            
            Thông tin người dùng hiện tại:
            - Tên: %s
            
            %s
            
            Hướng dẫn QUAN TRỌNG:
            - Luôn trả lời bằng TIẾNG VIỆT, ngắn gọn và rõ ràng.
            - Không bịa đặt thông tin không có trong dữ liệu context.
            - Bạn CÓ THỂ thao tác dữ liệu (Tạo thẻ, Chuyển cột, Giao việc) bằng cách TRẢ VỀ CHUỖI JSON ĐẶC BIỆT ở cuối câu trả lời theo đúng định dạng sau (chỉ dùng khi người dùng yêu cầu):
            
            Để tạo card mới:
            [ACTION]
            {
              "type": "CREATE_CARD",
              "title": "Tên card cần tạo",
              "listName": "Tên cột (ví dụ: To-do, Doing)"
            }
            [/ACTION]
            
            Để di chuyển card:
            [ACTION]
            {
              "type": "MOVE_CARD",
              "cardTitle": "Tên card hiện tại",
              "targetList": "Tên cột đích"
            }
            [/ACTION]
            
            Để gán thành viên:
            [ACTION]
            {
              "type": "ASSIGN_MEMBER",
              "cardTitle": "Tên card",
              "memberName": "Tên username của thành viên"
            }
            [/ACTION]
            
            CHỈ tạo block [ACTION] khi bạn chắc chắn có đủ thông tin (tên card, tên cột đích). Nếu thiếu, hãy hỏi lại người dùng.
            """;

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GeminiProvider geminiProvider;
    private final AiContextBuilder contextBuilder;
    private final AiActionExecutor actionExecutor;

    @Override
    @Transactional
    public ConversationResponse createConversation(Long boardId, Long workspaceId, UserPrincipal currentUser) {
        User user = getUser(currentUser.getId());
        AiConversation conversation = AiConversation.builder()
                .user(user)
                .boardId(boardId)
                .workspaceId(workspaceId)
                .title("Cuộc trò chuyện mới")
                .build();
        AiConversation saved = conversationRepository.save(conversation);
        return mapToConversationResponse(saved, null);
    }

    @Override
    public List<ConversationResponse> getConversations(int page, int size, UserPrincipal currentUser) {
        return conversationRepository
                .findByUserIdOrderByUpdatedAtDesc(currentUser.getId(), PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(c -> mapToConversationResponse(c, null))
                .toList();
    }

    @Override
    public List<MessageResponse> getMessages(Long conversationId, UserPrincipal currentUser) {
        validateOwnership(conversationId, currentUser.getId());
        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .filter(m -> m.getRole() != MessageRole.SYSTEM)
                .map(this::mapToMessageResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request, UserPrincipal currentUser) {
        // 1. Get or create conversation
        AiConversation conversation = resolveConversation(request, currentUser);

        // 2. Save user message
        AiMessage userMessage = AiMessage.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(request.message())
                .build();
        messageRepository.saveAndFlush(userMessage);

        // 3. Build system prompt with context
        String contextData = contextBuilder.buildContext(
                currentUser.getId(), request.boardId(), request.workspaceId());
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE,
                currentUser.getUsername(), contextData);

        // 4. Build messages history for Gemini
        List<Map<String, String>> history = buildMessageHistory(conversation.getId());

        // 5. Call Gemini API
        String reply = geminiProvider.generateContent(systemPrompt, history);

        // 6. Save assistant message
        AiMessage assistantMessage = AiMessage.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(reply)
                .build();
        messageRepository.save(assistantMessage);

        updateConversationTitleIfNeeded(conversation, request.message());
        
        // 7. Process Actions if any
        actionExecutor.processActions(reply, request.boardId(), currentUser);

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(assistantMessage.getId())
                .reply(reply)
                .createdAt(assistantMessage.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public Long streamMessage(ChatRequest request, UserPrincipal currentUser,
                              java.util.function.Consumer<String> onToken) {
        // 1. Get or create conversation
        AiConversation conversation = resolveConversation(request, currentUser);

        // 2. Save user message
        AiMessage userMessage = AiMessage.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(request.message())
                .build();
        messageRepository.saveAndFlush(userMessage);

        // 3. Build system prompt with context
        String contextData = contextBuilder.buildContext(
                currentUser.getId(), request.boardId(), request.workspaceId());
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE,
                currentUser.getUsername(), contextData);

        // 4. Build messages history
        List<Map<String, String>> history = buildMessageHistory(conversation.getId());

        // 5. Stream from Groq API
        StringBuilder fullReply = new StringBuilder();
        try {
            geminiProvider.streamContent(systemPrompt, history,
                    token -> {
                        onToken.accept(token);
                        fullReply.append(token);
                    },
                    completeText -> {
                        // 6. Save complete reply after stream ends
                        AiMessage assistantMessage = AiMessage.builder()
                                .conversation(conversation)
                                .role(MessageRole.ASSISTANT)
                                .content(completeText)
                                .build();
                        messageRepository.save(assistantMessage);
                        updateConversationTitleIfNeeded(conversation, request.message());
                        
                        // 7. Process Actions if any
                        actionExecutor.processActions(completeText, request.boardId(), currentUser);
                    });
        } catch (Exception e) {
            log.error("Stream failed for user {}: {}", currentUser.getId(), e.getMessage());
            String errorMsg = "Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại! 🔧";
            onToken.accept(errorMsg);
        }

        return conversation.getId();
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, UserPrincipal currentUser) {
        validateOwnership(conversationId, currentUser.getId());
        conversationRepository.deleteById(conversationId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private AiConversation resolveConversation(ChatRequest request, UserPrincipal currentUser) {
        if (request.conversationId() != null) {
            validateOwnership(request.conversationId(), currentUser.getId());
            return conversationRepository.findById(request.conversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", request.conversationId()));
        }
        // Auto-create new conversation
        User user = getUser(currentUser.getId());
        AiConversation newConv = AiConversation.builder()
                .user(user)
                .boardId(request.boardId())
                .workspaceId(request.workspaceId())
                .build();
        return conversationRepository.save(newConv);
    }

    private List<Map<String, String>> buildMessageHistory(Long conversationId) {
        List<AiMessage> recent = messageRepository.findRecentMessages(conversationId, MAX_CONTEXT_MESSAGES);
        // Reverse to chronological order
        List<AiMessage> ordered = new ArrayList<>(recent);
        java.util.Collections.reverse(ordered);

        return ordered.stream()
                .filter(m -> m.getRole() != MessageRole.SYSTEM)
                .map(m -> Map.of("role", m.getRole().name(), "content", m.getContent()))
                .toList();
    }

    private void updateConversationTitleIfNeeded(AiConversation conversation, String firstMessage) {
        if ("Cuộc trò chuyện mới".equals(conversation.getTitle())) {
            String title = firstMessage.length() > 50
                    ? firstMessage.substring(0, 47) + "..."
                    : firstMessage;
            conversation.setTitle(title);
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
        } else {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
        }
    }

    private void validateOwnership(Long conversationId, Long userId) {
        if (!conversationRepository.existsByIdAndUserId(conversationId, userId)) {
            throw new ResourceNotFoundException("Conversation", conversationId);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private ConversationResponse mapToConversationResponse(AiConversation c, List<MessageResponse> messages) {
        return ConversationResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .boardId(c.getBoardId())
                .workspaceId(c.getWorkspaceId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .messages(messages)
                .build();
    }

    private MessageResponse mapToMessageResponse(AiMessage m) {
        return MessageResponse.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
