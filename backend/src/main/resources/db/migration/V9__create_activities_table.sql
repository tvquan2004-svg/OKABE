CREATE TABLE activities (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id      BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    action_type  VARCHAR(50) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
