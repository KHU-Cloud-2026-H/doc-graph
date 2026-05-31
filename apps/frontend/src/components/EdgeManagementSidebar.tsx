import { useState, useRef, useCallback } from 'react';
import { X, Trash2, Plus, ArrowRight, GitBranch } from 'lucide-react';
import type { AppEdge } from '../features/graph/mockData';
import type { DocumentFlowNode } from '../features/graph/mockData';

type Props = {
  edges: AppEdge[];
  nodes: DocumentFlowNode[];
  onClose: () => void;
  onDeleteEdge: (edgeId: string) => void;
  onAddEdge: (sourceId: string, targetId: string) => void;
};

export const EdgeManagementSidebar = ({ edges, nodes, onClose, onDeleteEdge, onAddEdge }: Props) => {
  const [showAddForm, setShowAddForm] = useState(false);
  const [sourceId, setSourceId] = useState('');
  const [targetId, setTargetId] = useState('');
  const [width, setWidth] = useState(420);
  const isResizing = useRef(false);

  const onMouseDown = useCallback(() => {
    isResizing.current = true;
    document.body.style.userSelect = 'none';

    const onMouseMove = (e: MouseEvent) => {
      if (!isResizing.current) return;
      const newWidth = window.innerWidth - e.clientX;
      if (newWidth >= 240 && newWidth <= 600) {
        setWidth(newWidth);
      }
    };

    const onMouseUp = () => {
      isResizing.current = false;
      document.body.style.userSelect = '';
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }, []);

  const getNodeLabel = (nodeId: string) =>
    nodes.find((n) => n.id === nodeId)?.data.label ?? nodeId;

  const managedEdges = edges.filter((e) => e.type !== 'conflict');

  const handleAdd = () => {
    if (!sourceId || !targetId || sourceId === targetId) return;
    onAddEdge(sourceId, targetId);
    setSourceId('');
    setTargetId('');
    setShowAddForm(false);
  };

  return (
    <aside
      className="absolute top-0 right-0 h-full bg-white border-l border-slate-200 shadow-xl z-30 flex flex-col"
      style={{ width }}
    >
      {/* Resize Handle */}
      <div
        onMouseDown={onMouseDown}
        className="absolute left-0 top-0 h-full w-1 cursor-col-resize transition-colors z-10"
        style={{ userSelect: 'none' }}
      />

      {/* Header */}
      <div className="flex items-center justify-between px-5 py-4 border-b border-slate-200 shrink-0">
        <div className="flex items-center gap-2">
          <GitBranch className="w-4 h-4 text-slate-500" />
          <span className="text-sm font-semibold text-slate-800">엣지 관리</span>
          <span className="ml-1 text-xs bg-slate-100 text-slate-500 rounded-full px-2 py-0.5">
            {managedEdges.length}
          </span>
        </div>
        <button
          onClick={onClose}
          className="p-1 rounded hover:bg-slate-100 transition-colors text-slate-400 hover:text-slate-600"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Edge List */}
      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-2">
        {managedEdges.length === 0 ? (
          <p className="text-xs text-slate-400 text-center py-8">엣지가 없습니다.</p>
        ) : (
          managedEdges.map((edge) => (
            <div
              key={edge.id}
              className="flex items-center justify-between gap-2 px-3 py-2.5 rounded-lg border border-slate-100 bg-slate-50 group"
            >
              <div className="flex items-center gap-2 min-w-0">
                <span className="text-xs text-slate-700 truncate">
                  {getNodeLabel(edge.source)}
                </span>
                <ArrowRight className="w-3 h-3 text-slate-400 shrink-0" />
                <span className="text-xs text-slate-700 truncate">
                  {getNodeLabel(edge.target)}
                </span>
              </div>
              <button
                onClick={() => onDeleteEdge(edge.id)}
                className="p-1 rounded hover:bg-red-50 text-slate-300 hover:text-red-500 transition-colors shrink-0 opacity-0 group-hover:opacity-100"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          ))
        )}
      </div>

      {/* Add Edge */}
      <div className="px-4 py-4 border-t border-slate-200 shrink-0">
        {!showAddForm ? (
          <button
            onClick={() => setShowAddForm(true)}
            className="w-full flex items-center justify-center gap-2 py-2 rounded-lg border border-dashed border-slate-300 text-xs text-slate-500 hover:border-blue-400 hover:text-blue-600 hover:bg-blue-50 transition-colors"
          >
            <Plus className="w-3.5 h-3.5" />
            새 엣지 추가
          </button>
        ) : (
          <div className="space-y-2">
            <select
              value={sourceId}
              onChange={(e) => setSourceId(e.target.value)}
              className="w-full text-xs border border-slate-200 rounded-lg px-3 py-2 text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">출발 문서 선택</option>
              {nodes.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.data.label}
                </option>
              ))}
            </select>
            <select
              value={targetId}
              onChange={(e) => setTargetId(e.target.value)}
              className="w-full text-xs border border-slate-200 rounded-lg px-3 py-2 text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">도착 문서 선택</option>
              {nodes.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.data.label}
                </option>
              ))}
            </select>
            <div className="flex gap-2">
              <button
                onClick={() => { setShowAddForm(false); setSourceId(''); setTargetId(''); }}
                className="flex-1 py-2 rounded-lg border border-slate-200 text-xs text-slate-500 hover:bg-slate-50 transition-colors"
              >
                취소
              </button>
              <button
                onClick={handleAdd}
                disabled={!sourceId || !targetId || sourceId === targetId}
                className="flex-1 py-2 rounded-lg bg-blue-600 text-xs text-white font-medium hover:bg-blue-700 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
              >
                추가
              </button>
            </div>
          </div>
        )}
      </div>
    </aside>
  );
};