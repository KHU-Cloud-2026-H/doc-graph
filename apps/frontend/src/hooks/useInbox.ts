import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

export interface InboxDocumentRef {
  id: number
  title: string
  type: 'meeting_notes' | 'planning' | 'requirements' | 'design' | 'research' | null
}

export interface MyConflictRow {
  id: number
  edgeId: number
  workspaceId: number
  projectId: number
  projectName: string
  sourceDocument: InboxDocumentRef
  targetDocument: InboxDocumentRef
  title: string
  status: 'ACTIVE' | 'IGNORED'
  latestFindingId: number | null
  firstDetectedAt: string
  ignoredAt: string | null
}

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export function useInbox() {
  const { data, isLoading, isError } = useQuery<PageResponse<MyConflictRow>>({
    queryKey: ['inbox'],
    queryFn: () => apiClient.GET<PageResponse<MyConflictRow>>('/api/me/conflicts?status=ALL'),
    staleTime: 60 * 1000,
  })

  return {
    conflicts: data?.content ?? [],
    total: data?.totalElements ?? 0,
    isLoading,
    isError,
  }
}
