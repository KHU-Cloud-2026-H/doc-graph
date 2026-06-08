import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

interface AddEdgeParams {
  sourceDocumentId: number
  targetDocumentId: number
  validationCriterion: string
}

export function useAddEdge(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (params: AddEdgeParams) =>
      apiClient.POST<{ id: number }>(`/api/projects/${projectId}/edges`, params),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['graph', projectId] }),
  })
}

export function useDeleteEdge(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (edgeId: number) =>
      apiClient.DELETE(`/api/projects/${projectId}/edges/${edgeId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['graph', projectId] }),
  })
}

export function useAcceptProposal(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (proposalId: number) =>
      apiClient.POST(`/api/projects/${projectId}/proposals/${proposalId}/accept`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['graph', projectId] }),
  })
}

export function useRejectProposal(projectId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (proposalId: number) =>
      apiClient.DELETE(`/api/projects/${projectId}/proposals/${proposalId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['graph', projectId] }),
  })
}
