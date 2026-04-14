import { http, HttpResponse } from 'msw';

/**
 * MSW 请求处理器 - 预置 Mock 数据
 * 用于前端独立开发调试
 */
export const handlers = [
  // 登录
  http.post('/api/auth/login', async ({ request }) => {
    const body = (await request.json()) as { username: string; password: string };

    if (body.username === 'admin' && body.password === 'admin123') {
      return HttpResponse.json({
        code: 200,
        message: '操作成功',
        data: {
          accessToken: 'mock-access-token-xxx',
          refreshToken: 'mock-refresh-token-xxx',
          tokenType: 'Bearer',
          expiresIn: 3600,
        },
      });
    }

    return HttpResponse.json(
      { code: 1003, message: '用户名或密码错误', data: null },
      { status: 200 }
    );
  }),

  // 注册
  http.post('/api/auth/register', () => {
    return HttpResponse.json({
      code: 200,
      message: '操作成功',
      data: null,
    });
  }),

  // 用户列表
  http.get('/api/users', () => {
    return HttpResponse.json({
      code: 200,
      message: '操作成功',
      data: {
        records: [
          { id: 1, username: 'admin', nickname: '管理员', email: 'admin@example.com', status: 1 },
          { id: 2, username: 'user1', nickname: '用户1', email: 'user1@example.com', status: 1 },
        ],
        total: 2,
        size: 10,
        current: 1,
      },
    });
  }),

  // 当前用户信息（含角色）
  http.get('/api/users/me', () => {
    return HttpResponse.json({
      code: 200,
      message: '操作成功',
      data: {
        id: 1,
        username: 'admin',
        nickname: '管理员',
        email: 'admin@example.com',
        phone: null,
        avatar: null,
        status: 1,
        roles: ['ADMIN'],
      },
    });
  }),

  // 删除用户（仅示意，Mock 直接返回成功）
  http.delete('/api/users/:id', () => {
    return HttpResponse.json({ code: 200, message: '操作成功', data: null });
  }),
];
