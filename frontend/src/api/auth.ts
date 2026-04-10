import client from './client';

export interface LoginParams {
  username: string;
  password: string;
}

export interface RegisterParams {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

/**
 * 用户登录
 */
export function login(params: LoginParams) {
  return client.post<unknown, { data: TokenResponse }>('/auth/login', params);
}

/**
 * 用户注册
 */
export function register(params: RegisterParams) {
  return client.post('/auth/register', params);
}

/**
 * 刷新 Token
 */
export function refreshToken() {
  return client.post<unknown, { data: TokenResponse }>('/auth/refresh');
}
