import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../lib/apiClient'
import type { User } from '../types/auth'

export const AUTH_QUERY_KEY = ['auth', 'me'] as const

export function useAuth() {
  const { data: user, isLoading, isError } = useQuery<User>({
    queryKey: AUTH_QUERY_KEY,
    queryFn: () => apiClient.GET<User>('/api/auth/me', { skipRedirect: true }),
    retry: false,
    staleTime: 5 * 60 * 1000,
  })

  return {
    user: user ?? null,
    isLoading,
    isLoggedIn: !isError && !!user,
  }
}

export function useLogout() {
  const qc = useQueryClient()

  return useMutation({
    mutationFn: () => apiClient.DELETE('/api/auth/sessions'),
    onSuccess: () => {
      qc.clear()
      window.location.href = '/login'
    },
  })
}
