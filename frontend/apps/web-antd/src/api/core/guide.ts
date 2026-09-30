import { requestClient } from '#/api/request';

/**
 * 站点导航 API（fly_guide，导航管理页）
 *
 * 导航项三种来源：type 0=自定义链接（link 直存）1=栏目（refId=栏目id）
 * 2=模型分类（refId=分类id）。url 为后端按来源计算的最终链接。
 */

export interface GuideRow {
  children?: GuideRow[];
  /** type=0 时的自定义链接 */
  link?: string;
  /** 上级导航项 id，0=顶级 */
  fatherId: number;
  id: string;
  name: string;
  /** 绑定对象 id（type=1 栏目 / type=2 分类），type=0 时为空 */
  refId?: number | string;
  /** 打开方式：'' 当前窗口 / _blank 新窗口 */
  target?: string;
  /** 0=自定义链接 1=栏目 2=模型分类 */
  type: number;
  /** 排序，越小越靠前 */
  sort: number;
  /** 1=显示 0=隐藏 */
  status: number;
  /** 后端按 type 计算的最终链接（栏目 /{dir}/、分类 /{code}/c{id}） */
  url?: string;
  /** type=2 时绑定的模型 code（编辑回显分类树用） */
  refModel?: string;
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

/** 导航树（含隐藏项，父项选择用） */
export async function getGuideTreeApi() {
  return requestClient.get<GuideRow[]>('/system/guide/tree');
}

/** 导航项详情 */
export async function getGuideApi(id: string) {
  return requestClient.get<GuideRow>('/system/guide/get', {
    params: { id },
  });
}

/** 新增/修改导航项（id 传空串=新增） */
export async function saveGuideApi(data: {
  fatherId: number | string;
  id?: string;
  link?: string;
  name: string;
  refId?: number | string;
  sort?: number;
  status?: number;
  target?: string;
  type: number;
}) {
  return requestClient.post<void>('/system/guide/save', buildForm(data), {
    headers: FORM_HEADERS,
  });
}

export async function deleteGuideApi(id: string) {
  return requestClient.post<void>('/system/guide/delete', buildForm({ id }), {
    headers: FORM_HEADERS,
  });
}

/** 显隐切换 */
export async function statusGuideApi(id: string, status: number) {
  return requestClient.post<void>(
    '/system/guide/status',
    buildForm({ id, status }),
    { headers: FORM_HEADERS },
  );
}
