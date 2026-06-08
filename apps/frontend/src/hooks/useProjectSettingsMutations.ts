import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

// ── 프로젝트 멤버 ──────────────────────────────────────────────────

export function useAddProjectMember(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ workspaceMemberId, role }: { workspaceMemberId: number; role: 'ADMIN' | 'MEMBER' }) =>
      apiClient.POST(`/api/projects/${projectId}/members`, { workspaceMemberId, role }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-detail', projectId] }),
  })
}

export function useDeleteProjectMember(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (memberId: number) =>
      apiClient.DELETE(`/api/projects/${projectId}/members/${memberId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-detail', projectId] }),
  })
}

export function useUpdateProjectMemberRole(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ memberId, role }: { memberId: number; role: 'ADMIN' | 'MEMBER' }) =>
      apiClient.PATCH(`/api/projects/${projectId}/members/${memberId}`, { role }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-detail', projectId] }),
  })
}

// ── 카테고리 ──────────────────────────────────────────────────────

export function useAddProjectCategory(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ notionPageId, documentType }: { notionPageId: string; documentType: string }) =>
      apiClient.POST(`/api/projects/${projectId}/categories`, { notionPageId, documentType }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-categories', projectId] }),
  })
}

export function useUpdateProjectCategoryType(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ categoryId, documentType }: { categoryId: number; documentType: string }) =>
      apiClient.PATCH(`/api/projects/${projectId}/categories/${categoryId}`, { documentType }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-categories', projectId] }),
  })
}

export function useDeleteProjectCategory(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (categoryId: number) =>
      apiClient.DELETE(`/api/projects/${projectId}/categories/${categoryId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-categories', projectId] }),
  })
}

// ── 타입별 담당자 ──────────────────────────────────────────────────

interface TypeAssigneeItem {
  documentType: string
  assigneeMemberId: number | null
}

export function useUpdateTypeAssignees(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (assignees: TypeAssigneeItem[]) =>
      apiClient.PUT(`/api/projects/${projectId}/type-assignees`, { assignees }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-type-assignees', projectId] }),
  })
}

// ── 프로젝트 생성 ─────────────────────────────────────────────────

export function useCreateProject(workspaceId: number | undefined) {
  return useMutation({
    mutationFn: ({ name, notionRootPageId }: { name: string; notionRootPageId: string }) =>
      apiClient.POST<{ id: number }>(`/api/workspaces/${workspaceId}/projects`, {
        name,
        notionRootPageId,
        validationEnabled: true,
      }),
  })
}
