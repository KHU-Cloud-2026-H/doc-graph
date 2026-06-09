import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'
import type { components } from '@docgraph/api-types'

type ConflictResponse = components['schemas']['ConflictResponse']
type ConflictFindingResponse = components['schemas']['ConflictFindingResponse']

interface PageResponse<T> {
  content: T[]
  totalElements: number
}

export type { ConflictResponse, ConflictFindingResponse }

export function useProjectConflicts(projectId: number | undefined) {
  const { data, isLoading } = useQuery<PageResponse<ConflictResponse>>({
    queryKey: ['conflicts', projectId],
    queryFn: () =>
      apiClient.GET<PageResponse<ConflictResponse>>(`/api/projects/${projectId}/conflicts?size=200`),
    enabled: projectId !== undefined,
    staleTime: 30 * 1000,
  })

  return {
    conflicts: data?.content ?? [],
    isLoading,
  }
}
