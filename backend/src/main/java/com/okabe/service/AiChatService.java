package com.okabe.service;

import com.okabe.dto.request.ChatRequest;
import com.okabe.dto.response.ChatResponse;
import com.okabe.dto.response.ConversationResponse;
import com.okabe.dto.response.MessageResponse;
import com.okabe.security.UserPrincipal;

import java.util.List;

public interface AiChatService {

    /**
     * Creates a new conversation for the user.
     */
    ConversationResponse createConversation(Long boardId, Long workspaceId, UserPrincipal currentUser);

    /**
     * Returns paginated list of conversations for the current user.
     */
    List<ConversationResponse> getConversations(int page, int size, UserPrincipal currentUser);

    /**
     * Returns all messages in a conversation (user must own it).
     */
    List<MessageResponse> getMessages(Long conversationId, UserPrincipal currentUser);

    /**
     * Sends a message and returns the AI reply.
     */
    ChatResponse sendMessage(ChatRequest request, UserPrincipal currentUser);

    /**
     * Streams the AI reply using SSE — calls onToken for each chunk.
     * Returns the conversation ID (created if new).
     */
    Long streamMessage(ChatRequest request, UserPrincipal currentUser,
                       java.util.function.Consumer<String> onToken);

    /**
     * Deletes a conversation and all its messages.
     */
    void deleteConversation(Long conversationId, UserPrincipal currentUser);
}
