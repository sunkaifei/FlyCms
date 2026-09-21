import { requestClient } from '#/api/request';

/**
 * 审计日志 API（阶段 A4）
 */

export interface AdminLogRow {
  adminId: number;
  adminName: string;
  costMs: number;
  createTime: string;
  id: string;
  ip: string;
  method: string;
  path: string;
  query: string;
  status: number;
}

export async function getAdminLogApi(params: {
  adminName?: string;
  endTime?: string;
  p?: number;
  path?: string;
  startTime?: string;
}) {
  return requestClient.get<{ count: number; list: AdminLogRow[] }>(
    '/system/log/list',
    { params },
  );
}
