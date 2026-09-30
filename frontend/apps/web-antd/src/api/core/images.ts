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

// /////////////////// W 批次：控件直传与回显 ///////////////////

/** 控件内直传图片（multipart），落 fly_images 孤儿态，返回 id+url */
export async function uploadImageApi(file: globalThis.File) {
  const form = new FormData();
  form.append('file', file);
  return requestClient.post<{ id: string; imgUrl: string; imgName?: string }>(
    '/system/images/upload',
    form,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}

/** 批量 id → {id,imgUrl,imgName} 映射（编辑表单打开时回显缩略图） */
export async function getAttachmentBatchApi(ids: string[]) {
  return requestClient.get<
    { id: number; imgName?: string; imgUrl?: string }[]
  >('/system/images/batch', { params: { ids: ids.join(',') } });
}
