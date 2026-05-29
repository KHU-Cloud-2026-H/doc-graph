import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

export interface ProjectIcon {
  type: 'EMOJI' | 'EXTERNAL' | 'FILE'
  value: string
}

export interface ProjectSummary {
  id: number
  name: string
  memberCount: number
  documentCount: number
  unresolvedConflictCount: number
  lastNotionChangedAt: string | null
  rootPageTitle: string | null
  rootPageIcon: ProjectIcon | null
}

export function useProjects(workspaceId: number | undefined) {
  const { data, isLoading, isError } = useQuery<ProjectSummary[]>({
    queryKey: ['projects', workspaceId],
    queryFn: () => apiClient.GET<ProjectSummary[]>(`/api/workspaces/${workspaceId}/projects`),
    enabled: workspaceId !== undefined,
  })

  return {
    projects: data ?? [],
    isLoading,
    isError,
  }
}
