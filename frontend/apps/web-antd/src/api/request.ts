/**
 * 该文件可自行根据业务逻辑进行调整
 */
import type { RequestClientOptions } from '@vben/request';

import { useAppConfig } from '@vben/hooks';
import { preferences } from '@vben/preferences';
import {
  authenticateResponseInterceptor,
  defaultResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';
import { useAccessStore } from '@vben/stores';

import { message } from 'ant-design-vue';

import { useAuthStore } from '#/store';

import { refreshTokenApi } from './core';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

/**
 * CSRF 双提交（double-submit cookie）客户端实现。
 *
 * 后端 CsrfFilter 对 `/api/**` 的非安全方法（POST/PUT/PATCH/DELETE）校验
 * 请求头 `X-XSRF-TOKEN` 必须与 cookie `XSRF-TOKEN` 一致；该 cookie 由任意
 * 安全请求（GET）自动签发且非 HttpOnly，此处读出来回填请求头即可。
 *
 * 为什么必须显式回填：cookie 由浏览器自动携带（同源请求默认带 cookie），
 * 但请求头只能由 JS 设置——这正是「攻击者无法跨站读取 cookie、因而无法伪造请求头」
 * 的防护原理。缺了这段，生产开启 CSRF 强制校验后所有写操作都会 403。
 */
function readXsrfToken(): null | string {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match?.[1] ? decodeURIComponent(match[1]) : null;
}

function appendCsrfHeader(config: any) {
  const token = readXsrfToken();
  if (token) {
    config.headers['X-XSRF-TOKEN'] = token;
  }
  return config;
}

function createRequestClient(baseURL: string, options?: RequestClientOptions) {
  const client = new RequestClient({
    ...options,
    baseURL,
  });

  /**
   * 重新认证逻辑
   */
  async function doReAuthenticate() {
    console.warn('Access token or refresh token is invalid or expired. ');
    const accessStore = useAccessStore();
    const authStore = useAuthStore();
    accessStore.setAccessToken(null);
    if (
      preferences.app.loginExpiredMode === 'modal' &&
      accessStore.isAccessChecked
    ) {
      accessStore.setLoginExpired(true);
    } else {
      await authStore.logout();
    }
  }

  /**
   * 刷新token逻辑
   */
  async function doRefreshToken() {
    const accessStore = useAccessStore();
    const resp = await refreshTokenApi();
    const newToken = resp.data;
    accessStore.setAccessToken(newToken);
    return newToken;
  }

  function formatToken(token: null | string) {
    return token ? `Bearer ${token}` : null;
  }

  // 请求头处理
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const accessStore = useAccessStore();

      config.headers.Authorization = formatToken(accessStore.accessToken);
      config.headers['Accept-Language'] = preferences.app.locale;
      // CSRF 双提交：回填 X-XSRF-TOKEN
      appendCsrfHeader(config);
      return config;
    },
  });

  // 处理返回的响应数据格式
  client.addResponseInterceptor(
    defaultResponseInterceptor({
      codeField: 'code',
      dataField: 'data',
      successCode: 0,
    }),
  );

  // token过期的处理
  client.addResponseInterceptor(
    authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken,
      enableRefreshToken: preferences.app.enableRefreshToken,
      formatToken,
    }),
  );

  // 通用的错误处理,如果没有进入上面的错误处理逻辑，就会进入这里
  client.addResponseInterceptor(
    errorMessageResponseInterceptor((msg: string, error) => {
      // 这里可以根据业务进行定制,你可以拿到 error 内的信息进行定制化处理，根据不同的 code 做不同的提示，而不是直接使用 message.error 提示 msg
      // 当前mock接口返回的错误字段是 error 或者 message
      const responseData = error?.response?.data ?? {};
      const errorMessage = responseData?.error ?? responseData?.message ?? '';
      // 如果没有错误信息，则会根据状态码进行提示
      message.error(errorMessage || msg);
    }),
  );

  return client;
}

export const requestClient = createRequestClient(apiURL, {
  responseReturn: 'data',
});

// baseRequestClient 无 token 刷新等拦截器，但 /auth/refresh 与 /auth/logout 同为
// /api/** 的 POST，同样会被 CSRF 校验拦住，故单独补一个回填请求头的拦截器。
export const baseRequestClient = new RequestClient({ baseURL: apiURL });
baseRequestClient.addRequestInterceptor({ fulfilled: appendCsrfHeader });
