import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'
import type { components } from '@docgraph/api-types'

export type DocumentDetail = components['schemas']['DocumentDetail']

export function useDocument(documentId: number | undefined) {
  const { data, isLoading, isError } = useQuery<DocumentDetail>({
    queryKey: ['document', documentId],
    queryFn: () => apiClient.GET<DocumentDetail>(`/api/documents/${documentId}`),
    enabled: documentId !== undefined,
  })

  return {
    document: data ?? null,
    isLoading,
    isError,
  }
}
