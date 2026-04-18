-- =============================================
-- V1__init_schema.sql
-- OKABE Task Manager — Initial Database Schema
-- =============================================

-- Users
CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) UNIQUE NOT NULL,
    username    VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    avatar_url  VARCHAR(500),
    is_active   BOOLEAN DEFAULT TRUE NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Workspaces
CREATE TABLE workspaces (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    owner_id    BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Workspace Members
CREATE TABLE workspace_members (
    workspace_id BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    role         ENUM('OWNER','ADMIN','MEMBER','VIEWER') DEFAULT 'MEMBER' NOT NULL,
    joined_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, user_id),
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Boards
CREATE TABLE boards (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    background   VARCHAR(255),
    is_starred   BOOLEAN DEFAULT FALSE NOT NULL,
    is_archived  BOOLEAN DEFAULT FALSE NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

-- Lists (Columns on a Board)
CREATE TABLE lists (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id    BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    position    INT NOT NULL,
    is_archived BOOLEAN DEFAULT FALSE NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE
);

-- Cards (Tasks)
CREATE TABLE cards (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    list_id     BIGINT NOT NULL,
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    position    INT NOT NULL,
    due_date    DATETIME,
    priority    ENUM('LOW','MEDIUM','HIGH','CRITICAL') DEFAULT 'MEDIUM' NOT NULL,
    is_archived BOOLEAN DEFAULT FALSE NOT NULL,
    created_by  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Card Members (Assignments)
CREATE TABLE card_members (
    card_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (card_id, user_id),
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Comments
CREATE TABLE comments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id    BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    content    TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Labels
CREATE TABLE labels (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id BIGINT NOT NULL,
    name     VARCHAR(100),
    color    VARCHAR(20) NOT NULL,
    FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE
);

-- Card Labels
CREATE TABLE card_labels (
    card_id  BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    PRIMARY KEY (card_id, label_id),
    FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    FOREIGN KEY (label_id) REFERENCES labels(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_workspaces_owner ON workspaces(owner_id);
CREATE INDEX idx_workspaces_slug ON workspaces(slug);
CREATE INDEX idx_boards_workspace ON boards(workspace_id);
CREATE INDEX idx_lists_board ON lists(board_id);
CREATE INDEX idx_cards_list ON cards(list_id);
CREATE INDEX idx_cards_created_by ON cards(created_by);
CREATE INDEX idx_comments_card ON comments(card_id);
CREATE INDEX idx_labels_board ON labels(board_id);
