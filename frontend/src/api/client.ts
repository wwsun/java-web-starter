import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { useAuthStore } from '@/stores/useAuthStore';

/**
 * Axios 实例
 * - 请求拦截器：自动注入 Access Token
 * - 响应拦截器：401 时先尝试用 Refresh Token 续期，续期失败才跳转登录
 */
const client: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// 用于 token 刷新的裸 axios（不带业务拦截器，避免循环）
const bareAxios = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// 刷新状态标志与等待队列
let isRefreshing = false;
let pendingRequests: Array<(token: string) => void> = [];

// ==================== 请求拦截器 ====================
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ==================== 响应拦截器 ====================
client.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response;
    if (data.code !== undefined && data.code !== 200) {
      console.error(`[API Error] ${data.code}: ${data.message}`);
      return Promise.reject(new Error(data.message || '请求失败'));
    }
    return data;
  },
  async (error) => {
    if (error.response?.status !== 401) {
      return Promise.reject(error);
    }

    const rt = localStorage.getItem('refresh_token');
    if (!rt) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    // 已有刷新在进行中，将当前请求加入等待队列
    if (isRefreshing) {
      return new Promise<unknown>((resolve) => {
        pendingRequests.push((newToken: string) => {
          error.config.headers.Authorization = `Bearer ${newToken}`;
          resolve(client(error.config));
        });
      });
    }

    isRefreshing = true;

    try {
      const res = await bareAxios.post('/auth/refresh', null, {
        headers: { Authorization: `Bearer ${rt}` },
      });
      const { accessToken, refreshToken: newRefreshToken } = res.data.data;
      useAuthStore.getState().setTokens(accessToken, newRefreshToken);

      // 重试等待队列中的请求
      pendingRequests.forEach((cb) => cb(accessToken));
      pendingRequests = [];

      // 重试原请求
      error.config.headers.Authorization = `Bearer ${accessToken}`;
      return client(error.config);
    } catch {
      useAuthStore.getState().logout();
      window.location.href = '/login';
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

export default client;
