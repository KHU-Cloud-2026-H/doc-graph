import { useMutation } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'

export function useDeleteProject(projectId: number | undefined) {
    return useMutation({
        mutationFn: () => apiClient.DELETE(`/api/projects/${projectId}`),
    })
}
