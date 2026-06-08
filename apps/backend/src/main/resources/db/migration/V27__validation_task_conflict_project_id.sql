-- validation_task·conflict에 project_id 비정규화 (discard cascade를 순서 무관하게 만들기 위함).
-- conflict_finding은 conflict·validation_task 양쪽 ON DELETE CASCADE라 별도 정리 불필요.

-- 1) 기존 고아 정리 (DELETE는 재실행 안전) — backfill 전에 edge 없는 row를 제거해 project_id NULL 잔존을 막는다.
DELETE FROM validation_task vt
 WHERE NOT EXISTS (SELECT 1 FROM dependency_edge e WHERE e.id = vt.edge_id);
DELETE FROM conflict c
 WHERE NOT EXISTS (SELECT 1 FROM dependency_edge e WHERE e.id = c.edge_id);
DELETE FROM ai_usage_record a
 WHERE NOT EXISTS (SELECT 1 FROM project p WHERE p.id = a.project_id);

-- 2) project_id 컬럼 추가 (nullable — enqueue fail-open(edge=null race) 경로 허용)
ALTER TABLE validation_task ADD COLUMN project_id BIGINT;
ALTER TABLE conflict        ADD COLUMN project_id BIGINT;

-- 3) backfill: edge → project
UPDATE validation_task vt
   SET project_id = (SELECT e.project_id FROM dependency_edge e WHERE e.id = vt.edge_id);
UPDATE conflict c
   SET project_id = (SELECT e.project_id FROM dependency_edge e WHERE e.id = c.edge_id);

-- 4) discard cascade 조회용 인덱스
CREATE INDEX idx_validation_task_project_id ON validation_task (project_id);
CREATE INDEX idx_conflict_project_id ON conflict (project_id);
