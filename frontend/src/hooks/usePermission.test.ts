// frontend/src/hooks/usePermission.test.ts
import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { usePermission } from './usePermission'
import { useAuthStore } from '@/stores/useAuthStore'

describe('usePermission', () => {
  beforeEach(() => {
    useAuthStore.setState({ roles: [], isAuthenticated: false })
  })

  it('持有该角色时返回 true', () => {
    useAuthStore.setState({ roles: ['ADMIN', 'USER'] })

    const { result } = renderHook(() => usePermission('ADMIN'))

    expect(result.current).toBe(true)
  })

  it('不持有该角色时返回 false', () => {
    useAuthStore.setState({ roles: ['USER'] })

    const { result } = renderHook(() => usePermission('ADMIN'))

    expect(result.current).toBe(false)
  })

  it('未登录（roles 为空）时返回 false', () => {
    const { result } = renderHook(() => usePermission('USER'))

    expect(result.current).toBe(false)
  })
})
