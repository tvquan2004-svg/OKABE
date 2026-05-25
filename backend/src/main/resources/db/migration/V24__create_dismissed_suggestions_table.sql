CREATE TABLE dismissed_suggestions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    type         VARCHAR(50) NOT NULL,
    card_id      BIGINT,
    dismissed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_workspace_type_card (user_id, workspace_id, type, card_id),
    INDEX idx_user_workspace (user_id, workspace_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
