import { requestClient } from '#/api/request';

/**
 * 短信设置 API（五厂商：阿里云/腾讯云/华为云/百度云/火山引擎）
 */

export interface SmsProvider {
  key: string;
  label: string;
}

export interface SmsProviderConfig {
  /** 访问密钥 ID（阿里AccessKeyID/腾讯SecretID/华为APP_Key/百度INVOKE_ID/火山AccessKeyID） */
  ak: string;
  /** 访问密钥 Secret（掩码 ****** 回显，提交掩码/空则不更新） */
  sk: string;
  /** 短信签名 */
  sign: string;
  /** 应用/通道标识：腾讯SmsSdkAppId/华为短信通道号/火山SmsAccount */
  account: string;
  /** 注册验证码模板 ID */
  tpl_reg: string;
  /** 绑定/安全手机验证码模板 ID */
  tpl_safe: string;
  /** 找回密码验证码模板 ID */
  tpl_reset: string;
}

export interface SmsConfig {
  provider: string;
  providers: SmsProvider[];
  config: Record<string, SmsProviderConfig>;
}

function postForm<T>(url: string, data: Record<string, unknown>) {
  const form = new URLSearchParams();
  for (const [key, value] of Object.entries(data)) {
    if (value !== undefined && value !== null && value !== '') {
      form.append(key, String(value));
    }
  }
  return requestClient.post<T>(url, form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

export async function getSmsConfigApi() {
  return requestClient.get<SmsConfig>('/system/sms/config');
}

export async function saveSmsConfigApi(data: Record<string, string>) {
  return postForm<void>('/system/sms/save', data);
}

/** 用当前厂商的注册验证码模板发送测试短信 */
export async function testSmsApi(phone: string) {
  return postForm<void>('/system/sms/test', { phone });
}
