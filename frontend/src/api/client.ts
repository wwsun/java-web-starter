import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { useAuthStore } from '@/stores/useAuthStore';
import { handleUnauthorized } from './tokenRefresher';

/**
 * Axios 实例
 * - 请求拦截器：自动注入 Access Token
 * - 响应拦截器：精简返回结构，401 时自动换牌（由 tokenRefresher 处理）
 */
const client: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// ==================== 请求拦截器 ====================
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 从 Zustand Store 中获取 token (Store 内部处理了持久化)
    const token = useAuthStore.getState().token;
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // 注入请求关联 ID，便于前后端日志链路追踪
    if (config.headers) {
      config.headers['X-Request-Id'] = crypto.randomUUID();
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
    if (error.response?.status === 401) {
      return handleUnauthorized(error.config, client);
    }
    return Promise.reject(error);
  }
);

export default client;
