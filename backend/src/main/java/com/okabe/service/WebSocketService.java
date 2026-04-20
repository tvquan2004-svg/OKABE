package com.okabe.service;

import com.okabe.dto.response.WebSocketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastToBoard(Long boardId, String type, Object payload, Long actorId) {
        WebSocketEvent event = WebSocketEvent.builder()
                .type(type)
                .boardId(boardId)
                .payload(payload)
                .actorId(actorId)
                .build();
        
        String destination = "/topic/board." + boardId;
        
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.debug("Broadcasting {} to {} after commit", type, destination);
                    messagingTemplate.convertAndSend(destination, event);
                }
            });
        } else {
            log.debug("Broadcasting {} to {} (no active transaction)", type, destination);
            messagingTemplate.convertAndSend(destination, event);
        }
    }

    public void sendToUser(Long userId, String type, Object payload) {
        WebSocketEvent event = WebSocketEvent.builder()
                .type(type)
                .payload(payload)
                .build();
        
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.debug("Sending {} to user {} after commit", type, userId);
                    messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", event);
                }
            });
        } else {
            log.debug("Sending {} to user {} (no active transaction)", type, userId);
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notifications", event);
        }
    }

    public void sendToTopic(String destination, String type, Object payload) {
        WebSocketEvent event = WebSocketEvent.builder()
                .type(type)
                .payload(payload)
                .build();
        
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.debug("Sending {} to topic {} after commit", type, destination);
                    messagingTemplate.convertAndSend(destination, event);
                }
            });
        } else {
            log.debug("Sending {} to topic {} (no active transaction)", type, destination);
            messagingTemplate.convertAndSend(destination, event);
        }
    }
}
