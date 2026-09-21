import { requestClient } from '#/api/request';

/**
 * 碎片/推荐位管理 API（阶段 E）
 */

export interface BlockRow {
  blockKey: string;
  blockName: string;
  blockType: number;
  cacheSeconds: number;
  content?: string;
  id: string;
  itemCount: number;
  sort: number;
  status: number;
}

export interface BlockItemRow {
  blockId: string;
  endTime?: string;
  id: string;
  image?: string;
  sort: number;
  startTime?: string;
  status: number;
  summary?: string;
  title?: string;
  url?: string;
}

function postForm<T>(url: string, data: Record<string, unknown>) {
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(data)) {
    if (v !== undefined && v !== null && v !== '') {
      form.append(k, String(v));
    }
  }
  return requestClient.post<T>(url, form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

export async function getBlockListApi(params: { p?: number }) {
  return requestClient.get<{ count: number; list: BlockRow[] }>(
    '/system/block/list',
    { params },
  );
}

export async function saveBlockApi(data: Record<string, unknown>) {
  return postForm<void>(
    data.id ? '/system/block/update' : '/system/block/save',
    data,
  );
}

export async function deleteBlockApi(id: string) {
  return postForm<void>('/system/block/delete', { id });
}

export async function getBlockItemsApi(blockId: string) {
  return requestClient.get<BlockItemRow[]>('/system/blockItem/list', {
    params: { blockId },
  });
}

export async function saveBlockItemApi(data: Record<string, unknown>) {
  return postForm<void>('/system/blockItem/save', data);
}

export async function deleteBlockItemApi(id: string) {
  return postForm<void>('/system/blockItem/delete', { id });
}
