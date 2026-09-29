import { requestClient } from '#/api/request';

/**
 * G22 AI 内容助手 API（/api/system/ai/**）
 * 红线：AI 输出仅回填表单由人工审核后保存，服务端不直接改写已发布内容。
 */

export async function getAiStatusApi() {
  return requestClient.get<{ configured: boolean }>('/system/ai/status');
}

export async function aiGenerateApi(data: {
  content?: string;
  targetLang?: string;
  task: 'keywords' | 'summary' | 'title' | 'translate';
  title?: string;
}) {
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(data)) {
    if (v !== undefined && v !== null && v !== '') form.append(k, String(v));
  }
  return requestClient.post<string>('/system/ai/generate', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' },
  });
}
