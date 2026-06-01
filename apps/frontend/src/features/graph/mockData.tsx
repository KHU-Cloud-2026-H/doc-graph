import type { ReactNode } from 'react';
import type { Node, Edge } from '@xyflow/react';
import { FileText, Database, Share2, BarChart2, Folder } from 'lucide-react';

export type DocumentNodeData = {
  label: string;
  id?: string;
  hasError?: boolean;
  icon: ReactNode;
};

export type DocumentFlowNode = Node<DocumentNodeData, 'document'>;

export type ConflictEdgeData = {
  onClick?: () => void;
  conflictStatus?: 'NONE' | 'CONFLICT';
  source?: 'NOTION_REFERENCE' | 'PROPOSAL_ACCEPTED' | 'CUSTOM';
  validationCriterion?: string;
};
export type ConflictFlowEdge = Edge<ConflictEdgeData, 'conflict'>;
export type AppEdge = Edge<ConflictEdgeData, 'straight' | 'conflict'>;

export const initialNodes: DocumentFlowNode[] = [
  { id: '1', type: 'document', position: { x: 150, y: 250 }, data: { label: 'Product Planning', icon: <span className="text-xl">🚀</span> } },
  { id: '2', type: 'document', position: { x: 300, y: 150 }, data: { label: '[PRD] v1.0 MVP 요구사항', icon: <FileText className="text-slate-500 w-6 h-6" /> } },
  { id: '3', type: 'document', position: { x: 300, y: 350 }, data: { id: 'prd', label: '[PRD] v2.0 결제 시스템 요구사항', hasError: true, icon: <FileText className="text-red-600 w-6 h-6" /> } },
  { id: '4', type: 'document', position: { x: 480, y: 250 }, data: { label: 'UI/UX 디자인 시안', icon: <span className="text-xl">🎨</span> } },
  { id: '5', type: 'document', position: { x: 480, y: 350 }, data: { label: 'v2.0 UT 결과 보고서', icon: <BarChart2 className="text-slate-500 w-6 h-6" /> } },
  { id: '6', type: 'document', position: { x: 200, y: 600 }, data: { label: 'Engineering', icon: <span className="text-xl">💻</span> } },
  { id: '7', type: 'document', position: { x: 430, y: 500 }, data: { label: '[API Spec] 결제 연동 API', icon: <Share2 className="text-slate-500 w-6 h-6" /> } },
  { id: '8', type: 'document', position: { x: 430, y: 600 }, data: { label: '[DB] 정산 시스템 설계서', icon: <Database className="text-slate-500 w-6 h-6" /> } },
  { id: '9', type: 'document', position: { x: 430, y: 700 }, data: { label: '[FE] 결제 위젯 가이드', icon: <span className="text-sm font-bold text-slate-500">JS</span> } },
  { id: '10', type: 'document', position: { x: 650, y: 200 }, data: { label: 'Meeting Notes', icon: <span className="text-xl">🗓️</span> } },
  { id: '11', type: 'document', position: { x: 830, y: 300 }, data: { label: '2026년 4월', icon: <Folder className="text-amber-500 fill-amber-500 w-6 h-6" /> } },
  { id: '12', type: 'document', position: { x: 1000, y: 250 }, data: { label: '[주간회의록] 4월 3주차', icon: <FileText className="text-slate-500 w-6 h-6" /> } },
  { id: '13', type: 'document', position: { x: 1000, y: 400 }, data: { id: 'meet-urgent', label: '[주간회의록] 4월 4주차 백엔드 싱크', hasError: true, icon: <FileText className="text-red-600 w-6 h-6" /> } },
];

export const initialEdges: AppEdge[] = [
  { id: 'e1-2', source: '1', target: '2', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'NOTION_REFERENCE', validationCriterion: '제품 기획 문서는 PRD를 참조해야 합니다.' } },
  { id: 'e1-3', source: '1', target: '3', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'NOTION_REFERENCE', validationCriterion: '제품 기획 문서는 PRD를 참조해야 합니다.' } },
  { id: 'e3-4', source: '3', target: '4', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'NOTION_REFERENCE', validationCriterion: 'PRD는 UI/UX 디자인 시안과 일치해야 합니다.' } },
  { id: 'e3-5', source: '3', target: '5', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'CUSTOM', validationCriterion: 'PRD 요구사항은 UT 결과와 대조되어야 합니다.' } },
  { id: 'e6-7', source: '6', target: '7', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'NOTION_REFERENCE', validationCriterion: '엔지니어링 문서는 API 명세를 참조해야 합니다.' } },
  { id: 'e6-8', source: '6', target: '8', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'NOTION_REFERENCE', validationCriterion: '엔지니어링 문서는 DB 설계서와 일치해야 합니다.' } },
  { id: 'e6-9', source: '6', target: '9', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'CUSTOM', validationCriterion: '엔지니어링 문서는 FE 가이드를 참조해야 합니다.' } },
  { id: 'e10-11', source: '10', target: '11', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'NOTION_REFERENCE', validationCriterion: '회의록은 월별 폴더로 분류되어야 합니다.' } },
  { id: 'e11-12', source: '11', target: '12', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'NOTION_REFERENCE', validationCriterion: '월별 폴더는 주간 회의록을 포함해야 합니다.' } },
  { id: 'e11-13', source: '11', target: '13', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 }, data: { conflictStatus: 'NONE', source: 'PROPOSAL_ACCEPTED', validationCriterion: '월별 폴더는 주간 회의록을 포함해야 합니다.' } },
  { id: 'e3-7', source: '3', target: '7', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 1.5, strokeDasharray: '5,5' }, data: { conflictStatus: 'NONE', source: 'PROPOSAL_ACCEPTED', validationCriterion: 'PRD는 API 명세를 참조해야 합니다.' } },
  { id: 'e3-8', source: '3', target: '8', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 1.5, strokeDasharray: '5,5' }, data: { conflictStatus: 'NONE', source: 'PROPOSAL_ACCEPTED', validationCriterion: 'PRD는 DB 설계서를 참조해야 합니다.' } },
  { id: 'e3-9', source: '3', target: '9', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 1.5, strokeDasharray: '5,5' }, data: { conflictStatus: 'NONE', source: 'PROPOSAL_ACCEPTED', validationCriterion: 'PRD는 FE 가이드를 참조해야 합니다.' } },
  { id: 'e3-13', source: '3', target: '13', type: 'conflict', animated: true, style: { stroke: '#DC2626', strokeWidth: 3, strokeDasharray: '8,8' }, data: { conflictStatus: 'CONFLICT', source: 'NOTION_REFERENCE', validationCriterion: 'PG사 선택 기준이 모든 관련 문서에서 일치해야 합니다.' } },
];