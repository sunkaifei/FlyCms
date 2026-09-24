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
  return postForm<{ code: number; msg: string }>('/system/template/restore', { file, version });
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
  return postForm<{ code: number; msg: string }>('/system/skin/save', { from, skin });
}

export async function deleteSkinApi(skin: string) {
  return postForm<{ code: number; msg: string }>('/system/skin/delete', { skin });
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

// /////////////////// P5 主题市场：列表/预检/启用/回滚 ///////////////////

export interface ThemeInfo {
  author?: string;
  code: string;
  description?: string;
  device?: string;
  isCurrent?: number;
  name: string;
  parentCode?: string;
  thumbnail?: string;
  version?: string;
  supportsTemplates?: string[];
  customTemplates?: { file: string; name: string; postTypes: string[] }[];
}

export interface ThemeListResult {
  current: string;
  themes: ThemeInfo[];
}

export async function getThemeListApi() {
  return requestClient.get<ThemeListResult>('/system/theme/list');
}

export async function checkThemeApi(code: string) {
  return requestClient.get<{
    code: number;
    data?: { blocks: string[]; ok: boolean; warnings: string[] };
    msg: string;
  }>('/system/theme/check', { params: { code } });
}

export async function enableThemeApi(code: string) {
  return postForm<{ code: number; data?: { previous: string }; msg: string }>(
    '/system/theme/enable',
    { code },
  );
}

export async function rollbackThemeApi() {
  return postForm<{ code: number; msg: string }>('/system/theme/rollback', {});
}

export async function previewThemeApi(code: string) {
  return requestClient.get<{ code: number; data?: { url: string }; msg: string }>(
    '/system/theme/preview',
    { params: { code } },
  );
}

export async function createChildThemeApi(
  child: string,
  parent: string,
  name?: string,
) {
  return postForm<{ code: number; msg: string }>('/system/theme/createChild', { child, parent, name });
}

export async function copyParentThemeApi(
  child: string,
  parent: string,
  files: string[],
) {
  return postForm<{ code: number; msg: string }>('/system/theme/copyParent', { child, parent, files });
}

// /////////////////// P7 模板指派（DB 覆盖层） ///////////////////

export interface TemplateAssignRow {
  id: string;
  pageType: string;
  targetId: string;
  targetType: string;
  template: string;
}

export async function assignTemplateApi(
  targetType: string,
  targetId: string,
  pageType: string,
  template: string,
) {
  return postForm<{ code: number; msg: string }>('/system/template/assign', {
    pageType,
    targetId,
    targetType,
    template,
  });
}

export async function unassignTemplateApi(
  targetType: string,
  targetId: string,
  pageType: string,
) {
  return postForm<{ code: number; msg: string }>('/system/template/unassign', {
    pageType,
    targetId,
    targetType,
  });
}

export async function getAssignListApi(targetType: string) {
  return requestClient.get<{ list: TemplateAssignRow[] }>(
    '/system/template/assign/list',
    { params: { targetType } },
  );
}

// /////////////////// D22 WordPress 主题包转换 + 模板派生 ///////////////////

export async function importWpThemeApi(
  file: File,
  skin?: string,
  overwrite = false,
) {
  const form = new FormData();
  form.append('file', file);
  if (skin) {
    form.append('skin', skin);
  }
  form.append('overwrite', overwrite ? '1' : '0');
  return requestClient.post<{
    code: number;
    data?: {
      convertedFiles: string[];
      copiedResources: string[];
      todo: string[];
      todoCount: number;
    };
    msg: string;
  }>('/system/skin/importWp', form);
}

export async function getConvertReportApi(skin?: string) {
  return requestClient.get<{ code: number; data?: Record<string, unknown>; msg: string }>(
    '/system/skin/convertReport',
    { params: { skin } },
  );
}

export async function deriveTemplateApi(
  file: string,
  target: string,
  skin?: string,
  overwrite = false,
) {
  return postForm<{ code: number; msg: string }>('/system/template/derive', {
    file,
    overwrite: overwrite ? '1' : '0',
    skin,
    target,
  });
}

export async function getDeriveTargetsApi(skin?: string) {
  return requestClient.get<{
    skin: string;
    slots: {
      exists: boolean;
      key: string;
      kind: string;
      label: string;
      scope: string;
      target: string;
    }[];
    total: number;
  }>('/system/template/deriveTargets', { params: { skin } });
}

// /////////////////// P9 一键清缓存 + P8 候选链调试 ///////////////////

/** 清空 FreeMarker 模板缓存 + 主题注册表缓存（"改了没生效"的最后手段） */
export async function clearTemplateCacheApi() {
  return postForm<{ code: number; msg: string }>(
    '/system/template/cache/clear',
    {},
  );
}

/** 候选链节点（后台调试条） */
export interface DebugChainCandidate {
  exists: boolean;
  /** 相对文件名（无 .html 后缀） */
  file: string;
  source: string;
  /** 命中的真实主题（子主题覆盖时为父主题） */
  theme?: string;
  /** 命中的视图名 */
  view?: string;
}

/** 给定页面上下文，返回完整候选链与命中情况（不触发渲染） */
export async function getDebugChainApi(params: {
  channel?: string;
  contentId?: string;
  errorCode?: number;
  model?: string;
  pageType: string;
  shortUrl?: string;
}) {
  return requestClient.get<{ chain: DebugChainCandidate[]; skin: string }>(
    '/system/template/debugChain',
    { params },
  );
}
