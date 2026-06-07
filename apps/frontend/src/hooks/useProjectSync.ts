import { useMutation } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

export function useProjectSync() {
  return useMutation({
    mutationFn: (projectId: number) =>
      apiClient.POST(`/api/projects/${projectId}/sync`),
  })
}
