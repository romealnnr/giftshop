import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from './useAuth'

/**
 * Shows its children only to logged-in users; others are sent to the login page,
 * which brings them back here afterwards.
 *
 * This is a convenience, not a security barrier: the backend rejects requests
 * without a valid token, whatever the frontend shows.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <p>Chargement…</p>
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}
