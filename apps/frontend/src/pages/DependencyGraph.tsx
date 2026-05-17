import { useCallback, useState } from 'react';
import type { ReactNode } from 'react';
import {
  ReactFlow,
  Controls,
  useNodesState,
  useEdgesState,
  addEdge,
  Handle,
  Position,
  BaseEdge,
  EdgeLabelRenderer,
  getStraightPath
} from '@xyflow/react';

import type { NodeProps, EdgeProps, Node, Edge } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { ExternalLink, AlertTriangle, FileText, Database, Share2, BarChart2, Folder } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { GraphRightSidebar } from '../components/GraphRightSidebar';
import { useAppStore } from '../store';

/** 커스텀 문서 노드의 `data` 필드 타입 (React Flow `Node`의 제네릭과 맞춤) */
type DocumentNodeData = {
  label: string;
  id?: string;
  hasError?: boolean;
  icon: ReactNode;
};

type DocumentFlowNode = Node<DocumentNodeData, 'document'>;

/** 충돌 엣지 등에서 `edge.data`로 전달하는 콜백 타입 */
type ConflictEdgeData = { onClick?: () => void };

type ConflictFlowEdge = Edge<ConflictEdgeData, 'conflict'>;

type AppEdge = Edge<ConflictEdgeData, 'straight' | 'conflict'>;

