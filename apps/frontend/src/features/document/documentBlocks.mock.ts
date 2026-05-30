import type { components } from "@docgraph/api-types";

type Block = components["schemas"]["Block"];

/**
 * BlockRenderer 시연용 mock 데이터.
 * 평평한 리스트(절대 미리 중첩하지 않음) — 렌더러가 parentBlockId + order 로 트리를 재구성한다.
 * 문서별 실제 blocks 연결은 이후 API 훅 연동 단계에서 진행한다.
 */
export const MOCK_DOCUMENT_BLOCKS: Block[] = [
  {
    blockId: "b2",
    parentBlockId: null,
    type: "paragraph",
    text: "본 문서는 WorkSync 근태관리 시스템의 제품 요구사항(Product Requirements)을 정의한다. 출퇴근 기록, 지각·조퇴 산정, 근태 리포트 자동화를 핵심 범위로 한다.",
    order: 1,
  },
  {
    blockId: "b3",
    parentBlockId: null,
    type: "heading_2",
    text: "1. 개요",
    order: 2,
  },
  {
    blockId: "b4",
    parentBlockId: null,
    type: "paragraph",
    text: "WorkSync는 사내 구성원의 출퇴근을 자동으로 집계하여 인사·급여 시스템과 연동되는 근태관리 솔루션이다. 수기 기록으로 인한 오류를 줄이고, 관리자에게 실시간 근태 현황을 제공하는 것을 목표로 한다.",
    order: 3,
  },
  {
    blockId: "b5",
    parentBlockId: null,
    type: "quote",
    text: "출근은 오전 9시, 퇴근은 오후 6시 — 표준 근무 정책을 기준으로 모든 산정 로직이 동작한다.",
    order: 4,
  },
  {
    blockId: "b6",
    parentBlockId: null,
    type: "heading_3",
    text: "1.1 목표",
    order: 5,
  },
  {
    blockId: "b7",
    parentBlockId: null,
    type: "bulleted_list_item",
    text: "출퇴근 기록을 자동으로 수집·저장한다.",
    order: 6,
  },
  {
    blockId: "b8",
    parentBlockId: null,
    type: "bulleted_list_item",
    text: "지각·조퇴 상태를 정책에 따라 자동 산정한다.",
    order: 7,
  },
  {
    blockId: "b9",
    parentBlockId: "b8",
    type: "bulleted_list_item",
    text: "오전 9시 11분 00초부터 지각(LATE)으로 처리한다.",
    order: 0,
  },
  {
    blockId: "b11",
    parentBlockId: null,
    type: "heading_3",
    text: "1.2 핵심 기능 우선순위",
    order: 8,
  },
  {
    blockId: "b12",
    parentBlockId: null,
    type: "numbered_list_item",
    text: "출퇴근 체크인/체크아웃",
    order: 9,
  },
  {
    blockId: "b13",
    parentBlockId: null,
    type: "numbered_list_item",
    text: "실시간 근태 현황 대시보드",
    order: 10,
  },
  {
    blockId: "b14",
    parentBlockId: null,
    type: "numbered_list_item",
    text: "휴가 신청 및 결재 워크플로",
    order: 11,
  },
  {
    blockId: "b15",
    parentBlockId: null,
    type: "heading_4",
    text: "1.2.1 비고",
    order: 12,
  },
  {
    blockId: "b16",
    parentBlockId: null,
    type: "paragraph",
    text: "우선순위는 매 분기 회고에서 재검토하며, 변경 시 본 문서를 갱신한다.",
    order: 13,
  },
  {
    blockId: "b17",
    parentBlockId: null,
    type: "divider",
    text: null,
    order: 14,
  },
  {
    blockId: "b18",
    parentBlockId: null,
    type: "heading_2",
    text: "2. 관련 페이지",
    order: 15,
  },
  {
    blockId: "b19",
    parentBlockId: null,
    type: "child_page",
    text: "API 명세서",
    order: 16,
  },
  {
    blockId: "b20",
    parentBlockId: null,
    type: "child_page",
    text: "데이터베이스 스키마 설계",
    order: 17,
  },
  {
    blockId: "b21",
    parentBlockId: null,
    type: "child_database",
    text: "이슈 트래커",
    order: 18,
  },
  {
    blockId: "b22",
    parentBlockId: null,
    type: "image",
    text: null,
    order: 19,
  },
  {
    blockId: "b23",
    parentBlockId: null,
    type: "table",
    text: null,
    order: 20,
  },
];
