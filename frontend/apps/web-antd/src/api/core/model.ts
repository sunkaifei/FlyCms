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
  /** 表单布局开关（U3）：0=内容表单不渲染「详细内容」选项卡；undefined 视为 1 */
  useContent?: number;
  /** 表单布局开关（U3）：0=内容表单不渲染「SEO 设置」选项卡；undefined 视为 1 */
  useSeo?: number;
  /** E9 模型级评论开关：0=关闭前台评论；undefined 视为 1 */
  enableComment?: number;
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
  /** 表单隐藏（U3）：0=不出现在内容表单（不渲染不提交不校验） */
  isForm?: number;
  /** E6 行内公式（FORMULA 类型）：变量=本模型字段名 */
  formula?: string;
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

export async function getModelListApi(params: { p?: number; rows?: number }) {
  return requestClient.get<PageData<ModelRow>>('/system/model/list', { params });
}

/** G12 内容版本行 */
export interface ContentVersionRow {
  createTime?: string;
  editorId?: string;
  id: string;
  remark?: string;
  status?: number;
  targetId: string;
  targetModel: string;
  version: number;
}

/** G12 内容版本列表（新→旧） */
export async function getVersionListApi(modelId: number | string, id: string, params?: { p?: number; rows?: number }) {
  return requestClient.get<{ count: number; list: ContentVersionRow[] }>(
    `/system/modelData/version/list/${modelId}/${id}`,
    { params },
  );
}

/** G16 草稿预览：签发短时效预览 URL（绑定模型+内容，重启失效） */
export async function getPreviewTokenApi(modelId: number | string, id: string) {
  return requestClient.get<{ url: string }>('/system/modelData/previewToken', {
    params: { id, modelId },
  });
}

/** G12 恢复到指定版本（恢复动作另存为新版本） */
export async function restoreVersionApi(modelId: number | string, id: string, version: number) {
  const form = new URLSearchParams({ id: String(id), modelId: String(modelId), version: String(version) });
  return requestClient.post<void>('/system/modelData/version/restore', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' },
  });
}

/** B1 字段绑定数据源：用户选项（可搜索；显示昵称，提交存 user_id） */
export async function getUserOptionsApi(keyword?: string) {
  return requestClient.get<
    { id: string; label: string }[]
  >('/system/options/users', { params: { keyword, rows: 200 } });
}

/** B1 字段绑定数据源：绑定模型的分类树平铺（前端组树） */
export async function getCategoryOptionsApi(modelCode: string) {
  return requestClient.get<
    { fatherId: string; id: string; name: string; status: number }[]
  >('/system/options/categories', { params: { modelCode } });
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

export async function deleteCategoryApi(id: number | string) {
  return postForm<void>('/system/modelCategory/del', { id });
}

/** G15 字段组库（复制式 Component） */
export async function getComponentListApi() {
  return requestClient.get<any[]>('/system/component/list');
}

export async function applyComponentApi(modelId: number | string, componentId: number | string) {
  const form = new URLSearchParams({
    componentId: String(componentId),
    modelId: String(modelId),
  });
  return requestClient.post<{ message: string }>('/system/component/apply', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' },
  });
}

export async function pickupComponentApi(data: Record<string, unknown>) {
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(data)) {
    if (v !== undefined && v !== null && v !== '') form.append(k, String(v));
  }
  return requestClient.post<void>('/system/component/pickup', form, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' },
  });
}

export async function deleteComponentApi(id: number | string) {
  return postForm<void>('/system/component/del', { id });
}

/** G20 模型定义（导出/导入） */
export interface ModelDefinition {
  categories: { fatherName?: string; keywords?: string; name: string; sort?: number }[];
  fields: Record<string, unknown>[];
  model: Record<string, unknown>;
}

export async function exportModelApi(id: number | string) {
  return requestClient.get<ModelDefinition>(`/system/model/export/${id}`);
}

export async function importModelApi(payload: string) {
  const form = new URLSearchParams({ payload });
  return requestClient.post<{ categoryAdded: number; errors: string[]; fieldAdded: number; fieldFailed: number; fieldUpdated: number; modelCreated: boolean }>(
    '/system/model/import',
    form,
    { headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' } },
  );
}

/** E10：重新生成模型默认模板（list.html/detail.html 强制覆盖 + 注入标签速查注释块） */
export async function regenTemplatesApi(id: number | string) {
  return postForm<void>(`/system/model/regenTemplates/${id}`, {});
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
