import { requestClient } from '#/api/request';

/**
 * 自定义模型系统 API（/api/system/model/**，DataVo 契约已由拦截器剥壳）
 */

export interface ModelRow {
  code: string;
  createAt?: string;
  description?: string;
  detailTemplate?: string;
  icon?: string;
  id: number;
  isSystem: number;
  listTemplate?: string;
  name: string;
  sort: number;
  status: number;
  titleLabel: string;
}

export interface ModelFieldRow {
  columnType: string;
  defaultValue?: string;
  fieldName: string;
  fieldLabel: string;
  fieldType: string;
  id: number;
  tabName?: string;
  isFilter: number;
  isList: number;
  isRequired: number;
  isSearch: number;
  maxlength?: number;
  options?: string;
  placeholder?: string;
  regex?: string;
  /** RELATE / RELATES 字段的目标模型 code（E1；关联本模型时=本模型 code） */
  relateModel?: string;
  /** 值是否全模型唯一（1=是） */
  isUnique?: number;
  /** 数值区间下限（number/decimal/rating） */
  minValue?: number;
  /** 数值区间上限（number/decimal/rating） */
  maxValue?: number;
  /** 父字段 id（P1：>0 = GROUP/REPEATER 子字段） */
  parentId?: number;
  /** 条件显隐 JSON（P1：{"field","op","value"}） */
  visibleWhen?: string;
  /** Rollup 聚合 JSON（P2：{"source","func","column"}） */
  rollupExpr?: string;
  /** Lookup 展示列 JSON 数组（P2：relate 展开目标行的额外列） */
  lookupFields?: string;
  sort: number;
  tips?: string;
}

export interface ModelCategoryRow {
  fatherId: number;
  id: number;
  name: string;
  sort: number;
}

interface PageData<T> {
  count: number;
  list: T[];
}

export interface FormMeta {
  categories: ModelCategoryRow[];
  fields: ModelFieldRow[];
  model: ModelRow;
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

// /////////////////// 模型 ///////////////////

export async function getModelListApi(params: { p?: number }) {
  return requestClient.get<PageData<ModelRow>>('/system/model/list', { params });
}

export async function getModelByCodeApi(code: string) {
  return requestClient.get<ModelRow>(`/system/model/byCode/${code}`);
}

export async function saveModelApi(data: Record<string, unknown>) {
  return postForm<void>('/system/model/save', data);
}

export async function updateModelApi(data: Record<string, unknown>) {
  return postForm<void>('/system/model/update', data);
}

export async function deleteModelApi(id: number | string) {
  return postForm<void>('/system/model/del', { id });
}

// /////////////////// 字段 ///////////////////

export async function getFieldListApi(modelId: number | string) {
  return requestClient.get<ModelFieldRow[]>('/system/modelField/list/' + modelId);
}

export async function saveFieldApi(data: Record<string, unknown>) {
  return postForm<void>('/system/modelField/save', data);
}

export async function updateFieldApi(data: Record<string, unknown>) {
  return postForm<void>('/system/modelField/update', data);
}

export async function deleteFieldApi(id: number | string) {
  return postForm<void>('/system/modelField/del', { id });
}

export async function sortFieldApi(ids: number[], sorts: number[]) {
  const form = new URLSearchParams();
  for (const id of ids) form.append('ids', String(id));
  for (const s of sorts) form.append('sorts', String(s));
  return requestClient.post<void>('/system/modelField/sort', form, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
  });
}

// /////////////////// 动态内容 ///////////////////

export async function getDataListApi(
  modelId: number | string,
  params: Record<string, unknown>,
) {
  return requestClient.get<PageData<Record<string, any>>>(
    '/system/modelData/list/' + modelId,
    { params },
  );
}

export async function getFormMetaApi(modelId: number | string) {
  return requestClient.get<FormMeta>('/system/modelData/formMeta/' + modelId);
}

export async function getDataDetailApi(modelId: number | string, id: number | string) {
  return requestClient.get<Record<string, any>>(
    `/system/modelData/detail/${modelId}/${id}`,
  );
}

export async function saveDataApi(data: Record<string, unknown>) {
  return postForm<number>('/system/modelData/save', data);
}

export async function updateDataApi(data: Record<string, unknown>) {
  return postForm<void>('/system/modelData/update', data);
}

export async function deleteDataApi(modelId: number | string, ids: string) {
  return postForm<void>('/system/modelData/del', { modelId, ids });
}

export async function updateDataStatusApi(
  modelId: number | string,
  ids: string,
  status: number,
) {
  return postForm<void>('/system/modelData/status', { modelId, ids, status });
}

// /////////////////// 分类 ///////////////////

export async function getCategoryTreeApi(modelId: number) {
  return requestClient.get<ModelCategoryRow[]>(
    '/system/modelCategory/tree/' + modelId,
  );
}

export async function saveCategoryApi(data: Record<string, unknown>) {
  return postForm<void>('/system/modelCategory/save', data);
}

// /////////////////// 附件（AttachmentPicker 数据源） ///////////////////

export interface AttachmentRow {
  id: number;
  imgName?: string;
  imgUrl?: string;
}

export async function getAttachmentListApi(params: { p?: number }) {
  return requestClient.get<PageData<AttachmentRow>>(
    '/system/attachment/list',
    { params },
  );
}
