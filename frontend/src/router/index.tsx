import { createHashRouter } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';
import LoginPage from '@/pages/LoginPage';
import DashboardPage from '@/pages/DashboardPage';
import NotFoundPage from '@/pages/NotFoundPage';
import ForbiddenPage from '@/pages/ForbiddenPage';
import RequireAuth from '@/router/RequireAuth';

export const router = createHashRouter([
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
