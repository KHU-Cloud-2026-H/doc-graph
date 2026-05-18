import { BaseEdge, EdgeLabelRenderer, getStraightPath } from '@xyflow/react';
import type { EdgeProps } from '@xyflow/react';
import { AlertTriangle } from 'lucide-react';
import type { ConflictFlowEdge } from './mockData';

export const ConflictEdge = ({
  sourceX,
  sourceY,
  targetX,
  targetY,
  style = {},
  markerEnd,
  data,
}: EdgeProps<ConflictFlowEdge>) => {
  const [edgePath, labelX, labelY] = getStraightPath({ sourceX, sourceY, targetX, targetY });

  return (
    <>
      <BaseEdge path={edgePath} markerEnd={markerEnd} style={style} className="react-flow__edge-path" />
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