// Custom Node components matching the design
const DocumentNode = ({ data, isConnectable }: NodeProps<DocumentFlowNode>) => {
  const isError = data.hasError;
  return (
    <Link to={`/w/sample-workspace/docs/${data.id || 'prd'}`} className={`flex flex-col items-center gap-2 relative`}>
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center bg-white border-2 shadow-sm relative transition-transform hover:scale-105 ${
        isError ? 'border-red-600 bg-red-50' : 'border-slate-800'
      }`}>
        <Handle type="target" position={Position.Left} isConnectable={isConnectable} className="opacity-0" />
        {data.icon}
        {isError && (
          <div className="absolute -top-1.5 -right-1.5 bg-red-600 text-white text-[10px] font-bold w-4 h-4 rounded-full flex items-center justify-center border-2 border-white">
            1
          </div>
        )}
        <Handle type="source" position={Position.Right} isConnectable={isConnectable} className="opacity-0" />
      </div>
      <div className={`text-[11px] font-medium whitespace-nowrap bg-white/80 px-1.5 py-0.5 rounded ${
        isError ? 'text-red-600 font-bold' : 'text-slate-600'
      }`}>
        {isError && 'ⓘ '}{data.label}
      </div>
    </Link>
  );
};

const ConflictEdge = ({
  sourceX,
  sourceY,
  targetX,
  targetY,
  style = {},
  markerEnd,
  data
}: EdgeProps<ConflictFlowEdge>) => {
  const [edgePath, labelX, labelY] = getStraightPath({
    sourceX,
    sourceY,
    targetX,
    targetY,
  });

  return (
    <>
      <BaseEdge 
        path={edgePath} 
        markerEnd={markerEnd} 
        style={style} 
        className="react-flow__edge-path" 
      />
      {/* Invisible thicker path to make edge clicking easier */}
      <BaseEdge 
        path={edgePath} 
        style={{ strokeWidth: 20, stroke: 'transparent', cursor: 'pointer' }}
        className="react-flow__edge-interaction"
        onClick={() => data?.onClick?.()}
      />
      <EdgeLabelRenderer>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
            pointerEvents: 'all',
          }}
          className="nodrag nopan z-50 cursor-pointer"
          onClick={() => data?.onClick?.()}
        >
          <div className="bg-red-600 rounded-full p-1 shadow-sm hover:scale-110 transition-transform flex items-center justify-center">
            <AlertTriangle className="w-4 h-4 text-white" />
          </div>
        </div>
      </EdgeLabelRenderer>
    </>
  );
};

const nodeTypes = {
  document: DocumentNode,
};

const edgeTypes = {
  conflict: ConflictEdge,
};

// TODO: 현재 노드의 data.id는 목업 데이터입니다.
// 실제 API 연동 시 node-document mapping 구조 재정비 필요

const initialNodes: DocumentFlowNode[] = [
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

const initialEdges: AppEdge[] = [
  { id: 'e1-2', source: '1', target: '2', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  { id: 'e1-3', source: '1', target: '3', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  { id: 'e3-4', source: '3', target: '4', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  { id: 'e3-5', source: '3', target: '5', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  { id: 'e6-7', source: '6', target: '7', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  { id: 'e6-8', source: '6', target: '8', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  { id: 'e6-9', source: '6', target: '9', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  { id: 'e10-11', source: '10', target: '11', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  { id: 'e11-12', source: '11', target: '12', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  { id: 'e11-13', source: '11', target: '13', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 2 } },
  // AI Inference (not animated)
  { id: 'e3-7', source: '3', target: '7', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 1.5, strokeDasharray: '5,5' } },
  { id: 'e3-8', source: '3', target: '8', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 1.5, strokeDasharray: '5,5' } },
  { id: 'e3-9', source: '3', target: '9', type: 'straight', style: { stroke: '#1E293B', strokeWidth: 1.5, strokeDasharray: '5,5' } },
  // Conflict (animated via react-flow built-in class + custom component for label)
  { id: 'e3-13', source: '3', target: '13', type: 'conflict', animated: true, style: { stroke: '#DC2626', strokeWidth: 3, strokeDasharray: '8,8' } },
];

export const DependencyGraph = () => {
  const workspace = useAppStore((state) => state.workspace);
  const { workspaceId } = useParams();
  const [showRightSidebar, setShowRightSidebar] = useState(false);

  // TODO: workspaceId를 URL params에서 읽어오도록 수정 필요
  // 현재는 'sample-workspace'로 하드코딩되어 있음

  // Bind the onClick handler directly to the edge data
  const edgesWithData: AppEdge[] = initialEdges.map((edge) => ({
    ...edge,
    data: {
      ...(edge.data ?? {}),
      onClick: edge.id === 'e3-13' ? () => setShowRightSidebar(true) : undefined,
    },
  }));

  const [nodes, , onNodesChange] = useNodesState<DocumentFlowNode>(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState<AppEdge>(edgesWithData);

  const onConnect = useCallback(
    (params: any) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  return (
    <main className="flex-1 flex flex-col h-full bg-slate-50 relative">
      {/* Header */}
      <header className="h-14 flex items-center justify-between px-6 border-b border-slate-200 bg-white shrink-0 z-20">
        <div className="flex items-center text-sm text-slate-500 flex-1">
          <Link to={`/w/${workspaceId}`} className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900">
            {workspace}
          </Link>
          <span className="mx-1 text-[14px] opacity-40">/</span>
          <span className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors text-slate-900 font-medium truncate max-w-[300px]">Dependency Graph</span>
        </div>
        <button className="flex items-center gap-1.5 px-3 py-1 border border-slate-200 rounded hover:bg-slate-50 transition-colors text-xs font-medium text-slate-600">
          <ExternalLink className="w-4 h-4" />
          Edit in Notion
        </button>
      </header>

      {/* Graph Area */}
      <div className="flex-1 relative w-full h-full" style={{ backgroundImage: 'radial-gradient(circle, #cbd5e1 1.5px, transparent 1.5px)', backgroundSize: '32px 32px' }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          fitView
          attributionPosition="bottom-right"
        >
          <Controls position="bottom-right" className="bg-white border border-slate-200 shadow-sm rounded-xl overflow-hidden mb-6 mr-6" showInteractive={false} />
          
          <div className="absolute bottom-6 left-6 bg-white/95 border border-slate-200 rounded-xl p-4 shadow-lg z-20">
            <h4 className="text-xs font-semibold text-slate-800 mb-3">Graph Legend</h4>
            <div className="flex flex-col gap-2">
              <div className="flex items-center gap-3">
                <div className="w-8 h-0.5 bg-slate-800"></div>
                <span className="text-[11px] text-slate-600">Direct Dependency</span>
              </div>
              <div className="flex items-center gap-3">
                <div className="w-8 h-0.5 border-t border-dashed border-slate-800"></div>
                <span className="text-[11px] text-slate-600">AI Inference Link</span>
              </div>
              <div className="flex items-center gap-3">
                <div className="w-8 h-[3px] border-t-2 border-dashed border-red-600"></div>
                <span className="text-[11px] font-medium text-red-600">Conflict Detected</span>
              </div>
            </div>
          </div>
        </ReactFlow>
      </div>

      {showRightSidebar && <GraphRightSidebar onClose={() => setShowRightSidebar(false)} />}
    </main>
  );
};