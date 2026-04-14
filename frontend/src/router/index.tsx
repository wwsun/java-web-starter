import { createHashRouter } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';
import LoginPage from '@/pages/LoginPage';
import DashboardPage from '@/pages/DashboardPage';
import UserListPage from '@/pages/UserListPage';
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
      {
        path: 'users',
        element: (
          <RequireAuth requiredRole="ADMIN">
            <UserListPage />
          </RequireAuth>
        ),
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
