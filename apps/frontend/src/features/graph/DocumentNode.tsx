import { Handle, Position } from '@xyflow/react';
import type { NodeProps } from '@xyflow/react';
import { Link, useParams } from 'react-router-dom';
import type { DocumentFlowNode } from './mockData';

export const DocumentNode = ({ data, isConnectable }: NodeProps<DocumentFlowNode>) => {
  const { workspaceId, projectId } = useParams();
  const isError = data.hasError;

  return (
    <Link
      to={`/w/${workspaceId}/p/${projectId}/docs/${data.id}`}
      className="flex flex-col items-center gap-2 relative"
    >
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
