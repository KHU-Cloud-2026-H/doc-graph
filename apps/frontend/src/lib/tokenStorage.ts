/**
 * 토큰 저장소
 *
 * JWT 토큰을 저장/조회/삭제한다.
 * 현재는 localStorage 사용.
 * 백엔드 답변(쿠키 방식 등)에 따라 이 파일만 수정하면 다른 코드는 영향 없음.
 */

const TOKEN_KEY = 'docgraph_token'

export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY)
}

export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token)
}

export const clearToken = (): void => {
  localStorage.removeItem(TOKEN_KEY)
}

export const hasToken = (): boolean => {
  return getToken() !== null
}