import type { TokenResponse } from '../types'

/** An error answered by the API, with its HTTP status and its message (RFC 9457 "detail"). */
export class ApiError extends Error {
  readonly status: number
  /** For 429 Too Many Requests: how many seconds to wait, from the Retry-After header. */
  readonly retryAfterSeconds: number | null // NEW

  constructor(status: number, message: string, retryAfterSeconds: number | null = null) {
    super(message)
    this.status = status
    this.retryAfterSeconds = retryAfterSeconds
  }
}

/**
 * The access token lives only in memory: never in localStorage, where any injected
 * script could read it. After a page reload, it is restored with the refresh cookie.
 */
let accessToken: string | null = null

let sessionExpiredHandler: () => void = () => {}

let refreshInFlight: Promise<string | null> | null = null

export function setAccessToken(token: string | null): void {
  accessToken = token
}

/** Called when the session cannot be refreshed anymore (the user must log in again). */
export function onSessionExpired(handler: () => void): void {
  sessionExpiredHandler = handler
}

/**
 * Asks for a new access token, using the HttpOnly refresh cookie.
 *
 * Single flight: if a refresh is already running, callers share its result instead of
 * sending a second request. The backend revokes the whole session when a refresh token
 * is used twice (theft detection), so two concurrent refreshes would log the user out.
 */
export function refreshAccessToken(): Promise<string | null> {
  if (!refreshInFlight) {
    refreshInFlight = fetch('/api/auth/refresh', { method: 'POST', credentials: 'same-origin' })
      .then(async (response) => {
        if (!response.ok) {
          accessToken = null
          return null
        }
        const body = (await response.json()) as TokenResponse
        accessToken = body.accessToken
        return accessToken
      })
      .catch(() => {
        accessToken = null
        return null
      })
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight
}

/**
 * Calls the API: adds the access token, and if it has expired (401), refreshes it
 * once and replays the request.
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response = await send(path, init)

  const canRefresh = !path.startsWith('/api/auth/')
  if (response.status === 401 && canRefresh && accessToken !== null) {
    const newToken = await refreshAccessToken()
    if (newToken === null) {
      sessionExpiredHandler()
    } else {
      response = await send(path, init)
    }
  }

  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

function send(path: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers)
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  return fetch(path, { ...init, headers, credentials: 'same-origin' })
}

async function toApiError(response: Response): Promise<ApiError> {
  let message = `Request failed (HTTP ${response.status})`
  try {
    const problem = (await response.json()) as { detail?: string }
    if (problem.detail) {
      message = problem.detail
    }
  } catch {
    // The body is not JSON: keep the generic message
  }
  return new ApiError(response.status, message, parseRetryAfter(response)) // NEW
}

/** Retry-After in seconds ("42"); the HTTP-date form is not used by our backend. */
function parseRetryAfter(response: Response): number | null {
  const seconds = Number(response.headers.get('Retry-After'))
  return Number.isInteger(seconds) && seconds > 0 ? seconds : null
}
