import { requestClient } from '#/api/request';

/**
 * 友情链接 API（规划阶段 B3）
 * type：0文字链接 1logo链接；isShow：0不显示 1显示
 */

export interface LinkRow {
  createTime?: string;
  id: string;
  isShow: number;
  linkLogo?: string;
  linkName: string;
  linkUrl: string;
  sort: number;
  type: number;
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

export async function getLinksPageApi(params: {
  isShow?: number;
  keyword?: string;
  p?: number;
  rows?: number;
  type?: number;
}) {
  return requestClient.get<{ count: number; list: LinkRow[] }>(
    '/system/links/page',
    { params },
  );
}

export async function getLinkApi(id: string) {
  return requestClient.get<LinkRow>('/system/links/get', { params: { id } });
}

export async function saveLinkApi(data: Record<string, unknown>) {
  return postForm<void>('/system/links/save', data);
}

export async function deleteLinkApi(id: string) {
  return postForm<void>('/system/links/delete', { id });
}

export async function updateLinkStatusApi(id: string, isShow: number) {
  return postForm<void>('/system/links/status', { id, isShow });
}
