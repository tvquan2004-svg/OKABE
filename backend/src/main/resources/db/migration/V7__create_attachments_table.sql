CREATE TABLE attachments (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id      BIGINT NOT NULL,
    uploaded_by  BIGINT NOT NULL,
    filename     VARCHAR(255) NOT NULL,
    storage_key  VARCHAR(500) NOT NULL,
    file_size    BIGINT,
    mime_type    VARCHAR(100),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id)     REFERENCES cards(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);
