CREATE TABLE notification_preferences (
    user_id           BIGINT NOT NULL,
    email_assigned    BOOLEAN DEFAULT TRUE,
    email_mentioned   BOOLEAN DEFAULT TRUE,
    email_due_soon    BOOLEAN DEFAULT TRUE,
    email_invited     BOOLEAN DEFAULT TRUE,
    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_notif_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
