import { requestClient } from '#/api/request';

/**
 * 附件库 API（规划阶段 B2）
 */

export interface ImageRow {
  createTime?: string;
  description?: string;
  fileSize?: number;
  id: string;
  imgHeight?: number;
  imgName?: string;
  imgUrl: string;
  imgWidth?: number;
  infoCount?: number;
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

export async function getImagesPageApi(params: {
  keyword?: string;
  onlyOrphan?: number;
  p?: number;
  rows?: number;
}) {
  return requestClient.get<{ count: number; list: ImageRow[] }>(
    '/system/images/page',
    { params },
  );
}

export async function getOrphanCountApi() {
  return requestClient.get<{ count: number }>('/system/images/orphanCount');
}

export async function deleteOrphanImagesApi(ids?: string[]) {
  return postForm<void>('/system/images/deleteOrphan', {
    ids: ids && ids.length ? ids.join(',') : '',
  });
}
