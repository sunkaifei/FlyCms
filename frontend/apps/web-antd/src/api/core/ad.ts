import { requestClient } from '#/api/request';

/**
 * 广告系统 API（fly_ad_position + fly_ad，对标帝国/Dede/PHPCMS 广告模块）
 */

export interface AdPositionRow {
  adKey: string;
  adCount?: number;
  createTime?: string;
  description?: string;
  height?: number;
  id: string;
  name: string;
  sort: number;
  status: number; // 1=启用 0=停用
  updateTime?: string;
  width?: number;
}

export interface AdRow {
  adType: 'code' | 'image' | 'text';
  countClick?: number;
  countView?: number;
  createTime?: string;
  endTime?: string;
  htmlCode?: string;
  id: string;
  imageUrl?: string;
  name: string;
  positionId: string;
  remark?: string;
  sort: number;
  startTime?: string;
  status: number; // 1=启用 0=停用
  textContent?: string;
  updateTime?: string;
  url?: string;
  weight: number;
}

function buildForm(data: Record<string, null | number | string | undefined>) {
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(data)) {
    if (v !== undefined && v !== null) form.append(k, String(v));
  }
  return form;
}

const FORM_HEADERS = {
  'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
};

// /////////////////// 广告位 ///////////////////

export async function getAdPositionListApi(params: {
  keyword?: string;
  p?: number;
  rows?: number;
}) {
  return requestClient.get<{ count: number; list: AdPositionRow[] }>(
    '/system/ad/position/list',
    { params },
  );
}

export async function saveAdPositionApi(data: {
  adKey: string;
  description?: string;
  height?: number;
  name: string;
  sort?: number;
  status?: number;
  width?: number;
}) {
  return requestClient.post<void>(
    '/system/ad/position/save',
    buildForm(data),
    { headers: FORM_HEADERS },
  );
}

export async function updateAdPositionApi(data: {
  description?: string;
  height?: number;
  id: string;
  name: string;
  sort?: number;
  status?: number;
  width?: number;
}) {
  return requestClient.post<void>(
    '/system/ad/position/update',
    buildForm(data),
    { headers: FORM_HEADERS },
  );
}

export async function deleteAdPositionApi(id: string) {
  return requestClient.post<void>(
    '/system/ad/position/delete',
    buildForm({ id }),
    { headers: FORM_HEADERS },
  );
}

// /////////////////// 广告 ///////////////////

export async function getAdListApi(params: {
  keyword?: string;
  p?: number;
  positionId: string;
  rows?: number;
}) {
  return requestClient.get<{ count: number; list: AdRow[] }>(
    '/system/ad/ad/list',
    { params },
  );
}

export async function saveAdApi(data: {
  adType: string;
  endTime?: string;
  htmlCode?: string;
  imageUrl?: string;
  name: string;
  positionId: string;
  remark?: string;
  sort?: number;
  startTime?: string;
  status?: number;
  textContent?: string;
  url?: string;
  weight?: number;
}) {
  return requestClient.post<void>('/system/ad/ad/save', buildForm(data), {
    headers: FORM_HEADERS,
  });
}

export async function updateAdApi(data: {
  adType: string;
  endTime?: string;
  htmlCode?: string;
  id: string;
  imageUrl?: string;
  name: string;
  remark?: string;
  sort?: number;
  startTime?: string;
  status?: number;
  textContent?: string;
  url?: string;
  weight?: number;
}) {
  return requestClient.post<void>('/system/ad/ad/update', buildForm(data), {
    headers: FORM_HEADERS,
  });
}

export async function deleteAdApi(id: string) {
  return requestClient.post<void>('/system/ad/ad/delete', buildForm({ id }), {
    headers: FORM_HEADERS,
  });
}

// /////////////////// 统计 ///////////////////

export interface AdStatDailyRow {
  clicks: number;
  statDate: string;
  views: number;
}

/** 按日统计（adId/positionId 可选过滤，days 默认 30），行按日期倒序 */
export async function getAdStatApi(params: {
  adId?: string;
  days?: number;
  positionId?: string;
}) {
  return requestClient.get<AdStatDailyRow[]>('/system/ad/stat/list', { params });
}
