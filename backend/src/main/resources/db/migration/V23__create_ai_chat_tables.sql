-- AI Conversations table
CREATE TABLE ai_conversations (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    board_id     BIGINT,
    workspace_id BIGINT,
    title        VARCHAR(255) NOT NULL DEFAULT 'New conversation',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_ai_conv_user (user_id, updated_at DESC)
);

-- AI Messages table
CREATE TABLE ai_messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role            ENUM('USER', 'ASSISTANT', 'SYSTEM') NOT NULL,
    content         TEXT NOT NULL,
    tokens_used     INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE,
    INDEX idx_ai_msg_conv (conversation_id, created_at ASC)
);
