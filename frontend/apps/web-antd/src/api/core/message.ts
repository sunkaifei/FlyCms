import { requestClient } from '#/api/request';

/**
 * 站内信 API（2026-10-23）。
 *
 * 背景：审核页一直弹「已驳回，并已站内信通知作者」，而在此之前站内信
 * 无表、无实现、无接口 —— UI 承诺了不存在的能力。这里把能力补齐。
 */

export interface UserMessageRow {
  content?: string;
  createTime?: string;
  id: string;
  isRead: number;
  msgType: number;
  senderName?: string;
  targetId?: number;
  targetModel?: string;
  title: string;
  userId: number;
  [key: string]: unknown;
}

/** 消息类型字典（与后端 UserMessage.Type 对齐） */
export const MESSAGE_TYPE_TEXT: Record<number, string> = {
  1: '系统通知',
  2: '审核通知',
  3: '互动提醒',
};

export const MESSAGE_TYPE_COLOR: Record<number, string> = {
  1: 'blue',
  2: 'orange',
  3: 'green',
};

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

export async function getMessagePageApi(params: {
  isRead?: number;
  keyword?: string;
  msgType?: number;
  p?: number;
  rows?: number;
  userId?: number;
}) {
  return requestClient.get<{ count: number; list: UserMessageRow[] }>(
    '/system/message/page',
    { params },
  );
}

/** 发送站内信：userIds 为空表示全体 */
export async function sendMessageApi(data: {
  content?: string;
  title: string;
  userId?: number;
  userIds?: string;
}) {
  return postForm<number>('/system/message/send', data);
}

export async function deleteMessageApi(ids: string[]) {
  return postForm<number>('/system/message/delete', { ids: ids.join(',') });
}

export async function readMessageApi(data: { ids?: string; userId: number }) {
  return postForm<number>('/system/message/read', data);
}
