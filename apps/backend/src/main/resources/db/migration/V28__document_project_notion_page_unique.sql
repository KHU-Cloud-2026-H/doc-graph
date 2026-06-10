-- document는 (project_id, notion_page_id) 단위로 유일해야 하나 그동안 제약이 없어
-- 동시 sync 경합·고아 누적으로 같은 프로젝트 안에 중복 행이 생길 수 있었다.
-- 유니크 인덱스를 추가하기 전에 기존 중복을 정리한다. 그룹별 최신(MAX id)을 winner로 남긴다.

-- 1) loser를 부모로 가리키던 자식 문서의 parent_document_id를 winner로 재지정.
--    document.parent_document_id에는 cascade가 없어 먼저 옮기지 않으면 삭제 시 FK 위반.
WITH winners AS (
    SELECT project_id, notion_page_id, MAX(id) AS winner_id
    FROM document
    GROUP BY project_id, notion_page_id
    HAVING COUNT(*) > 1
),
losers AS (
    SELECT d.id AS loser_id, w.winner_id
    FROM document d
    JOIN winners w
        ON w.project_id = d.project_id
       AND w.notion_page_id = d.notion_page_id
    WHERE d.id <> w.winner_id
)
UPDATE document c
SET parent_document_id = l.winner_id
FROM losers l
WHERE c.parent_document_id = l.loser_id;

-- 2) loser 문서 삭제. block·dependency_edge·edge_proposal은 ON DELETE CASCADE로 함께 정리되며,
--    엣지/제안은 다음 sync에서 재생성된다.
WITH winners AS (
    SELECT project_id, notion_page_id, MAX(id) AS winner_id
    FROM document
    GROUP BY project_id, notion_page_id
    HAVING COUNT(*) > 1
)
DELETE FROM document d
USING winners w
WHERE w.project_id = d.project_id
  AND w.notion_page_id = d.notion_page_id
  AND d.id <> w.winner_id;

-- 3) 이후 중복을 DB 레벨에서 차단.
CREATE UNIQUE INDEX uk_document_project_notion_page ON document (project_id, notion_page_id);
