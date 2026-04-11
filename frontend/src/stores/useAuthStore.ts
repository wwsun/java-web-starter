import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  roles: string[];
  setTokens: (accessToken: string, refreshToken: string) => void;
  setRoles: (roles: string[]) => void;
  logout: () => void;
}

/**
 * 认证状态管理 (Zustand)
 * 使用 persist 中间件自动同步到 localStorage
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      isAuthenticated: false,
      roles: [],

      setTokens: (accessToken, refreshToken) => {
        set({
          token: accessToken,
          refreshToken,
          isAuthenticated: true,
        });
      },

      setRoles: (roles) => {
        set({ roles });
      },

      logout: () => {
        set({
          token: null,
          refreshToken: null,
          isAuthenticated: false,
          roles: [],
        });
      },
    }),
    {
      name: 'auth-storage', // localStorage 中的 key
    }
  )
);

