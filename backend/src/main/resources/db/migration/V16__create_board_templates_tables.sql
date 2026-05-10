CREATE TABLE board_templates (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_system   BOOLEAN DEFAULT FALSE,
    created_by  BIGINT,
    workspace_id BIGINT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_template_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
);

CREATE TABLE template_lists (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    position    INT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_list_template FOREIGN KEY (template_id) REFERENCES board_templates(id) ON DELETE CASCADE
);

CREATE TABLE template_cards (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_list_id BIGINT NOT NULL,
    title        VARCHAR(500) NOT NULL,
    description  TEXT,
    position     INT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_card_list FOREIGN KEY (template_list_id) REFERENCES template_lists(id) ON DELETE CASCADE
);
