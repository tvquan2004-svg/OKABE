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
        User user = getUser(currentUser.getId()); // Lấy thông tin người dùng hiện tại
        AiConversation conversation = AiConversation.builder()
                .user(user) // Gán người dùng sở hữu cuộc trò chuyện
                .boardId(boardId) // Gán ID bảng liên quan
                .workspaceId(workspaceId) // Gán ID không gian làm việc
                .title("Cuộc trò chuyện mới") // Đặt tiêu đề mặc định
                .build(); // Xây dựng đối tượng AiConversation
        AiConversation saved = conversationRepository.save(conversation); // Lưu cuộc trò chuyện vào CSDL
        return mapToConversationResponse(saved, null); // Trả về phản hồi dạng ConversationResponse
    }

    @Override
    public List<ConversationResponse> getConversations(int page, int size, UserPrincipal currentUser) {
        return conversationRepository
                .findByUserIdOrderByUpdatedAtDesc(currentUser.getId(), PageRequest.of(page, size)) // Tìm cuộc trò chuyện theo user, phân trang
                .getContent() // Lấy nội dung trang
                .stream()
                .map(c -> mapToConversationResponse(c, null)) // Chuyển đổi sang ConversationResponse
                .toList(); // Thu thập thành danh sách
    }

    @Override
    public List<MessageResponse> getMessages(Long conversationId, UserPrincipal currentUser) {
        validateOwnership(conversationId, currentUser.getId()); // Kiểm tra quyền sở hữu cuộc trò chuyện
        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId) // Tìm tin nhắn theo conversation ID
                .stream()
                .filter(m -> m.getRole() != MessageRole.SYSTEM) // Lọc bỏ tin nhắn hệ thống
                .map(this::mapToMessageResponse) // Chuyển đổi sang MessageResponse
                .toList(); // Thu thập thành danh sách
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request, UserPrincipal currentUser) {
        // 1. Get or create conversation
        AiConversation conversation = resolveConversation(request, currentUser); // Lấy hoặc tạo mới cuộc trò chuyện

        // 2. Save user message
        AiMessage userMessage = AiMessage.builder()
                .conversation(conversation) // Gán cuộc trò chuyện cho tin nhắn
                .role(MessageRole.USER) // Đặt vai trò là người dùng
                .content(request.message()) // Gán nội dung tin nhắn từ request
                .build(); // Xây dựng đối tượng AiMessage
        messageRepository.saveAndFlush(userMessage); // Lưu và flush tin nhắn người dùng

        // 3. Build system prompt with context
        String contextData = contextBuilder.buildContext(
                currentUser.getId(), request.boardId(), request.workspaceId()); // Xây dựng ngữ cảnh cho AI
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE,
                currentUser.getUsername(), contextData); // Tạo system prompt với thông tin người dùng và ngữ cảnh

        // 4. Build messages history for Gemini
        List<Map<String, String>> history = buildMessageHistory(conversation.getId()); // Xây dựng lịch sử hội thoại

        // 5. Call Gemini API
        String reply = geminiProvider.generateContent(systemPrompt, history); // Gọi Gemini API để sinh phản hồi

        // 6. Save assistant message
        AiMessage assistantMessage = AiMessage.builder()
                .conversation(conversation) // Gán cuộc trò chuyện
                .role(MessageRole.ASSISTANT) // Đặt vai trò là trợ lý AI
                .content(reply) // Gán nội dung phản hồi từ AI
                .build(); // Xây dựng đối tượng AiMessage
        messageRepository.save(assistantMessage); // Lưu tin nhắn trợ lý vào CSDL

        updateConversationTitleIfNeeded(conversation, request.message()); // Cập nhật tiêu đề cuộc trò chuyện nếu cần
        
        // 7. Process Actions if any
        actionExecutor.processActions(reply, request.boardId(), currentUser); // Xử lý các hành động từ AI (tạo thẻ, di chuyển...)

        return ChatResponse.builder()
                .conversationId(conversation.getId()) // Gán ID cuộc trò chuyện
                .messageId(assistantMessage.getId()) // Gán ID tin nhắn trợ lý
                .reply(reply) // Gán nội dung phản hồi
                .createdAt(assistantMessage.getCreatedAt()) // Gán thời gian tạo
                .build(); // Xây dựng và trả về ChatResponse
    }

    @Override
    @Transactional
    public Long streamMessage(ChatRequest request, UserPrincipal currentUser,
                              java.util.function.Consumer<String> onToken) {
        // 1. Get or create conversation
        AiConversation conversation = resolveConversation(request, currentUser); // Lấy hoặc tạo mới cuộc trò chuyện

        // 2. Save user message
        AiMessage userMessage = AiMessage.builder()
                .conversation(conversation) // Gán cuộc trò chuyện
                .role(MessageRole.USER) // Đặt vai trò người dùng
                .content(request.message()) // Gán nội dung tin nhắn
                .build(); // Xây dựng đối tượng AiMessage
        messageRepository.saveAndFlush(userMessage); // Lưu và flush tin nhắn người dùng

        // 3. Build system prompt with context
        String contextData = contextBuilder.buildContext(
                currentUser.getId(), request.boardId(), request.workspaceId()); // Xây dựng ngữ cảnh
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE,
                currentUser.getUsername(), contextData); // Tạo system prompt

        // 4. Build messages history
        List<Map<String, String>> history = buildMessageHistory(conversation.getId()); // Xây dựng lịch sử hội thoại

        // 5. Stream from Groq API
        try {
            geminiProvider.streamContent(systemPrompt, history, // Gọi Gemini stream API
                    token -> onToken.accept(token), // Gửi từng token đến consumer
                    completeText -> {
                        // 6. Save complete reply after stream ends
                        AiMessage assistantMessage = AiMessage.builder()
                                .conversation(conversation) // Gán cuộc trò chuyện
                                .role(MessageRole.ASSISTANT) // Đặt vai trò trợ lý AI
                                .content(completeText) // Gán nội dung hoàn chỉnh
                                .build(); // Xây dựng đối tượng AiMessage
                        messageRepository.save(assistantMessage); // Lưu tin nhắn trợ lý vào CSDL
                        updateConversationTitleIfNeeded(conversation, request.message()); // Cập nhật tiêu đề nếu cần
                        
                        // 7. Process Actions if any
                        actionExecutor.processActions(completeText, request.boardId(), currentUser); // Xử lý hành động từ AI
                    });
        } catch (Exception e) {
            log.error("Stream failed for user {}: {}", currentUser.getId(), e.getMessage()); // Ghi log lỗi stream
            String errorMsg = "Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại! 🔧"; // Thông báo lỗi cho người dùng
            onToken.accept(errorMsg); // Gửi thông báo lỗi đến consumer
        }

        return conversation.getId(); // Trả về ID cuộc trò chuyện
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, UserPrincipal currentUser) {
        validateOwnership(conversationId, currentUser.getId()); // Kiểm tra quyền sở hữu
        conversationRepository.deleteById(conversationId); // Xóa cuộc trò chuyện theo ID
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private AiConversation resolveConversation(ChatRequest request, UserPrincipal currentUser) {
        if (request.conversationId() != null) { // Nếu có conversationId trong request
            validateOwnership(request.conversationId(), currentUser.getId()); // Kiểm tra quyền sở hữu
            return conversationRepository.findById(request.conversationId()) // Tìm cuộc trò chuyện theo ID
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", request.conversationId())); // Ném lỗi nếu không tìm thấy
        } // Nếu không có conversationId, tự động tạo mới
        User user = getUser(currentUser.getId()); // Lấy thông tin người dùng
        AiConversation newConv = AiConversation.builder()
                .user(user) // Gán người dùng
                .boardId(request.boardId()) // Gán ID bảng
                .workspaceId(request.workspaceId()) // Gán ID không gian làm việc
                .build(); // Xây dựng đối tượng AiConversation
        return conversationRepository.save(newConv); // Lưu và trả về cuộc trò chuyện mới
    }

    private List<Map<String, String>> buildMessageHistory(Long conversationId) {
        List<AiMessage> recent = messageRepository.findRecentMessages(conversationId, MAX_CONTEXT_MESSAGES); // Lấy tin nhắn gần đây
        // Reverse to chronological order
        List<AiMessage> ordered = new ArrayList<>(recent); // Sao chép danh sách
        java.util.Collections.reverse(ordered); // Đảo ngược thứ tự để sắp xếp theo thời gian

        return ordered.stream()
                .filter(m -> m.getRole() != MessageRole.SYSTEM) // Lọc bỏ tin nhắn hệ thống
                .map(m -> Map.of("role", m.getRole().name(), "content", m.getContent())) // Chuyển đổi thành Map role-content
                .toList(); // Thu thập thành danh sách
    }

    private void updateConversationTitleIfNeeded(AiConversation conversation, String firstMessage) {
        if ("Cuộc trò chuyện mới".equals(conversation.getTitle())) { // Nếu tiêu đề vẫn là mặc định
            String title = firstMessage.length() > 50 // Cắt ngắn nếu quá dài
                    ? firstMessage.substring(0, 47) + "..."
                    : firstMessage;
            conversation.setTitle(title); // Cập nhật tiêu đề mới
            conversation.setUpdatedAt(LocalDateTime.now()); // Cập nhật thời gian sửa đổi
            conversationRepository.save(conversation); // Lưu thay đổi
        } else { // Nếu tiêu đề đã được đặt
            conversation.setUpdatedAt(LocalDateTime.now()); // Chỉ cập nhật thời gian sửa đổi
            conversationRepository.save(conversation); // Lưu thay đổi
        }
    }

    private void validateOwnership(Long conversationId, Long userId) {
        if (!conversationRepository.existsByIdAndUserId(conversationId, userId)) { // Nếu không phải chủ sở hữu
            throw new ResourceNotFoundException("Conversation", conversationId); // Ném lỗi không tìm thấy
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId) // Tìm người dùng theo ID
                .orElseThrow(() -> new ResourceNotFoundException("User", userId)); // Ném lỗi nếu không tìm thấy
    }

    private ConversationResponse mapToConversationResponse(AiConversation c, List<MessageResponse> messages) {
        return ConversationResponse.builder()
                .id(c.getId()) // Gán ID cuộc trò chuyện
                .title(c.getTitle()) // Gán tiêu đề
                .boardId(c.getBoardId()) // Gán ID bảng
                .workspaceId(c.getWorkspaceId()) // Gán ID không gian làm việc
                .createdAt(c.getCreatedAt()) // Gán thời gian tạo
                .updatedAt(c.getUpdatedAt()) // Gán thời gian cập nhật
                .messages(messages) // Gán danh sách tin nhắn
                .build(); // Xây dựng ConversationResponse
    }

    private MessageResponse mapToMessageResponse(AiMessage m) {
        return MessageResponse.builder()
                .id(m.getId()) // Gán ID tin nhắn
                .role(m.getRole()) // Gán vai trò (user/assistant)
                .content(m.getContent()) // Gán nội dung tin nhắn
                .createdAt(m.getCreatedAt()) // Gán thời gian tạo
                .build(); // Xây dựng MessageResponse
    }
}
