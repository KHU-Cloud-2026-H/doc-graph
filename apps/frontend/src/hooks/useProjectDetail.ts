import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
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

// ── 정합성 검증 ON/OFF ────────────────────────────────────────────

export function useProjectValidation(projectId: number | undefined) {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery<{ enabled: boolean }>({
    queryKey: ['project-validation', projectId],
    queryFn: () => apiClient.GET<{ enabled: boolean }>(`/api/projects/${projectId}/validation`),
    enabled: projectId !== undefined,
  })
  const toggle = useMutation({
    mutationFn: (enabled: boolean) =>
      apiClient.PUT(`/api/projects/${projectId}/validation`, { enabled }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-validation', projectId] }),
  })
  return { enabled: data?.enabled ?? true, isLoading, toggle }
}

// ── AI 사용량 ─────────────────────────────────────────────────────

interface AiUsageSummary {
  totalCalls?: number
  totalPromptTokens?: number
  totalCompletionTokens?: number
  totalTokens?: number
  byModel?: { model?: string; calls?: number; promptTokens?: number; completionTokens?: number }[]
}

export function useProjectAiUsage(projectId: number | undefined) {
  const { data, isLoading } = useQuery<AiUsageSummary>({
    queryKey: ['project-ai-usage', projectId],
    queryFn: () => apiClient.GET<AiUsageSummary>(`/api/projects/${projectId}/ai-usage`),
    enabled: projectId !== undefined,
  })
  return { usage: data ?? null, isLoading }
}

// ── 그래프 룰 ─────────────────────────────────────────────────────

export interface RuleDetail {
  id: number
  projectId: number | null
  sourceType: string
  targetType: string
  validationCriterion: string
  isDefault: boolean
}

export function useProjectRules(projectId: number | undefined) {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery<RuleDetail[]>({
    queryKey: ['project-rules', projectId],
    queryFn: () => apiClient.GET<RuleDetail[]>(`/api/projects/${projectId}/rules`),
    enabled: projectId !== undefined,
  })
  const addRule = useMutation({
    mutationFn: (rule: { sourceType: string; targetType: string; validationCriterion: string }) =>
      apiClient.POST(`/api/projects/${projectId}/rules`, rule),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-rules', projectId] }),
  })
  const deleteRule = useMutation({
    mutationFn: (ruleId: number) =>
      apiClient.DELETE(`/api/projects/${projectId}/rules/${ruleId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-rules', projectId] }),
  })
  return { rules: data ?? [], isLoading, addRule, deleteRule }
}

// ── Webhook 알림 ──────────────────────────────────────────────────

export function useProjectWebhook(projectId: number | undefined) {
  const qc = useQueryClient()
  const { data, isLoading, isError } = useQuery<{ url: string | null }>({
    queryKey: ['project-webhook', projectId],
    queryFn: () => apiClient.GET<{ url: string | null }>(`/api/projects/${projectId}/webhook`),
    enabled: projectId !== undefined,
    retry: false,
  })
  const save = useMutation({
    mutationFn: (url: string) =>
      apiClient.PUT(`/api/projects/${projectId}/webhook`, { url }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['project-webhook', projectId] }),
  })
  return { url: data?.url ?? null, isLoading, isError, save }
}

export function useLatestValidationTask(projectId: number | undefined) {
  const { data } = useQuery<{ content?: { createdAt?: string }[] }>({
    queryKey: ['validation-tasks', projectId],
    queryFn: () =>
      apiClient.GET(`/api/projects/${projectId}/validation-tasks?page=0&size=1`),
    enabled: projectId !== undefined,
    staleTime: 30 * 1000,
  })
  const createdAt = data?.content?.[0]?.createdAt
  return { lastValidatedAt: createdAt ? new Date(createdAt) : null }
}
