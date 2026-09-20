import { requestClient } from '#/api/request';

/**
 * 网站管理 + 前台模板管理 API
 */

export interface TemplateFile {
  file: string;
  lastModified: number;
  size: number;
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

// /////////////////// 网站管理 ///////////////////

export interface WebsiteConfig {
  config: Record<string, string>;
  skins: string[];
}

export async function getWebsiteConfigApi() {
  return requestClient.get<WebsiteConfig>('/system/website/config');
}

export async function saveWebsiteConfigApi(data: Record<string, string>) {
  return postForm<void>('/system/website/save', data);
}

// /////////////////// 模板管理 ///////////////////

export async function getTemplateFilesApi() {
  return requestClient.get<{ files: TemplateFile[]; skin: string }>(
    '/system/template/files',
  );
}

export async function readTemplateApi(file: string) {
  return requestClient.get<{ content: string; file: string }>(
    '/system/template/read',
    { params: { file } },
  );
}

export async function saveTemplateApi(file: string, content: string) {
  return postForm<void>('/system/template/save', { content, file });
}

export async function createTemplateApi(file: string) {
  return postForm<void>('/system/template/create', { file });
}

export async function deleteTemplateApi(file: string) {
  return postForm<void>('/system/template/delete', { file });
}
