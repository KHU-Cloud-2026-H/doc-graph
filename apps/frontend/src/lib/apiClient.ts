/**
 * API 클라이언트
 *
 * 백엔드(Spring Security OAuth2 세션 기반)와 통신하는 단일 진입점.
 *
 * 책임:
 * - 베이스 URL 관리
 * - 세션 쿠키 자동 전송 (credentials: 'include')
 * - 401 자동 처리 → Notion OAuth2 로그인으로 리다이렉트
 * - JSON 자동 파싱
 *
 * skipRedirect: true 를 넘기면 401 시 리다이렉트를 하지 않는다.
 * (useAuth의 초기 로그인 상태 확인에서 사용)
 */

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export const NOTION_LOGIN_URL = `${BASE_URL}/api/oauth2/authorization/notion`

interface RequestOptions extends RequestInit {
  skipRedirect?: boolean
}

async function request<T = unknown>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const { skipRedirect = false, headers = {}, ...rest } = options

  const response = await fetch(`${BASE_URL}${path}`, {
    ...rest,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(headers as Record<string, string>),
    },
  })

  if (response.status === 401) {
    if (!skipRedirect) {
      window.location.href = NOTION_LOGIN_URL
    }
    throw new Error('Unauthorized')
  }

  if (!response.ok) {
    const errorBody = await response.text()
    throw new Error(`API Error ${response.status}: ${errorBody}`)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export const apiClient = {
  GET: <T = unknown>(path: string, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'GET' }),

  POST: <T = unknown>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    }),

  PUT: <T = unknown>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, {
      ...options,
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    }),

  PATCH: <T = unknown>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, {
      ...options,
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    }),

  DELETE: <T = unknown>(path: string, options?: RequestOptions) =>
    request<T>(path, { ...options, method: 'DELETE' }),
}
