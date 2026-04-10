import { useAuthStore } from '@/stores/useAuthStore';

/**
 * 检查当前用户是否持有指定角色
 * 用法：const isAdmin = usePermission('ADMIN');
 */
export function usePermission(role: string): boolean {
  const roles = useAuthStore((s) => s.roles);
  return roles.includes(role);
}
