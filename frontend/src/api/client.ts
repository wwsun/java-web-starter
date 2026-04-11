import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { useAuthStore } from '@/stores/useAuthStore';

/**
 * Axios 实例
 * - 请求拦截器：自动注入 Access Token
 * - 响应拦截器：精简返回结构，401 时自动换牌
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
    // 从 Zustand Store 中获取 token (Store 内部处理了持久化)
    const token = useAuthStore.getState().token;
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
    // 如果 code 不存在或者不是 200，则判定为错误
    if (data.code !== undefined && data.code !== 200) {
      console.error(`[API Error] ${data.code}: ${data.message}`);
      return Promise.reject(new Error(data.message || '请求失败'));
    }
    // 直接返回业务 Payload (data.data)
    return data.data !== undefined ? data.data : data;
  },
  async (error) => {
    if (error.response?.status !== 401) {
      return Promise.reject(error);
    }

    const { refreshToken: rt, logout, setTokens } = useAuthStore.getState();
    if (!rt) {
      logout();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    // 已有刷新在进行中，将当前请求加入等待队列
    if (isRefreshing) {
      return new Promise<unknown>((resolve) => {
        pendingRequests.push((newToken: string) => {
          if (error.config.headers) {
            error.config.headers.Authorization = `Bearer ${newToken}`;
          }
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
      setTokens(accessToken, newRefreshToken);

      // 重试等待队列中的请求
      pendingRequests.forEach((cb) => cb(accessToken));
      pendingRequests = [];

      // 重试原请求
      if (error.config.headers) {
        error.config.headers.Authorization = `Bearer ${accessToken}`;
      }
      return client(error.config);
    } catch {
      logout();
      window.location.href = '/login';
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

export default client;

