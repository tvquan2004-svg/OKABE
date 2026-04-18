ALTER TABLE boards
ADD COLUMN position INT NOT NULL DEFAULT 0 AFTER description;

SET @current_workspace_id := NULL;
SET @current_position := -1;

UPDATE boards
SET position = (
    CASE
        WHEN @current_workspace_id = workspace_id THEN @current_position := @current_position + 1
        ELSE
            CASE
                WHEN (@current_workspace_id := workspace_id) IS NOT NULL
                THEN @current_position := 0
                ELSE 0
            END
    END
)
ORDER BY workspace_id, created_at, id;

CREATE INDEX idx_boards_workspace_position ON boards(workspace_id, position);
