// frontend/src/pages/UserListPage.test.tsx
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import UserListPage from './UserListPage'
import { useAuthStore } from '@/stores/useAuthStore'
import * as userApi from '@/api/user'
import type { PageResult, UserVO } from '@/api/user'

vi.mock('@/api/user', async (importOriginal) => {
  const actual = await importOriginal<typeof userApi>()
  return {
    ...actual,
    getUsers: vi.fn(),
    deleteUserById: vi.fn(),
  }
})

const mockUsers: PageResult<UserVO> = {
  records: [
    { id: 1, username: 'admin', nickname: '管理员', email: 'admin@example.com', phone: null, avatar: null, status: 1, roles: ['ADMIN'] },
    { id: 2, username: 'user1', nickname: '用户1', email: 'user1@example.com', phone: null, avatar: null, status: 1, roles: [] },
  ],
  total: 2,
  size: 10,
  current: 1,
  pages: 1,
}

function renderPage() {
  return render(
    <MemoryRouter>
      <UserListPage />
    </MemoryRouter>
  )
}

describe('UserListPage', () => {
  beforeEach(() => {
    vi.mocked(userApi.getUsers).mockResolvedValue(mockUsers)
    vi.mocked(userApi.deleteUserById).mockResolvedValue(undefined)
    useAuthStore.setState({ token: 'tok', isAuthenticated: true, roles: ['ADMIN'] })
  })

  it('先显示 Loading，加载后渲染用户列表', async () => {
    renderPage()
    expect(screen.getByText('Loading...')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('user1')).toBeInTheDocument()
    })
  })

  it('管理员可见删除按钮，非管理员不可见', async () => {
    const { unmount } = renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())
    expect(screen.getAllByRole('button', { name: /删除/ })).toHaveLength(2)
    unmount()

    useAuthStore.setState({ roles: [] })
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: /删除/ })).not.toBeInTheDocument()
  })

  it('点击删除 → 确认弹窗 → 调用 deleteUserById', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()
    await waitFor(() => expect(screen.getByText('admin')).toBeInTheDocument())

    const [firstDeleteBtn] = screen.getAllByRole('button', { name: /删除/ })
    await user.click(firstDeleteBtn)

    expect(window.confirm).toHaveBeenCalled()
    expect(userApi.deleteUserById).toHaveBeenCalledWith(1)
  })

  it('API 失败时显示错误信息', async () => {
    vi.mocked(userApi.getUsers).mockRejectedValue(new Error('服务不可用'))
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('服务不可用')).toBeInTheDocument()
    })
  })
})
