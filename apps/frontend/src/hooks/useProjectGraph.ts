import { useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { apiClient } from '../lib/apiClient'
import type { components } from '@docgraph/api-types'
import type { DocumentFlowNode, AppEdge } from '../features/graph/mockData'
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
  const flowEdges: AppEdge[] = edges.map((e) => ({
    id: `e-${e.id}`,
    source: String(e.sourceDocumentId),
    target: String(e.targetDocumentId),
    type: e.conflictStatus === 'CONFLICT' ? ('conflict' as const) : ('straight' as const),
    style: {
      stroke: e.conflictStatus === 'CONFLICT' ? '#DC2626' : '#1E293B',
      strokeWidth: 2,
    },
    data: {
      conflictStatus: e.conflictStatus ?? 'NONE',
      source: e.source,
      validationCriterion: e.validationCriterion,
    },
  }))

  const proposalEdges: AppEdge[] = proposals.map((p) => ({
    id: `p-${p.id}`,
    source: String(p.sourceDocumentId),
    target: String(p.targetDocumentId),
    type: 'straight' as const,
    style: { stroke: '#94A3B8', strokeWidth: 1.5, strokeDasharray: '5,5' },
    data: {
      conflictStatus: 'NONE' as const,
      source: 'PROPOSAL_ACCEPTED' as const,
      validationCriterion: p.validationCriterion,
    },
  }))

  return [...flowEdges, ...proposalEdges]
}

export function useProjectGraph(projectId: number | undefined) {
  const { data, isLoading, isError } = useQuery<ProjectGraphResponse>({
    queryKey: ['graph', projectId],
    queryFn: () => apiClient.GET<ProjectGraphResponse>(`/api/projects/${projectId}/graph`),
    enabled: projectId !== undefined,
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
