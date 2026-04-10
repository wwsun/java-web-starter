import { createBrowserRouter, Navigate } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';
import LoginPage from '@/pages/LoginPage';
import DashboardPage from '@/pages/DashboardPage';
import NotFoundPage from '@/pages/NotFoundPage';
import ForbiddenPage from '@/pages/ForbiddenPage';
import { useAuthStore } from '@/stores/useAuthStore';
import type { ReactNode } from 'react';

/**
 * 路由守卫：未登录重定向到登录页；无角色权限重定向到 403 页
 */
function RequireAuth({
  children,
  requiredRole,
}: {
  children: ReactNode;
  requiredRole?: string;
}) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const roles = useAuthStore((s) => s.roles);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (requiredRole && !roles.includes(requiredRole)) {
    return <Navigate to="/403" replace />;
  }
  return <>{children}</>;
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/403',
    element: <ForbiddenPage />,
  },
  {
    path: '/',
    element: (
      <RequireAuth>
        <MainLayout />
      </RequireAuth>
    ),
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      // 在此添加更多业务页面路由
      // 示例：需要 ADMIN 权限的路由：
      // {
      //   path: 'admin/xxx',
      //   element: (
      //     <RequireAuth requiredRole="ADMIN">
      //       <XxxPage />
      //     </RequireAuth>
      //   ),
      // },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
