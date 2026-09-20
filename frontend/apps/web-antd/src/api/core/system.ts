import { requestClient } from '#/api/request';

/**
 * FlyCms 系统管理 REST（/api/system/**，响应 DataVo，拦截器已剥壳返回 data 字段）
 */

/** 后端 PageVo 序列化后的分页数据 */
export interface PageData<T> {
  count: number;
  list: T[];
}

export interface AdminRow {
  adminName: string;
  createAt?: string;
  email?: string;
  id: number;
  lastLoginTime?: string;
  mobile?: string;
  nickName?: string;
  roleId?: number;
  status?: number;
}

export interface GroupRow {
  createAt?: string;
  id: number;
  name: string;
}

export interface PermissionRow {
  actionKey: string;
  controller?: string;
  id: number;
  remark?: string;
}

function toForm(data: Record<string, unknown>): URLSearchParams {
  const form = new URLSearchParams();
  for (const [key, value] of Object.entries(data)) {
    if (value !== undefined && value !== null && value !== '') {
      form.append(key, String(value));
    }
  }
  return form;
}

/**
 * 表单 POST：vben axios 实例默认 Content-Type 是 application/json，会覆盖
 * URLSearchParams 的自动检测，必须显式覆盖（见 docs/frontend-access-guide.md §5 坑 14）
 */
function postForm<T>(url: string, data: Record<string, unknown>) {
  return requestClient.post<T>(url, toForm(data), {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

// ///////////////////////////////
// /////       管理员       ////////
// ///////////////////////////////

export async function getAdminListApi(params: { adminName?: string; p?: number }) {
  return requestClient.get<PageData<AdminRow>>('/system/admin/list', { params });
}

export async function saveAdminApi(
  data: Partial<AdminRow> & { password?: string; repassword?: string },
) {
  return postForm<void>('/system/admin/save', data);
}

export async function updateAdminApi(
  data: Partial<AdminRow> & { password?: string; repassword?: string },
) {
  return postForm<void>('/system/admin/update', data);
}

export async function deleteAdminApi(id: number) {
  return postForm<void>('/system/admin/delete', { id });
}

// ///////////////////////////////
// /////       角色组       ////////
// ///////////////////////////////

export async function getGroupListApi() {
  return requestClient.get<GroupRow[]>('/system/group/list');
}

export async function saveGroupApi(data: { id?: number; name: string }) {
  return postForm<void>('/system/group/save', data);
}

export async function deleteGroupApi(id: number) {
  return postForm<void>('/system/group/delete', { id });
}

export async function getGroupPermissionIdsApi(id: number) {
  return requestClient.get<number[]>('/system/group/permissionIds', {
    params: { id },
  });
}

export async function assignGroupPermissionsApi(
  groupId: number,
  permissionIds: number[],
) {
  const form = new URLSearchParams();
  form.append('groupId', String(groupId));
  for (const id of permissionIds) {
    form.append('permissionIds', String(id));
  }
  return requestClient.post<void>('/system/group/assignPermissions', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

// ///////////////////////////////
// /////       权限节点      ////////
// ///////////////////////////////

export async function getPermissionListApi(params: { p?: number }) {
  return requestClient.get<PageData<PermissionRow>>('/system/permission/list', {
    params,
  });
}

export async function getAllPermissionsApi() {
  return requestClient.get<PermissionRow[]>('/system/permission/all');
}

export async function syncPermissionsApi() {
  return requestClient.post<void>('/system/permission/sync');
}

export async function deletePermissionApi(id: number) {
  return postForm<void>('/system/permission/delete', { id });
}
