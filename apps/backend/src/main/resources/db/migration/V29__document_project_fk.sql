-- document.project_id가 그동안 project를 FK로 참조하지 않아, 도메인 이벤트(DocumentProjectDiscardedEvent)를
-- 거치지 않는 삭제 경로(예: workspace 삭제 → project ON DELETE CASCADE)에서 고아 document가 남을 수 있었다.
-- project 삭제 시 document(그리고 cascade로 block·dependency_edge·edge_proposal)가 DB 레벨에서 함께
-- 정리되도록 FK + ON DELETE CASCADE를 건다.

-- 1) FK 추가 전, project가 사라진 기존 고아 document를 정리한다.
--    block은 block.document_id FK(ON DELETE CASCADE)로, edge·proposal은 document(project_id, id) FK로 함께 삭제된다.
DELETE FROM document d
WHERE NOT EXISTS (SELECT 1 FROM project p WHERE p.id = d.project_id);

-- 2) 이후 고아 발생을 DB 레벨에서 차단.
ALTER TABLE document
    ADD CONSTRAINT fk_document_project
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;
