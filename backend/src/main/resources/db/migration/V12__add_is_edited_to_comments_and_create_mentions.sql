-- =============================================
-- V12__add_is_edited_to_comments_and_create_mentions.sql
-- OKABE Task Manager — Card Comments & Mentions
-- =============================================

ALTER TABLE comments ADD COLUMN is_edited BOOLEAN DEFAULT FALSE NOT NULL;

CREATE TABLE comment_mentions (
    comment_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    PRIMARY KEY (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_comment_mentions_user ON comment_mentions(user_id);
