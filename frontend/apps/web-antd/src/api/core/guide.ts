import { requestClient } from '#/api/request';

/**
 * 导航管理 API（规划阶段 B3）
 */

export interface GuideRow {
  id: string;
  link: string;
  name: string;
  sort: number;
  status: number;
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

export async function getGuideTreeApi(status?: number) {
  return requestClient.get<GuideRow[]>('/system/guide/tree', {
    params: { status },
  });
}

export async function getGuidePageApi(params: {
  name?: string;
  p?: number;
  rows?: number;
  status?: number;
}) {
  return requestClient.get<{ count: number; list: GuideRow[] }>(
    '/system/guide/page',
    { params },
  );
}

export async function getGuideApi(id: string) {
  return requestClient.get<GuideRow>('/system/guide/get', { params: { id } });
}

export async function saveGuideApi(data: Record<string, unknown>) {
  return postForm<void>('/system/guide/save', data);
}

export async function deleteGuideApi(id: string) {
  return postForm<void>('/system/guide/delete', { id });
}

export async function updateGuideStatusApi(id: string, status: number) {
  return postForm<void>('/system/guide/status', { id, status });
}
