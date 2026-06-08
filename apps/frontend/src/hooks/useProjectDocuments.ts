import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'
import type { components } from '@docgraph/api-types'
import type { DocumentNode } from '../store'

type DocumentSummary = components['schemas']['DocumentSummary']

interface PageResponse {
  content?: DocumentSummary[]
}

function toDocumentTree(docs: DocumentSummary[]): DocumentNode[] {
  const map = new Map<number, DocumentNode>()

  for (const d of docs) {
    if (d.id == null) continue
    const emoji = d.icon?.type === 'EMOJI' ? d.icon.value : undefined
    map.set(d.id, {
      id: d.id,
      title: d.title ?? '',
      emoji,
      notionPageId: d.notionPageId,
      type: d.type as import('../store').DocumentType ?? undefined,
      children: [],
    })
  }

  const roots: DocumentNode[] = []
  for (const d of docs) {
    if (d.id == null) continue
    const node = map.get(d.id)!
    if (d.parentDocumentId != null && map.has(d.parentDocumentId)) {
      map.get(d.parentDocumentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  }
  return roots
}

export function useProjectDocuments(projectId: number | undefined) {
  const { data, isLoading, isError } = useQuery<PageResponse>({
    queryKey: ['documents', projectId],
    queryFn: () =>
      apiClient.GET<PageResponse>(`/api/projects/${projectId}/documents?size=200`),
    enabled: projectId !== undefined,
  })

  return {
    documents: toDocumentTree(data?.content ?? []),
    isLoading,
    isError,
  }
}
