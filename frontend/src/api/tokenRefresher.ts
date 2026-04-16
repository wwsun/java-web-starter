import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/useAuthStore';

// 用于刷新请求的裸 axios 实例（不带业务拦截器，避免 401 循环）
const bareAxios = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// 刷新状态标志与等待队列（模块内部状态）
let isRefreshing = false;
let pendingRequests: Array<(token: string) => void> = [];

function processQueue(newToken: string) {
  pendingRequests.forEach((cb) => cb(newToken));
  pendingRequests = [];
}

function redirectToLogin() {
  window.location.replace(`${window.location.origin}/#/login`);
}

/**
 * 处理 401 响应：自动刷新 token 并重试原请求
 *
 * - 若正在刷新中，将原请求加入等待队列，刷新完成后统一重试
 * - 若无 refreshToken，直接登出并跳转登录页
 * - 若刷新失败，登出并跳转登录页
 */
export async function handleUnauthorized(
  failedConfig: InternalAxiosRequestConfig,
  retryClient: AxiosInstance
): Promise<unknown> {
  const { refreshToken: rt, logout, setTokens } = useAuthStore.getState();

  if (!rt) {
    logout();
    redirectToLogin();
    return Promise.reject(new Error('无 refreshToken，已登出'));
  }

  // 已有刷新在进行中，将当前请求加入等待队列
  if (isRefreshing) {
    return new Promise<unknown>((resolve) => {
      pendingRequests.push((newToken: string) => {
        if (failedConfig.headers) {
          failedConfig.headers.Authorization = `Bearer ${newToken}`;
        }
        resolve(retryClient(failedConfig));
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

    // 通知并重试队列中的请求
    processQueue(accessToken);

    // 重试原请求
    if (failedConfig.headers) {
      failedConfig.headers.Authorization = `Bearer ${accessToken}`;
    }
    return retryClient(failedConfig);
  } catch {
    logout();
    redirectToLogin();
    return Promise.reject(new Error('Token 刷新失败，已登出'));
  } finally {
    isRefreshing = false;
  }
}
