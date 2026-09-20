import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    captcha?: string;
    password?: string;
    username?: string;
  }

  /** 登录接口返回值（Session-Cookie 方案下为占位符，会话由 JSESSIONID 维持） */
  export interface LoginResult {
    accessToken: string;
  }

  export interface RefreshTokenResult {
    data: string;
    status: number;
  }
}

/**
 * 登录（对接 POST /api/auth/login，DataVo.data 返回 { accessToken }）
 * 后端老接口一律读表单参数（@RequestParam），POST 统一走 form-urlencoded；
 * vben 的 axios 实例默认 Content-Type 是 application/json，会覆盖 URLSearchParams
 * 的自动检测，必须显式覆盖（见手册 §5 坑 14）
 */
export async function loginApi(data: AuthApi.LoginParams) {
  const form = new URLSearchParams();
  if (data.username) form.append('admin_name', data.username);
  if (data.password) form.append('password', data.password);
  if (data.captcha) form.append('captcha', data.captcha);
  return requestClient.post<AuthApi.LoginResult>('/auth/login', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' },
  });
}

/**
 * 刷新accessToken —— Session-Cookie 方案无刷新概念；
 * enableRefreshToken 默认 false，此函数不会被调用，保留以兼容 request.ts
 */
export async function refreshTokenApi() {
  return baseRequestClient.post<AuthApi.RefreshTokenResult>(
    '/auth/refresh',
    undefined,
    {
      withCredentials: true,
    },
  );
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return baseRequestClient.post('/auth/logout', undefined, {
    withCredentials: true,
  });
}

/**
 * 获取用户权限码（后端 /api/auth/codes，返回 action_key 列表）
 */
export async function getAccessCodesApi() {
  return requestClient.get<string[]>('/auth/codes');
}
