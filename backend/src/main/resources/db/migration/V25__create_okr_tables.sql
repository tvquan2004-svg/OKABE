CREATE TABLE objectives (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    quarter VARCHAR(20) NOT NULL,
    progress DECIMAL(5,2) DEFAULT 0.00,
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE key_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    objective_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    target_value DECIMAL(10,2),
    current_value DECIMAL(10,2) DEFAULT 0,
    unit VARCHAR(50) DEFAULT 'percent',
    linked_cards TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (objective_id) REFERENCES objectives(id) ON DELETE CASCADE
);
