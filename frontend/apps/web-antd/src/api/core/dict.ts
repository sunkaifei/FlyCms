import { requestClient } from '#/api/request';

/**
 * 数据字典 API（fly_dict_type + fly_dict_data，若依式 dict_type 绑定的底座）
 */

export interface DictTypeRow {
  createTime?: string;
  dictName: string;
  dictType: string;
  id: string;
  remark?: string;
  sort: number;
  status: number; // 1=启用 0=停用
  updateTime?: string;
}

export interface DictDataRow {
  createTime?: string;
  dictLabel: string;
  dictType: string;
  dictValue: string;
  id: string;
  remark?: string;
  sort: number;
  status: number; // 1=启用 0=停用
  updateTime?: string;
}

/** 表单候选项（label 显示 / value 入库） */
export interface DictOption {
  label: string;
  value: string;
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

// /////////////////// 字典类型 ///////////////////

export async function getDictTypeListApi(params: {
  keyword?: string;
  p?: number;
  rows?: number;
}) {
  return requestClient.get<{ count: number; list: DictTypeRow[] }>(
    '/system/dict/type/list',
    { params },
  );
}

/** 全部启用类型（字段设置「绑定字典」下拉用） */
export async function getDictTypeOptionsApi() {
  return requestClient.get<{ dictName: string; dictType: string }[]>(
    '/system/dict/type/options',
  );
}

export async function saveDictTypeApi(data: {
  dictName: string;
  dictType: string;
  remark?: string;
  sort?: number;
  status?: number;
}) {
  return requestClient.post<void>('/system/dict/type/save', buildForm(data), {
    headers: FORM_HEADERS,
  });
}

export async function updateDictTypeApi(data: {
  dictName: string;
  id: string;
  remark?: string;
  sort?: number;
  status?: number;
}) {
  return requestClient.post<void>('/system/dict/type/update', buildForm(data), {
    headers: FORM_HEADERS,
  });
}

export async function deleteDictTypeApi(id: string) {
  return requestClient.post<void>(
    '/system/dict/type/delete',
    buildForm({ id }),
    { headers: FORM_HEADERS },
  );
}

// /////////////////// 字典数据 ///////////////////

export async function getDictDataListApi(params: {
  dictType: string;
  keyword?: string;
  p?: number;
  rows?: number;
}) {
  return requestClient.get<{ count: number; list: DictDataRow[] }>(
    '/system/dict/data/list',
    { params },
  );
}

/** 某字典类型的启用数据（发布/筛选表单候选项） */
export async function getDictDataByTypeApi(dictType: string) {
  return requestClient.get<DictDataRow[]>(
    `/system/dict/data/type/${dictType}`,
  );
}

export async function saveDictDataApi(data: {
  dictLabel: string;
  dictType: string;
  dictValue: string;
  remark?: string;
  sort?: number;
  status?: number;
}) {
  return requestClient.post<void>('/system/dict/data/save', buildForm(data), {
    headers: FORM_HEADERS,
  });
}

export async function updateDictDataApi(data: {
  dictLabel: string;
  dictValue: string;
  id: string;
  remark?: string;
  sort?: number;
  status?: number;
}) {
  return requestClient.post<void>('/system/dict/data/update', buildForm(data), {
    headers: FORM_HEADERS,
  });
}

export async function deleteDictDataApi(id: string) {
  return requestClient.post<void>(
    '/system/dict/data/delete',
    buildForm({ id }),
    { headers: FORM_HEADERS },
  );
}
