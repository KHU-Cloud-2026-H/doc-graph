import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'
import type { components } from '@docgraph/api-types'

export type NotionPageItem = components['schemas']['NotionWorkspacePageResponse']

export function useNotionPages(workspaceId: number | undefined) {
  const { data, isLoading } = useQuery<NotionPageItem[]>({
    queryKey: ['notion-pages', workspaceId],
    queryFn: () =>
      apiClient.GET<NotionPageItem[]>(`/api/workspaces/${workspaceId}/notion/pages`),
    enabled: workspaceId !== undefined,
  })
  return { pages: data ?? [], isLoading }
}

export function useNotionPageChildren(workspaceId: number | undefined, pageId: string | null) {
  const { data, isLoading } = useQuery<NotionPageItem[]>({
    queryKey: ['notion-page-children', workspaceId, pageId],
    queryFn: () =>
      apiClient.GET<NotionPageItem[]>(`/api/workspaces/${workspaceId}/notion/pages/${pageId}/children`),
    enabled: workspaceId !== undefined && pageId !== null,
  })
  return { children: data ?? [], isLoading }
}
