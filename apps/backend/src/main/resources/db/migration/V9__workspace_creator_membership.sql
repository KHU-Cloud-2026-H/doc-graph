INSERT INTO workspace_member (workspace_id, user_id, joined_at)
SELECT id, created_by, created_at FROM workspace w
WHERE NOT EXISTS (
    SELECT 1 FROM workspace_member wm
    WHERE wm.workspace_id = w.id AND wm.user_id = w.created_by
);
