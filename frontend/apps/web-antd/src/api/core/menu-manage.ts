import { requestClient } from '#/api/request';

/**
 * 菜单管理 API（若依式：fly_admin_permission = 菜单/按钮树）
 */

export interface MenuNode {
  actionKey?: string;
  component?: string;
  controller?: string;
  icon?: string;
  id: number;
  menuName?: string;
  menuType: 'C' | 'F' | 'M';
  parentId: number;
  path?: string;
  remark?: string;
  sort: number;
  visible: number;
}

function postForm<T>(url: string, data: Record<string, unknown>) {
  const form = new URLSearchParams();
  for (const [key, value] of Object.entries(data)) {
    if (value !== undefined && value !== null && value !== '') {
      form.append(key, String(value));
    }
  }
  return requestClient.post<T>(url, form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

export async function getMenuListApi() {
  return requestClient.get<MenuNode[]>('/system/menu/list');
}

export async function saveMenuApi(data: Record<string, unknown>) {
  return postForm<void>('/system/menu/save', data);
}

export async function updateMenuApi(data: Record<string, unknown>) {
  return postForm<void>('/system/menu/update', data);
}

export async function deleteMenuApi(id: number) {
  return postForm<void>('/system/menu/del', { id });
}
