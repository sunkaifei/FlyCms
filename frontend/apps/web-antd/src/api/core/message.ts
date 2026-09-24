import { requestClient } from '#/api/request';

/**
 * 站内短信管理 API（fly_message，is_admin=1 系统信息）
 */

export interface MessageRow {
  id: string;
  fromId: string;
  fromNickname?: string;
  isAdmin: number;
  message?: string;
  sendTime?: string;
  state: number;
  subject: string;
  toId: string;
  toNickname?: string;
}

export async function getMessageListApi(params: {
  p?: number;
  subject?: string;
}) {
  return requestClient.get<{ count: number; list: MessageRow[] }>(
    '/system/message/list',
    { params },
  );
}

export async function sendMessageApi(data: {
  message: string;
  subject: string;
  toUsername: string;
}) {
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(data)) {
    if (v) form.append(k, v);
  }
  return requestClient.post<void>('/system/message/send', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

export async function deleteMessageApi(id: string) {
  const form = new URLSearchParams();
  form.append('id', id);
  return requestClient.post<void>('/system/message/delete', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

// /////////////////// 公告管理 ///////////////////

export interface AnnouncementRow {
  content?: string;
  createTime?: string;
  endTime?: string;
  id: string;
  linkUrl?: string;
  sort: number;
  startTime?: string;
  status: number;
  title: string;
}

export async function getAnnouncementListApi(params: { p?: number }) {
  return requestClient.get<{ count: number; list: AnnouncementRow[] }>(
    '/system/announcement/list',
    { params },
  );
}

export async function getAnnouncementDetailApi(id: string) {
  return requestClient.get<AnnouncementRow>(`/system/announcement/${id}`);
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

export async function saveAnnouncementApi(data: Record<string, unknown>) {
  return postForm<void>('/system/announcement/save', data);
}

export async function updateAnnouncementApi(data: Record<string, unknown>) {
  return postForm<void>('/system/announcement/update', data);
}

export async function deleteAnnouncementApi(id: string) {
  return postForm<void>('/system/announcement/delete', { id });
}
