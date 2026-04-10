import client from './client';

export interface UserVO {
  id: number;
  username: string;
  nickname: string | null;
  email: string | null;
  phone: string | null;
  avatar: string | null;
  status: number;
  roles: string[];
}

/**
 * 获取当前登录用户信息（含角色）
 */
export function getMe() {
  return client.get<unknown, { data: UserVO }>('/users/me');
}
