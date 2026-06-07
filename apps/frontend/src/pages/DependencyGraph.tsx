import { useCallback, useState, useEffect } from 'react';
import {
  ReactFlow,
  Controls,
  useNodesState,
  useEdgesState,
} from '@xyflow/react';

import '@xyflow/react/dist/style.css';

import { ExternalLink, GitBranch } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { DocumentNode } from '../features/graph/DocumentNode';
import { ConflictEdge } from '../features/graph/ConflictEdge';
import type { AppEdge, DocumentFlowNode } from '../features/graph/mockData';
import { useProjectGraph } from '../hooks/useProjectGraph';
import { useAddEdge, useDeleteEdge, useAcceptProposal, useRejectProposal } from '../hooks/useEdgeMutations';

import { GraphRightSidebar } from '../components/GraphRightSidebar';
import { EdgeManagementSidebar } from '../components/EdgeManagementSidebar';
import { useProjectDetail } from '../hooks/useProjectDetail';
import { useAppStore } from '../store';

const nodeTypes = {
  document: DocumentNode,
};

const edgeTypes = {
  conflict: ConflictEdge,
};

export const DependencyGraph = () => {
  const workspaces = useAppStore((state) => state.workspaces);
  const projects = useAppStore((state) => state.projects);
  const { workspaceId, projectId } = useParams();
  const numericProjectId = projectId ? Number(projectId) : undefined;

  const workspaceName = workspaces.find((w) => w.id === Number(workspaceId))?.name ?? workspaceId;
  const projectName = projects.find((p) => p.id === numericProjectId)?.name ?? '';

  const [showRightSidebar, setShowRightSidebar] = useState(false);
  const [showEdgePanel, setShowEdgePanel] = useState(false);
  const [selectedEdge, setSelectedEdge] = useState<AppEdge | null>(null);
  const [pendingConnect, setPendingConnect] = useState<{ source: string; target: string } | null>(null);

  // 노드가 없으면 sync 대기 중 — 최대 2초 간격으로 재조회
  const { nodes: apiNodes, edges: apiEdges } = useProjectGraph(numericProjectId, { polling: true });
  const { project } = useProjectDetail(numericProjectId);
  const notionRootUrl = project?.notionRootPageId
    ? `https://notion.so/${project.notionRootPageId.replace(/-/g, '')}`
    : undefined;
  const addEdge = useAddEdge(numericProjectId);
  const deleteEdge = useDeleteEdge(numericProjectId);
  const acceptProposal = useAcceptProposal(numericProjectId);
  const rejectProposal = useRejectProposal(numericProjectId);

  const [nodes, setNodes, onNodesChange] = useNodesState<DocumentFlowNode>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<AppEdge>([]);

  useEffect(() => { setNodes(apiNodes); }, [apiNodes, setNodes]);

  useEffect(() => {
    setEdges(apiEdges.map((edge) => ({
      ...edge,
      data: {
        ...(edge.data ?? {}),
        onClick: () => {
          setSelectedEdge(edge);
          setShowRightSidebar(true);
          setShowEdgePanel(false);
        },
      },
    })));
  }, [apiEdges, setEdges]);

  // ReactFlow 드래그 연결 → EdgeManagementSidebar pre-fill
  const onConnect = useCallback((params: { source: string | null; target: string | null }) => {
    if (!params.source || !params.target) return;
    setPendingConnect({ source: params.source, target: params.target });
    setShowEdgePanel(true);
    setShowRightSidebar(false);
  }, []);

  // 직선 엣지 클릭 (일반·제안·충돌 전부)
  const handleEdgeClick = useCallback((_: React.MouseEvent, edge: AppEdge) => {
    setSelectedEdge(edge);
    setShowRightSidebar(true);
    setShowEdgePanel(false);
  }, []);

  const handleDeleteEdge = useCallback((edgeFlowId: string) => {
    if (edgeFlowId.startsWith('e-')) {
      const backendId = parseInt(edgeFlowId.slice(2), 10);
      if (!isNaN(backendId)) {
        deleteEdge.mutate(backendId);
        return;
      }
    }
    // proposal 엣지는 엣지 관리 패널에서 삭제 불가 (수락/거절로만 처리)
  }, [deleteEdge]);

  const handleAddEdge = useCallback((sourceId: string, targetId: string, validationCriterion: string) => {
    addEdge.mutate({
      sourceDocumentId: Number(sourceId),
      targetDocumentId: Number(targetId),
      validationCriterion,
    }, {
      onSuccess: () => setPendingConnect(null),
    });
  }, [addEdge]);

  const handleAcceptProposal = useCallback(() => {
    if (!selectedEdge?.id.startsWith('p-')) return;
    const proposalId = parseInt(selectedEdge.id.slice(2), 10);
    if (!isNaN(proposalId)) {
      acceptProposal.mutate(proposalId, {
        onSuccess: () => { setShowRightSidebar(false); setSelectedEdge(null); },
      });
    }
  }, [selectedEdge, acceptProposal]);

  const handleRejectProposal = useCallback(() => {
    if (!selectedEdge?.id.startsWith('p-')) return;
    const proposalId = parseInt(selectedEdge.id.slice(2), 10);
    if (!isNaN(proposalId)) {
      rejectProposal.mutate(proposalId, {
        onSuccess: () => { setShowRightSidebar(false); setSelectedEdge(null); },
      });
    }
  }, [selectedEdge, rejectProposal]);

  const selectedSourceLabel = nodes.find((n) => n.id === selectedEdge?.source)?.data.label ?? '';
  const selectedTargetLabel = nodes.find((n) => n.id === selectedEdge?.target)?.data.label ?? '';

  return (
    <main className="flex-1 flex flex-col h-full bg-slate-50 relative">
      {/* Header */}
      <header className="h-14 flex items-center justify-between px-6 border-b border-slate-200 bg-white shrink-0 z-20">
        <div className="flex items-center text-sm text-slate-500 flex-1">
          <Link to={`/w/${workspaceId}`} className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900">
            {workspaceName}
          </Link>
          <span className="mx-1 text-[14px] opacity-40">/</span>
          <Link to={`/w/${workspaceId}/p/${projectId}/graph`} className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900">
            {projectName}
          </Link>
          <span className="mx-1 text-[14px] opacity-40">/</span>
          <span className="px-2 py-1 rounded text-slate-900 font-medium truncate max-w-[300px]">Dependency Graph</span>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => { setShowEdgePanel(true); setShowRightSidebar(false); setPendingConnect(null); }}
            className="flex items-center gap-1.5 px-3 py-1 border border-slate-200 rounded hover:bg-slate-50 transition-colors text-xs font-medium text-slate-600"
          >
            <GitBranch className="w-4 h-4" />
            엣지 관리
          </button>
          <a
            href={notionRootUrl}
            target="_blank"
            rel="noopener noreferrer"
            className={`flex items-center gap-1.5 px-3 py-1 border border-slate-200 rounded hover:bg-slate-50 transition-colors text-xs font-medium text-slate-600 ${!notionRootUrl ? 'pointer-events-none opacity-40' : ''}`}
          >
            <ExternalLink className="w-4 h-4" />
            Edit in Notion
          </a>
        </div>
      </header>

      {/* Graph Area */}
      <div className="flex-1 relative w-full h-full" style={{ backgroundImage: 'radial-gradient(circle, #cbd5e1 1.5px, transparent 1.5px)', backgroundSize: '32px 32px' }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onEdgeClick={handleEdgeClick}
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

      {showRightSidebar && selectedEdge && (
        <GraphRightSidebar
          edge={selectedEdge}
          sourceLabel={selectedSourceLabel}
          targetLabel={selectedTargetLabel}
          isAccepting={acceptProposal.isPending}
          isRejecting={rejectProposal.isPending}
          onAccept={handleAcceptProposal}
          onReject={handleRejectProposal}
          onClose={() => { setShowRightSidebar(false); setSelectedEdge(null); }}
        />
      )}

      {showEdgePanel && (
        <EdgeManagementSidebar
          edges={edges}
          nodes={nodes}
          pendingConnect={pendingConnect}
          onClose={() => { setShowEdgePanel(false); setPendingConnect(null); }}
          onDeleteEdge={handleDeleteEdge}
          onAddEdge={handleAddEdge}
        />
      )}
    </main>
  );
};
