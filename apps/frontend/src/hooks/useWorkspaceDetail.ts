import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

export interface WorkspaceMemberSummary {
  userId: number
  name: string
  email: string
  joinedAt: string
}

export interface WorkspaceDetail {
  id: number
  name: string
  members: WorkspaceMemberSummary[]
  memberCount: number
  projectCount: number
  documentCount: number
  unresolvedConflictCount: number
  lastNotionChangedAt: string | null
}

export function useWorkspaceDetail(workspaceId: number | undefined) {
  const { data, isLoading, isError } = useQuery<WorkspaceDetail>({
    queryKey: ['workspace', workspaceId],
    queryFn: () => apiClient.GET<WorkspaceDetail>(`/api/workspaces/${workspaceId}`),
    enabled: workspaceId !== undefined,
  })

  return {
    workspace: data ?? null,
    isLoading,
    isError,
  }
}
