ALTER TABLE cards ADD COLUMN total_focus_minutes INT DEFAULT 0;

CREATE TABLE focus_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME,
    duration_minutes INT DEFAULT 25,
    completed BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
