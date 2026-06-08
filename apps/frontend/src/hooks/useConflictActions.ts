import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'
import type { DocumentDetail } from './useDocument'

export function useIgnoreConflict() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (conflictId: number) =>
      apiClient.POST(`/api/conflicts/${conflictId}/ignore`, { reason: null }),
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

interface ApproveConflictVars {
  conflictId: number
  findingId: number
  // 서버 stale 가드의 낙관적 동시성 토큰 — 조회 시점에 본 target 문서의 notionLastEditedAt. 미동기화 target이면 null.
  expectedTargetNotionLastEditedAt: string | null
  // 옵티미스틱 반영용 — 봇이 교체할 target 문서·블록·새 텍스트.
  targetDocumentId?: number
  targetBlockId?: string
  newText?: string
}

export function useApproveConflict() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ conflictId, findingId, expectedTargetNotionLastEditedAt }: ApproveConflictVars) =>
      apiClient.POST(`/api/conflicts/${conflictId}/findings/${findingId}/approve`, {
        expectedTargetNotionLastEditedAt,
      }),
    onSuccess: (_data, vars) => {
      // 봇 쓰기 webhook은 루프 방지로 무시되므로, 승인 성공(204) 시 문서 캐시의 해당 블록 텍스트를
      // 옵티미스틱하게 교체해 즉시 반영한다. 백엔드도 같은 블록을 갱신하므로 이후 refetch와 일관.
      const { targetDocumentId, targetBlockId, newText } = vars
      if (targetDocumentId !== undefined && targetBlockId && newText !== undefined) {
        qc.setQueryData<DocumentDetail>(['document', targetDocumentId], (old) =>
          old
            ? {
                ...old,
                blocks: (old.blocks ?? []).map((b) =>
                  b.blockId === targetBlockId ? { ...b, text: newText } : b,
                ),
              }
            : old,
        )
      }
      qc.invalidateQueries({ queryKey: ['inbox'] })
    },
  })
}
