// frontend/src/api/tokenRefresher.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '@/test-utils/server'
import client from './client'
import { useAuthStore } from '@/stores/useAuthStore'

// 替换 window.location，需保留 href 让 MSW XHR 拦截器能解析相对 URL
const mockReplace = vi.fn()
vi.stubGlobal('location', {
  href: 'http://localhost/',
  origin: 'http://localhost',
  replace: mockReplace,
})

describe('tokenRefresher - handleUnauthorized', () => {
  beforeEach(() => {
    mockReplace.mockClear()
    // 每个测试前重置 store 到已登录状态
    useAuthStore.setState({
      token: 'old-access-token',
      refreshToken: 'valid-refresh-token',
      isAuthenticated: true,
      roles: [],
    })
  })

  it('401 → 刷新成功 → 原请求使用新 token 重试并返回数据', async () => {
    let callCount = 0

    server.use(
      http.get('http://localhost/api/protected', () => {
        callCount++
        if (callCount === 1) return new HttpResponse(null, { status: 401 })
        return HttpResponse.json({ code: 200, message: 'ok', data: { value: 'success' } })
      }),
      http.post('http://localhost/api/auth/refresh', () =>
        HttpResponse.json({
          code: 200,
          message: 'ok',
          data: {
            accessToken: 'new-access-token',
            refreshToken: 'new-refresh-token',
            tokenType: 'Bearer',
            expiresIn: 3600,
          },
        })
      )
    )

    const result = await client.get('/protected')

    expect(result).toEqual({ value: 'success' })
    expect(callCount).toBe(2) // 原请求 + 重试
    expect(useAuthStore.getState().token).toBe('new-access-token')
    expect(useAuthStore.getState().refreshToken).toBe('new-refresh-token')
  })

  it('无 refreshToken → 不发起刷新，直接登出并重定向', async () => {
    useAuthStore.setState({
      token: null,
      refreshToken: null,
      isAuthenticated: false,
      roles: [],
    })

    server.use(
      http.get('http://localhost/api/protected', () => new HttpResponse(null, { status: 401 }))
    )

    await expect(client.get('/protected')).rejects.toThrow()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(mockReplace).toHaveBeenCalledWith('http://localhost/#/login')
  })

  it('refreshToken 有效但刷新接口返回失败 → 登出并重定向', async () => {
    server.use(
      http.get('http://localhost/api/protected', () => new HttpResponse(null, { status: 401 })),
      http.post('http://localhost/api/auth/refresh', () => new HttpResponse(null, { status: 500 }))
    )

    await expect(client.get('/protected')).rejects.toThrow()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(mockReplace).toHaveBeenCalledWith('http://localhost/#/login')
  })

  it('并发多个 401 → 只发起一次刷新请求，所有请求均重试成功', async () => {
    let refreshCount = 0

    server.use(
      http.get('http://localhost/api/resource-a', ({ request }) => {
        if (request.headers.get('Authorization') === 'Bearer old-access-token') {
          return new HttpResponse(null, { status: 401 })
        }
        return HttpResponse.json({ code: 200, message: 'ok', data: { from: 'a' } })
      }),
      http.get('http://localhost/api/resource-b', ({ request }) => {
        if (request.headers.get('Authorization') === 'Bearer old-access-token') {
          return new HttpResponse(null, { status: 401 })
        }
        return HttpResponse.json({ code: 200, message: 'ok', data: { from: 'b' } })
      }),
      http.post('http://localhost/api/auth/refresh', () => {
        refreshCount++
        return HttpResponse.json({
          code: 200,
          message: 'ok',
          data: {
            accessToken: 'new-access-token',
            refreshToken: 'new-refresh-token',
            tokenType: 'Bearer',
            expiresIn: 3600,
          },
        })
      })
    )

    const [resultA, resultB] = await Promise.all([
      client.get('/resource-a'),
      client.get('/resource-b'),
    ])

    expect(refreshCount).toBe(1) // 只刷新一次
    expect(resultA).toEqual({ from: 'a' })
    expect(resultB).toEqual({ from: 'b' })
  })
})
