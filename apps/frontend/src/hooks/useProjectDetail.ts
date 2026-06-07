import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

export interface ProjectMemberSummary {
  id: number
  name: string
  role: 'ADMIN' | 'MEMBER'
}

export interface ProjectDetail {
  id: number
  name: string
  notionRootPageId: string
  members: ProjectMemberSummary[]
  memberCount: number
  documentCount: number
  unresolvedConflictCount: number
  lastNotionChangedAt: string | null
}

export interface CategoryDetail {
  id: number
  notionPageId: string
  documentType: string
}

export interface TypeAssigneeDetail {
  documentType: string
  assigneeMemberId: number | null
}

export function useProjectDetail(projectId: number | undefined) {
  const { data, isLoading, isError } = useQuery<ProjectDetail>({
    queryKey: ['project-detail', projectId],
    queryFn: () => apiClient.GET<ProjectDetail>(`/api/projects/${projectId}`),
    enabled: projectId !== undefined,
  })
  return { project: data ?? null, isLoading, isError }
}

export function useProjectCategories(projectId: number | undefined) {
  const { data, isLoading } = useQuery<CategoryDetail[]>({
    queryKey: ['project-categories', projectId],
    queryFn: () => apiClient.GET<CategoryDetail[]>(`/api/projects/${projectId}/categories`),
    enabled: projectId !== undefined,
  })
  return { categories: data ?? [], isLoading }
}

export function useProjectTypeAssignees(projectId: number | undefined) {
  const { data, isLoading } = useQuery<TypeAssigneeDetail[]>({
    queryKey: ['project-type-assignees', projectId],
    queryFn: () => apiClient.GET<TypeAssigneeDetail[]>(`/api/projects/${projectId}/type-assignees`),
    enabled: projectId !== undefined,
  })
  return { typeAssignees: data ?? [], isLoading }
}
