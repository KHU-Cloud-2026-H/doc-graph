import { useCallback, useState } from 'react';
import {
  ReactFlow,
  Controls,
  useNodesState,
  useEdgesState,
  addEdge,
} from '@xyflow/react';

import '@xyflow/react/dist/style.css';

import { ExternalLink } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { DocumentNode } from '../features/graph/DocumentNode';
import { ConflictEdge } from '../features/graph/ConflictEdge';
import { initialNodes, initialEdges } from '../features/graph/mockData';
import type { AppEdge, DocumentFlowNode } from '../features/graph/mockData';

import { GraphRightSidebar } from '../components/GraphRightSidebar';
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
  const workspaceName = workspaces.find((w) => w.id === Number(workspaceId))?.name ?? workspaceId;
  const projectName = projects.find((p) => p.id === Number(projectId))?.name ?? '';
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
            {workspaceName}
          </Link>
          <span className="mx-1 text-[14px] opacity-40">/</span>
          <Link to={`/w/${workspaceId}/p/${projectId}`} className="px-2 py-1 rounded hover:bg-slate-100 cursor-pointer transition-colors hover:text-slate-900">
            {projectName}
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