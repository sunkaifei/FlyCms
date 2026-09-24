import { requestClient } from '#/api/request';

/**
 * 栏目管理 API（阶段 C）
 *
 * 栏目是「树 + URL 归属层」，modelId 默认为 0 表示不绑定模型
 * （单页/外链/聚合栏目），这是与"模型绑死"的老 CMS 的关键区别。
 */

export interface ChannelRow {
  channelDir: string;
  channelName: string;
  /** 0列表 1单页 2外链 3聚合 */
  channelType: number;
  children?: ChannelRow[];
  detailTemplate?: string;
  fatherId: number;
  id: string;
  listTemplate?: string;
  modelId: number;
  outUrl?: string;
  pageContent?: string;
  pageSize: number;
  seoDescription?: string;
  seoKeywords?: string;
  seoTitle?: string;
  sort: number;
  status: number;
}

export interface ChannelModelOption {
  code: string;
  id: number;
  modelName: string;
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

export async function getChannelTreeApi() {
  return requestClient.get<ChannelRow[]>('/system/channel/tree');
}

/** 可选模型（含 id=0 的"不绑定模型"） */
export async function getChannelModelsApi() {
  return requestClient.get<ChannelModelOption[]>('/system/channel/models');
}

/** 目录名冲突预检 */
export async function checkChannelDirApi(dir: string, id?: string) {
  return requestClient.get<{ code: number; msg: string }>(
    '/system/channel/checkdir',
    { params: { dir, id } },
  );
}

export async function saveChannelApi(data: Record<string, unknown>) {
  return postForm<void>('/system/channel/save', data);
}

/**
 * 删除栏目：只删栏目行，内容数据不动
 * @param childrenMode promote=子栏目上提到父级 hide=子栏目转为隐藏
 */
export async function deleteChannelApi(id: string, childrenMode = 'promote') {
  return postForm<void>('/system/channel/delete', { childrenMode, id });
}

export async function statusChannelApi(id: string, status: number) {
  return postForm<void>('/system/channel/status', { id, status });
}

export async function moveChannelApi(id: string, up: boolean) {
  return postForm<void>('/system/channel/move', { id, up: up ? 1 : 0 });
}

export async function moveToChannelApi(id: string, fatherId: number) {
  return postForm<void>('/system/channel/moveTo', { fatherId, id });
}
