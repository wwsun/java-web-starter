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

  it('初始状态：未认证', () => {
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().token).toBeNull()
    expect(useAuthStore.getState().roles).toEqual([])
  })

  it('setTokens：存储 token 并标记为已认证', () => {
    useAuthStore.getState().setTokens('access-abc', 'refresh-xyz')

    expect(useAuthStore.getState().isAuthenticated).toBe(true)
    expect(useAuthStore.getState().token).toBe('access-abc')
    expect(useAuthStore.getState().refreshToken).toBe('refresh-xyz')
    
    // 验证持久化到了 localStorage (通过 persist 中间件)
    const storedValue = JSON.parse(localStorage.getItem('auth-storage') || '{}')
    expect(storedValue.state.token).toBe('access-abc')
  })

  it('logout：清除 token 和 roles，标记为未认证', () => {
    useAuthStore.getState().setTokens('access-abc', 'refresh-xyz')
    useAuthStore.getState().setRoles(['ADMIN'])
    useAuthStore.getState().logout()

    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().token).toBeNull()
    expect(useAuthStore.getState().roles).toEqual([])
    
    const storedValue = JSON.parse(localStorage.getItem('auth-storage') || '{}')
    expect(storedValue.state.token).toBeNull()
  })

  it('setRoles：更新 roles 列表', () => {
    useAuthStore.getState().setRoles(['ADMIN', 'USER'])
    expect(useAuthStore.getState().roles).toEqual(['ADMIN', 'USER'])
  })
})

