import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

export function useIgnoreConflict() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (conflictId: number) =>
      apiClient.POST(`/api/conflicts/${conflictId}/ignore`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inbox'] })
    },
  })
}

export function useUnignoreConflict() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (conflictId: number) =>
      apiClient.DELETE(`/api/conflicts/${conflictId}/ignore`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inbox'] })
    },
  })
}

export function useApproveConflict() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ conflictId, findingId }: { conflictId: number; findingId: number }) =>
      apiClient.POST(`/api/conflicts/${conflictId}/findings/${findingId}/approve`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inbox'] })
    },
  })
}
