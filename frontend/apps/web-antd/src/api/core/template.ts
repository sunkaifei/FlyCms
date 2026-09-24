import { requestClient } from '#/api/request';

/**
 * 模板中心 API（阶段 D）
 *
 * 与 website.ts 的分工：website.ts 只管网站配置，模板相关的版本/皮肤/手册在本文件。
 * 在线编辑的 files/read/save/create/delete 仍走 website.ts（路径不变），
 * 但后端 save 已升级为「语法校验 + 版本快照 + 即时生效」。
 */

export interface TemplateVersionRow {
  createTime: string;
  editorId: string;
  id: string;
  remark: string;
  version: number;
}

export interface TemplateVersionResult {
  templateId: string;
  total: number;
  versions: TemplateVersionRow[];
}

export interface TagParam {
  desc: string;
  name: string;
  required: boolean;
}

export interface TagManualRow {
  group: string;
  label: string;
  name: string;
  output: string;
  params: TagParam[];
  snippet: string;
  usage: string;
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

// /////////////////// D1 版本历史与回滚 ///////////////////

export async function getTemplateVersionsApi(file: string, skin?: string) {
  return requestClient.get<TemplateVersionResult>('/system/template/versions', {
    params: { file, skin },
  });
}

export async function restoreTemplateApi(file: string, version: number) {
  return postForm<void>('/system/template/restore', { file, version });
}

// /////////////////// D8 语法校验与试渲染 ///////////////////

export async function checkTemplateApi(file: string, content: string) {
  return postForm<{ code: number; msg: string }>('/system/template/check', {
    content,
    file,
  });
}

export async function previewTemplateApi(
  content: string,
  file: string,
  model?: string,
) {
  return postForm<{ code: number; data?: { html: string }; msg: string }>(
    '/system/template/preview',
    { content, file, model },
  );
}

// /////////////////// D2/D3 皮肤管理 ///////////////////

export async function getSkinListApi() {
  return requestClient.get<{ current: string; skins: string[] }>(
    '/system/skin/list',
  );
}

export async function createSkinApi(skin: string, from?: string) {
  return postForm<void>('/system/skin/save', { from, skin });
}

export async function deleteSkinApi(skin: string) {
  return postForm<void>('/system/skin/delete', { skin });
}

/** D4 导出皮肤包（zip，二进制） */
export async function exportSkinApi(skin: string) {
  return requestClient.get<Blob>('/system/skin/export', {
    params: { skin },
    responseType: 'blob',
  });
}

/** D5 导入皮肤包 */
export async function importSkinApi(file: File, overwrite: boolean) {
  const form = new FormData();
  form.append('file', file);
  form.append('overwrite', overwrite ? '1' : '0');
  return requestClient.post<{ code: number; msg: string }>(
    '/system/skin/import',
    form,
  );
}

// /////////////////// D7 在线标签手册 ///////////////////

export async function getTagManualApi() {
  return requestClient.get<Record<string, TagManualRow[]>>(
    '/system/tags/manual',
  );
}

/** 下载二进制包（导出皮肤） */
export function downloadBlob(data: unknown, filename: string) {
  const url = window.URL.createObjectURL(data as Blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  window.URL.revokeObjectURL(url);
}
