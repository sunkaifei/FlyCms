import { requestClient } from '#/api/request';

/**
 * 表单系统 API（规划阶段 F）
 */

export interface FormRow {
  audit: number;
  createTime?: string;
  formCode: string;
  formName: string;
  id: string;
  needCaptcha: number;
  notifyEmail?: string;
  status: number;
  submitLimit: number;
  successTip?: string;
}

export interface FormFieldRow {
  defaultValue?: string;
  fieldCode: string;
  fieldName: string;
  fieldType: string;
  formId: string;
  id: string;
  options?: string;
  placeholder?: string;
  required: number;
  sort: number;
}

export interface FormDataRow {
  createTime?: string;
  formId: string;
  formName?: string;
  id: string;
  ip?: string;
  status: number;
  userId?: string;
}

export const FIELD_TYPES = [
  { label: '单行文本', value: 'text' },
  { label: '多行文本', value: 'textarea' },
  { label: '数字', value: 'number' },
  { label: '单选', value: 'radio' },
  { label: '多选', value: 'checkbox' },
  { label: '下拉', value: 'select' },
  { label: '日期', value: 'date' },
  { label: '邮箱', value: 'email' },
  { label: '手机', value: 'mobile' },
  { label: '文件上传', value: 'file' },
];

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

// /////////// 表单 ///////////

export async function getFormPageApi(params: {
  formName?: string;
  p?: number;
  rows?: number;
  status?: number;
}) {
  return requestClient.get<{ count: number; list: FormRow[] }>(
    '/system/form/page',
    { params },
  );
}

export async function getFormApi(id: string) {
  return requestClient.get<FormRow>('/system/form/get', { params: { id } });
}

export async function saveFormApi(data: Record<string, unknown>) {
  return postForm<void>('/system/form/save', data);
}

export async function deleteFormApi(id: string) {
  return postForm<void>('/system/form/delete', { id });
}

// /////////// 字段 ///////////

export async function getFormFieldListApi(formId: string) {
  return requestClient.get<FormFieldRow[]>('/system/formField/list', {
    params: { formId },
  });
}

export async function saveFormFieldApi(data: Record<string, unknown>) {
  return postForm<void>('/system/formField/save', data);
}

export async function deleteFormFieldApi(id: string) {
  return postForm<void>('/system/formField/delete', { id });
}

// /////////// 数据 ///////////

export async function getFormDataPageApi(params: {
  createTime?: string;
  formId?: string;
  p?: number;
  rows?: number;
  status?: number;
}) {
  return requestClient.get<{ count: number; list: FormDataRow[] }>(
    '/system/formData/page',
    { params },
  );
}

export async function getFormDataDetailApi(id: string) {
  return requestClient.get<{
    createTime?: string;
    formName?: string;
    id: string;
    ip?: string;
    status: number;
    values: Record<string, unknown>;
  }>('/system/formData/detail', { params: { id } });
}

export async function auditFormDataApi(id: string, status: number) {
  return postForm<void>('/system/formData/audit', { id, status });
}

export async function deleteFormDataApi(id: string) {
  return postForm<void>('/system/formData/delete', { id });
}

/** CSV 导出走浏览器直接下载（后端带 BOM，Excel 不乱码） */
export function exportFormDataUrl(params: {
  createTime?: string;
  formId?: string;
  status?: number;
}) {
  const query = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') {
      query.append(k, String(v));
    }
  }
  const qs = query.toString();
  return `/api/system/formData/export${qs ? `?${qs}` : ''}`;
}
