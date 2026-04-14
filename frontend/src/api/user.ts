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

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/**
 * 获取当前登录用户信息（含角色）
 */
export function getMe() {
  return client.get<UserVO, UserVO>('/users/me');
}

/**
 * 分页查询用户列表（仅 ADMIN）
 */
export function getUsers(page = 1, size = 10) {
  return client.get<PageResult<UserVO>, PageResult<UserVO>>('/users', {
    params: { page, size },
  });
}

/**
 * 删除用户（仅 ADMIN）
 */
export function deleteUserById(id: number) {
  return client.delete<void, void>(`/users/${id}`);
}
