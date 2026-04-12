import client from './client';

// client 的 baseURL 是 /api，这里的 /auth/* 最终会请求到 /api/auth/*。

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
  return client.post<TokenResponse, TokenResponse>('/auth/login', params);
}

/**
 * 用户注册
 */
export function register(params: RegisterParams) {
  return client.post<void, void>('/auth/register', params);
}

/**
 * 刷新 Token
 */
export function refreshToken() {
  return client.post<TokenResponse, TokenResponse>('/auth/refresh');
}

