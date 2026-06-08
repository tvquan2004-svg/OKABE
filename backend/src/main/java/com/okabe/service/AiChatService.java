package com.okabe.service;

import com.okabe.dto.request.ChatRequest;
import com.okabe.dto.response.ChatResponse;
import com.okabe.dto.response.ConversationResponse;
import com.okabe.dto.response.MessageResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface AiChatService {

    // Tạo cuộc hội thoại mới cho user
    ConversationResponse createConversation(Long boardId, Long workspaceId, UserPrincipal currentUser);

    // Lấy danh sách cuộc hội thoại của user (có phân trang)
    List<ConversationResponse> getConversations(int page, int size, UserPrincipal currentUser);

    // Lấy tất cả tin nhắn trong cuộc hội thoại (user phải là chủ sở hữu)
    List<MessageResponse> getMessages(Long conversationId, UserPrincipal currentUser);

    // Gửi tin nhắn và nhận phản hồi từ AI
    ChatResponse sendMessage(ChatRequest request, UserPrincipal currentUser);

    // Stream phản hồi AI qua SSE, gọi onToken cho mỗi chunk, trả về conversationId
    Long streamMessage(ChatRequest request, UserPrincipal currentUser,
                       java.util.function.Consumer<String> onToken);

    // Xoá cuộc hội thoại và tất cả tin nhắn trong đó
    void deleteConversation(Long conversationId, UserPrincipal currentUser);
}
