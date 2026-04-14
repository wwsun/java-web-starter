import { useEffect, useState } from 'react'
import { Trash2, ChevronLeft, ChevronRight } from 'lucide-react'
import { usePermission } from '@/hooks/usePermission'
import { getUsers, deleteUserById } from '@/api/user'
import type { UserVO, PageResult } from '@/api/user'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

const PAGE_SIZE = 10

export default function UserListPage() {
  const isAdmin = usePermission('ADMIN')
  const [data, setData] = useState<PageResult<UserVO> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(1)

  const load = (p: number) => {
    setLoading(true)
    setError(null)
    getUsers(p, PAGE_SIZE)
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load(page)
  }, [page])

  const handleDelete = (id: number) => {
    if (!confirm('确认删除该用户？')) return
    deleteUserById(id)
      .then(() => load(page))
      .catch((e: Error) => setError(e.message))
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">User Management</h1>
        <p className="text-muted-foreground text-sm font-medium mt-1">
          Manage system users and role assignments.
        </p>
      </div>

      <Card className="border-border shadow-none">
        <CardContent className="p-0">
          {loading && (
            <div className="p-8 text-center text-muted-foreground text-sm">Loading...</div>
          )}
          {error && (
            <div className="p-8 text-center text-destructive text-sm">{error}</div>
          )}
          {!loading && !error && data?.records.length === 0 && (
            <div className="p-8 text-center text-muted-foreground text-sm">No users found.</div>
          )}
          {!loading && !error && data && data.records.length > 0 && (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  {['Username', 'Nickname', 'Email', 'Status'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left font-mono text-xs font-bold uppercase tracking-widest text-muted-foreground/60">
                      {h}
                    </th>
                  ))}
                  {isAdmin && <th className="px-4 py-3" />}
                </tr>
              </thead>
              <tbody>
                {data.records.map((user) => (
                  <tr key={user.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-4 py-3 font-medium text-foreground">{user.username}</td>
                    <td className="px-4 py-3 text-muted-foreground">{user.nickname ?? '—'}</td>
                    <td className="px-4 py-3 text-muted-foreground">{user.email ?? '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${
                        user.status === 1 ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
                      }`}>
                        {user.status === 1 ? 'ACTIVE' : 'DISABLED'}
                      </span>
                    </td>
                    {isAdmin && (
                      <td className="px-4 py-3 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label={`删除 ${user.username}`}
                          className="h-7 w-7 p-0 text-destructive hover:text-destructive hover:bg-destructive/10"
                          onClick={() => handleDelete(user.id)}
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </Button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      {data && data.pages > 1 && (
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>Total {data.total} users</span>
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              className="h-7 w-7 p-0"
              disabled={page <= 1}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft className="w-4 h-4" />
            </Button>
            <span className="px-2 font-mono">
              Page {data.current} / {data.pages}
            </span>
            <Button
              variant="ghost"
              size="sm"
              className="h-7 w-7 p-0"
              disabled={page >= data.pages}
              onClick={() => setPage((p) => p + 1)}
            >
              <ChevronRight className="w-4 h-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
