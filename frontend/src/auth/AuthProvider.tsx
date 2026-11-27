import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { onSessionExpired, refreshAccessToken } from '../api/client'
import type { User } from '../types'
import { AuthContext, type AuthStatus } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')

  useEffect(() => {
    let cancelled = false

    // When the session cannot be refreshed anymore, the user is logged out everywhere
    onSessionExpired(() => {
      setUser(null)
      setStatus('anonymous')
    })

    // After a page reload, restore the session with the HttpOnly refresh cookie
    refreshAccessToken()
      .then((token) => (token ? authApi.fetchMe() : null))
      .then((me) => {
        if (cancelled) return
        setUser(me)
        setStatus(me ? 'authenticated' : 'anonymous')
      })
      .catch(() => {
        if (cancelled) return
        setUser(null)
        setStatus('anonymous')
      })

    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    await authApi.login(email, password)
    const me = await authApi.fetchMe()
    setUser(me)
    setStatus('authenticated')
  }, [])

  const register = useCallback(
    async (email: string, password: string) => {
      await authApi.register(email, password)
      await login(email, password)
    },
    [login],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      setUser(null)
      setStatus('anonymous')
    }
  }, [])

  const value = useMemo(
    () => ({ user, status, login, register, logout }),
    [user, status, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
