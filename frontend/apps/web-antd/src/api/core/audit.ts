import { requestClient } from '#/api/request';

/**
 * 投稿审核 API（规划阶段 H）
 * 文章真实状态：0未审核 1正常 2审核未通过 3删除
 */

export interface AuditArticleRow {
  categoryId?: string;
  createTime?: string;
  id: string;
  shortUrl?: string;
  status: number;
  title: string;
  userId?: string;
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

export async function getAuditPageApi(params: {
  createTime?: string;
  p?: number;
  rows?: number;
  status?: number;
  title?: string;
  userId?: string;
}) {
  return requestClient.get<{ count: number; list: AuditArticleRow[] }>(
    '/system/audit/page',
    { params },
  );
}

export async function getPendingCountApi() {
  return requestClient.get<number>('/system/audit/pendingCount');
}

/** status：1=通过 2=驳回（驳回 reason 必填） */
export async function auditArticleApi(
  id: string,
  status: number,
  reason?: string,
) {
  return postForm<void>('/system/audit/audit', { id, reason, status });
}

export async function batchAuditApi(
  ids: string[],
  status: number,
  reason?: string,
) {
  return postForm<void>('/system/audit/batch', {
    ids: ids.join(','),
    reason,
    status,
  });
}

export async function getAuditSwitchApi() {
  return requestClient.get<number>('/system/audit/switch');
}

export async function setAuditSwitchApi(value: number) {
  return postForm<void>('/system/audit/switch', { value });
}
