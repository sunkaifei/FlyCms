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

/** 跨模型聚合的审核队列（不必逐个模型切换） */
export async function getAuditPageAllApi(params: {
  p?: number;
  rows?: number;
  status?: number;
  title?: string;
}) {
  return requestClient.get<{ count: number; list: AuditArticleRow[] }>(
    '/system/audit/pageAll',
    { params },
  );
}

export async function getPendingCountApi() {
  return requestClient.get<number>('/system/audit/pendingCount');
}

/** status：1=通过 2=驳回（驳回 reason 必填；notify=0 时不给作者发站内信） */
export async function auditArticleApi(
  id: string,
  status: number,
  reason?: string,
  modelId?: number | string,
  notify?: number,
) {
  return postForm<string>('/system/audit/audit', {
    id,
    modelId,
    notify,
    reason,
    status,
  });
}

export async function batchAuditApi(
  ids: string[],
  status: number,
  reason?: string,
  modelId?: number | string,
  notify?: number,
) {
  return postForm<string>('/system/audit/batch', {
    ids: ids.join(','),
    modelId,
    notify,
    reason,
    status,
  });
}

export interface ContentAuditLogRow {
  action: number;
  auditorId?: string;
  auditorName?: string;
  createTime?: string;
  id: string;
  reason?: string;
  targetId?: string;
  targetModel?: string;
  title?: string;
}

/**
 * 单条内容详情（审核员必须能看到正文才能做决定）。
 * 2026-10-23 新增：此前审核列表只有标题，后端也没有详情接口。
 */
export async function getAuditDetailApi(params: {
  id: string;
  modelId?: number | string;
}) {
  return requestClient.get<Record<string, any>>('/system/audit/detail', {
    params,
  });
}

/** 某条内容的审核历史（驳回原因可追溯） */
export async function getAuditLogsApi(params: {
  id: string;
  modelId?: number | string;
  p?: number;
  rows?: number;
}) {
  return requestClient.get<{ count: number; list: ContentAuditLogRow[] }>(
    '/system/audit/logs',
    { params },
  );
}

export async function getAuditSwitchApi() {
  return requestClient.get<number>('/system/audit/switch');
}

export async function setAuditSwitchApi(value: number) {
  return postForm<void>('/system/audit/switch', { value });
}
