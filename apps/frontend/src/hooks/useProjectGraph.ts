import { useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { apiClient } from '../lib/apiClient'
import type { components } from '@docgraph/api-types'
import type { DocumentFlowNode, AppEdge } from '../features/graph/mockData'
import { MarkerType } from '@xyflow/react'
import { FileText } from 'lucide-react'
import { createElement } from 'react'

type ProjectGraphResponse = components['schemas']['ProjectGraphResponse']
type GraphNodeResponse = components['schemas']['GraphNodeResponse']
type EdgeResponse = components['schemas']['EdgeResponse']
type EdgeProposalResponse = components['schemas']['EdgeProposalResponse']

function toFlowNodes(nodes: GraphNodeResponse[]): DocumentFlowNode[] {
  const cols = Math.ceil(Math.sqrt(nodes.length || 1))
  return nodes.map((n, i) => ({
    id: String(n.id),
    type: 'document' as const,
    position: {
      x: (i % cols) * 260 + 100,
      y: Math.floor(i / cols) * 180 + 100,
    },
    data: {
      label: n.title ?? '',
      icon: createElement(FileText, { className: 'text-slate-500 w-6 h-6' }),
    },
  }))
}

function toFlowEdges(edges: EdgeResponse[], proposals: EdgeProposalResponse[]): AppEdge[] {
  const flowEdges: AppEdge[] = edges.map((e) => {
    const stroke = e.conflictStatus === 'CONFLICT' ? '#DC2626' : '#1E293B'
    return {
    id: `e-${e.id}`,
    source: String(e.sourceDocumentId),
    target: String(e.targetDocumentId),
    type: e.conflictStatus === 'CONFLICT' ? ('conflict' as const) : ('straight' as const),
    style: {
      stroke,
      strokeWidth: 2,
    },
    markerEnd: { type: MarkerType.ArrowClosed, width: 18, height: 18, color: stroke },
    data: {
      conflictStatus: e.conflictStatus ?? 'NONE',
      source: e.source,
      validationCriterion: e.validationCriterion,
    },
    }
  })

  const proposalEdges: AppEdge[] = proposals.map((p) => ({
    id: `p-${p.id}`,
    source: String(p.sourceDocumentId),
    target: String(p.targetDocumentId),
    type: 'straight' as const,
    style: { stroke: '#94A3B8', strokeWidth: 1.5, strokeDasharray: '5,5' },
    markerEnd: { type: MarkerType.ArrowClosed, width: 18, height: 18, color: '#94A3B8' },
    data: {
      conflictStatus: 'NONE' as const,
      source: 'PROPOSAL_ACCEPTED' as const,
      validationCriterion: p.validationCriterion,
    },
  }))

  return [...flowEdges, ...proposalEdges]
}

export function useProjectGraph(projectId: number | undefined, { polling = false }: { polling?: boolean } = {}) {
  const { data, isLoading, isError } = useQuery<ProjectGraphResponse>({
    queryKey: ['graph', projectId],
    queryFn: () => apiClient.GET<ProjectGraphResponse>(`/api/projects/${projectId}/graph`),
    enabled: projectId !== undefined,
    // 노드가 없거나 polling 모드일 때 2초마다 재조회 (새 프로젝트 sync 대기)
    refetchInterval: (query) => {
      if (!polling) return false;
      const nodes = query.state.data?.nodes ?? [];
      return nodes.length === 0 ? 2000 : false;
    },
  })

  const nodes = useMemo(() => toFlowNodes(data?.nodes ?? []), [data?.nodes])
  const edges = useMemo(() => toFlowEdges(data?.edges ?? [], data?.proposals ?? []), [data?.edges, data?.proposals])

  return {
    nodes,
    edges,
    rawEdges: data?.edges ?? [],
    isLoading,
    isError,
  }
}
