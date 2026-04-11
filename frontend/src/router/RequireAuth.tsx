import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/useAuthStore'

interface RequireAuthProps {
  children: ReactNode
  requiredRole?: string
}

export default function RequireAuth({ children, requiredRole }: RequireAuthProps) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const roles = useAuthStore((state) => state.roles)

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (requiredRole && !roles.includes(requiredRole)) {
    return <Navigate to="/403" replace />
  }

  return <>{children}</>
}
