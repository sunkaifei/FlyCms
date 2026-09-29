import { requestClient } from '#/api/request';

/**
 * 评论审核 API（规划阶段 B1）
 * status：0未审 1正常 2未通过 3删除
 */

export interface CommentRow {
  /** U3 平台评论：目标模型 code 与内容 id（兼容保留 articleId 别名语义） */
  targetModel?: string;
  targetId?: string;
  targetTitle?: string;
  content: string;
  createTime: string;
  id: string;
  status: number;
  userId?: string;
}

export interface CommentPage {
  count: number;
  list: CommentRow[];
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

export async function getCommentPageApi(params: {
  articleId?: string;
  targetModel?: string;
  targetId?: string;
  createTime?: string;
  keyword?: string;
  p?: number;
  rows?: number;
  status?: number;
  userId?: string;
}) {
  return requestClient.get<CommentPage>('/system/comment/page', { params });
}

export async function auditCommentApi(id: string, status: number) {
  return postForm<void>('/system/comment/audit', { id, status });
}

export async function deleteCommentApi(id: string) {
  return postForm<void>('/system/comment/delete', { id });
}

/** status：1=通过 2=驳回 3=删除 */
export async function batchCommentApi(ids: string[], status: number) {
  return postForm<void>('/system/comment/batch', {
    ids: ids.join(','),
    status,
  });
}
