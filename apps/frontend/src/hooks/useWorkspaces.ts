import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

export interface WorkspaceSummary {
  id: number
  name: string
  memberCount: number
  projectCount: number
  documentCount: number
  unresolvedConflictCount: number
  lastNotionChangedAt: string | null
}

export function useWorkspaces() {
  const { data, isLoading, isError } = useQuery<WorkspaceSummary[]>({
    queryKey: ['workspaces'],
    queryFn: () => apiClient.GET<WorkspaceSummary[]>('/api/workspaces'),
  })

  return {
    workspaces: data ?? [],
    isLoading,
    isError,
  }
}
