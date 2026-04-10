// frontend/src/stores/useAuthStore.test.ts
import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore } from './useAuthStore'

describe('useAuthStore', () => {
  beforeEach(() => {
    localStorage.clear()
    // 重置 store 状态
    useAuthStore.setState({
      token: null,
      refreshToken: null,
      isAuthenticated: false,
      roles: [],
    })
  })

  it('初始状态：localStorage 为空时未认证', () => {
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().token).toBeNull()
    expect(useAuthStore.getState().roles).toEqual([])
  })

  it('setTokens：存储 token 并标记为已认证', () => {
    useAuthStore.getState().setTokens('access-abc', 'refresh-xyz')

    expect(useAuthStore.getState().isAuthenticated).toBe(true)
    expect(useAuthStore.getState().token).toBe('access-abc')
    expect(localStorage.getItem('access_token')).toBe('access-abc')
    expect(localStorage.getItem('refresh_token')).toBe('refresh-xyz')
  })

  it('logout：清除 token 和 roles，标记为未认证', () => {
    useAuthStore.getState().setTokens('access-abc', 'refresh-xyz')
    useAuthStore.getState().setRoles(['ADMIN'])
    useAuthStore.getState().logout()

    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().token).toBeNull()
    expect(useAuthStore.getState().roles).toEqual([])
    expect(localStorage.getItem('access_token')).toBeNull()
  })

  it('setRoles：更新 roles 列表', () => {
    useAuthStore.getState().setRoles(['ADMIN', 'USER'])
    expect(useAuthStore.getState().roles).toEqual(['ADMIN', 'USER'])
  })
})
