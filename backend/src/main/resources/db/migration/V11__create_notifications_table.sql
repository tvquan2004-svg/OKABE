-- Notifications Table
CREATE TABLE notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    actor_id     BIGINT,
    type         VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(50) NOT NULL,
    entity_id    BIGINT NOT NULL,
    message      VARCHAR(500) NOT NULL,
    is_read      BOOLEAN DEFAULT FALSE NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_recipient_unread (recipient_id, is_read),
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Add notification_sent flag to cards to prevent duplicate due-date notifications
ALTER TABLE cards ADD COLUMN notification_sent BOOLEAN DEFAULT FALSE NOT NULL;
