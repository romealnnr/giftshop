import type { TokenResponse, User } from '../types'
import { apiFetch, setAccessToken } from './client'

export function register(email: string, password: string): Promise<User> {
  return apiFetch<User>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

/** Logs in: keeps the access token in memory; the refresh token arrives as an HttpOnly cookie. */
export async function login(email: string, password: string): Promise<void> {
  const tokens = await apiFetch<TokenResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
  setAccessToken(tokens.accessToken)
}

/** Ends the session on the server (the cookie is cleared) and forgets the access token. */
export async function logout(): Promise<void> {
  try {
    await apiFetch<void>('/api/auth/logout', { method: 'POST' })
  } finally {
    setAccessToken(null)
  }
}

export function fetchMe(): Promise<User> {
  return apiFetch<User>('/api/me')
}
