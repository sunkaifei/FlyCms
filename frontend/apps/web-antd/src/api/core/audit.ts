import { requestClient } from '#/api/request';

/**
 * 内容审核 API（U3 起模型驱动）：审核对象 = 自定义模型内容（fly_cmodel_*.status）
 * 状态：0待审 1发布 2未通过；审核开关沿用 fly_article_audit 键。
 */

export interface AuditArticleRow {
  categoryId?: string;
  createTime?: string;
  id: string;
  shortUrl?: string;
  status: number;
  title: string;
  userId?: string;
  [key: string]: unknown;
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
  modelId?: number | string;
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

/** status：1=通过 2=驳回（驳回 reason 必填；reason 由站内信通道另行处理） */
export async function auditArticleApi(
  id: string,
  status: number,
  reason?: string,
  modelId?: number | string,
) {
  return postForm<void>('/system/audit/audit', { id, modelId, reason, status });
}

export async function batchAuditApi(
  ids: string[],
  status: number,
  reason?: string,
  modelId?: number | string,
) {
  return postForm<void>('/system/audit/batch', {
    ids: ids.join(','),
    modelId,
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
