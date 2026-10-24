<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import {
  Button,
  DatePicker,
  Input,
  message,
  Modal as AntModal,
  Select,
  TabPane,
  Tabs,
  Textarea,
  TreeSelect,
} from 'ant-design-vue';

import {
  getCategoryOptionsApi,
  getDataDetailApi,
  getDataListApi,
  getModelListApi,
  getUserOptionsApi,
  saveDataApi,
  updateDataApi,
  type ModelCategoryRow,
  type ModelFieldRow,
  type ModelRow,
  contentNoun,
} from '#/api/core/model';
import { getDictDataByTypeApi } from '#/api/core/dict';
import { aiGenerateApi, getAiStatusApi } from '#/api/core/ai';
import { getAuditSwitchApi } from '#/api/core/audit';
import {
  assignTemplateApi,
  getAssignListApi,
  getDeriveTargetsApi,
  unassignTemplateApi,
} from '#/api/core/template';
import { statusOptionsFor } from '#/constants/content-status';

import ModelFieldItem from './model-field-item.vue';
import RichtextEditor from './richtext-editor.vue';
import DynamicFieldInput from './dynamic-field-input.vue';

/**
 * 动态内容表单（元数据驱动，全部模型共用，无需新增 Vue 页面）。
 *
 * - 选项卡：自定义字段按 tabName 分组（字段管理里配置），组内按 sort 排序；
 * - U3 表单布局：字段级 isForm=0 表单隐藏（不渲染不校验不提交）；
 *   模型级 useContent/useSeo 关闭「详细内容」「SEO 设置」基础页签；
 * - P1 结构层：GROUP 子字段组（JSON 对象）、REPEATER 重复行（可增删行）、
 *   条件显隐（visibleWhen 命中隐藏的字段不渲染、不校验、不提交）；
 * - P2 关系层：m2a 任意关联（"model:id" 复合值，候选项聚合全部模型内容）；
 * - 预览模式（preview: true）：只渲染表单不提交，供「建模后即时预览录入界面」。
 *
 * 2026-10-23 表单改造（调研见 docs/03-报告/2026-10-23-…）：
 * - 校验由「逐个 toast」改为结构化：错误内联显示在字段下方 + 页签红点 +
 *   自动切到第一个出错页签，此前用户在基础页签点保存、错误在 SEO 页签时完全找不到；
 * - GROUP/REPEATER 子字段的必填此前根本不校验，现在纳入；
 * - 三份重复的字段渲染块抽到 model-field-item.vue；
 * - 定时发布时间由手填字符串改为 DatePicker（手填格式错误会让定时任务静默不生效）。
 */
const TAB_BASE = '基础信息';
const TAB_CONTENT = '详细内容';
const TAB_SEO = 'SEO 设置';

const model = ref<null | ModelRow>(null);
const fields = ref<ModelFieldRow[]>([]);
const categories = ref<ModelCategoryRow[]>([]);
const record = ref<null | Record<string, any>>(null);
const activeTab = ref('base');
const values = reactive<Record<string, any>>({ status: '0' });
let onSaved: (() => void) | undefined;
/** 预览模式（field.vue「预览表单」按钮打开） */
const preview = ref(false);

/** 顶层字段（P1：parentId 为空或 0；后端 Long 全局序列化为字符串，须数值化比较） */
const topFields = computed(() =>
  fields.value.filter((f) => !f.parentId || Number(f.parentId) === 0),
);

/** 字段名 → 候选项（relate/relates/m2a/user） */
const optionMap = ref<Record<string, { label: string; value: string }[]>>({});
/** dictType → 候选项 */
const dictOptionMap = ref<Record<string, { label: string; value: string }[]>>({});
/** 字段名 → 分类树 */
const treeDataMap = ref<Record<string, any[]>>({});

// /////////// 校验：结构化（内联 + 页签定位） ///////////

/** 顶层字段错误：fieldName → 文案 */
const errors = reactive<Record<string, string>>({});
/** 子字段错误：父字段名 → { 子字段名: 文案 } */
const childErrors = reactive<Record<string, Record<string, string>>>({});

function clearErrors() {
  for (const k of Object.keys(errors)) delete errors[k];
  for (const k of Object.keys(childErrors)) delete childErrors[k];
}

/** 用户改动该字段时即时消掉红字（不必等下次保存） */
function clearError(key: string) {
  delete errors[key];
}


function isBlank(v: any): boolean {
  return (
    v === undefined ||
    v === null ||
    v === '' ||
    (Array.isArray(v) && v.length === 0)
  );
}

/**
 * 收集全部校验错误，返回第一个出错页签的 key（无错返回 undefined）。
 * 页签 key 与 TabPane 的 key 一致：base / content / seo / c-{自定义页签名}。
 */
function validate(): string | undefined {
  clearErrors();
  const tabsHit: string[] = [];
  const titleLabel = model.value?.titleLabel || '标题';
  if (!String(values.title ?? '').trim()) {
    errors.title = `${titleLabel}不能为空`;
    tabsHit.push('base');
  }
  if (values.status === '4' && !values.publish_time) {
    errors.publish_time = '定时发布必须选择发布时间';
    tabsHit.push('base');
  }
  for (const f of topFields.value) {
    if (f.isForm === 0) continue; // U3 表单隐藏字段不校验
    if (!isVisible(f)) continue; // P1 隐藏字段不校验
    if (f.fieldType === 'formula') continue; // E6 公式字段读侧计算，无表单值

    const tabKey = tabKeyOf(f);
    const v = values[f.fieldName];
    const blank = isBlank(v);

    if (f.isRequired === 1 && blank) {
      errors[f.fieldName] = `${f.fieldLabel}不能为空`;
      tabsHit.push(tabKey);
      continue;
    }
    if (!blank && f.regex && typeof v === 'string') {
      let re: RegExp | undefined;
      try {
        re = new RegExp(f.regex);
      } catch {
        re = undefined; // 字段里存了非法正则时不阻断保存（正则属于提示性约束）
      }
      if (re && !v.match(re)) {
        errors[f.fieldName] = `${f.fieldLabel}格式不正确`;
        tabsHit.push(tabKey);
        continue;
      }
    }
    // P1 结构层子字段：此前漏校验，组内必填形同虚设
    if (f.fieldType === 'group') {
      const group = values[f.fieldName];
      for (const c of childrenOf(f)) {
        if (c.isRequired === 1 && isBlank(group?.[c.fieldName])) {
          childErrors[f.fieldName] ??= {};
          childErrors[f.fieldName]![c.fieldName] = `${c.fieldLabel}不能为空`;
          tabsHit.push(tabKey);
        }
      }
    }
    if (f.fieldType === 'repeater') {
      const rows: Record<string, any>[] = Array.isArray(values[f.fieldName])
        ? values[f.fieldName]
        : [];
      if (f.isRequired === 1 && rows.length === 0) {
        errors[f.fieldName] = `${f.fieldLabel}至少填写一行`;
        tabsHit.push(tabKey);
      }
      for (const c of childrenOf(f)) {
        if (c.isRequired !== 1) continue;
        for (const [idx, row] of rows.entries()) {
          if (isBlank(row?.[c.fieldName])) {
            childErrors[f.fieldName] ??= {};
            childErrors[f.fieldName]![c.fieldName] =
              `第 ${idx + 1} 行的${c.fieldLabel}不能为空`;
            tabsHit.push(tabKey);
            break;
          }
        }
      }
    }
  }
  // 页签顺序与界面一致，报第一个
  const order = tabKeysInOrder();
  for (const k of order) {
    if (tabsHit.includes(k)) return k;
  }
  return undefined;
}

/** 字段所在页签的 TabPane key（tabName=基础信息 的字段并入固定 base 页签） */
function tabKeyOf(f: ModelFieldRow): string {
  const tab = f.tabName || TAB_BASE;
  return tab === TAB_BASE ? 'base' : `c-${tab}`;
}

function tabKeysInOrder(): string[] {
  return ['base', ...customTabs.value.map((t) => `c-${t}`)];
}

/** 各页签的当前错误数（页签红点） */
function tabErrorCount(key: string): number {
  if (key === 'base') {
    let n = errors.title ? 1 : 0;
    if (errors.publish_time) n++;
    for (const f of fieldsOfTab(TAB_BASE)) {
      if (errors[f.fieldName]) n++;
      if (childErrors[f.fieldName]) n += Object.keys(childErrors[f.fieldName]!).length;
    }
    return n;
  }
  const tab = key.replace(/^c-/, '');
  let n = 0;
  for (const f of fieldsOfTab(tab)) {
    if (errors[f.fieldName]) n++;
    if (childErrors[f.fieldName]) n += Object.keys(childErrors[f.fieldName]!).length;
  }
  return n;
}

/**
 * 自定义字段选项卡（保持出现顺序；表单隐藏字段不计入，空页签不渲染）。
 * 布局设计页登记的页签顺序（model.formTabs）优先，未登记的 tabName 按出现顺序追加。
 */
const customTabs = computed(() => {
  const names: string[] = [];
  let configured: string[] = [];
  try {
    configured = model.value?.formTabs ? JSON.parse(model.value.formTabs) : [];
  } catch {
    configured = [];
  }
  for (const tab of configured) {
    if (tab === TAB_BASE || names.includes(tab)) continue;
    names.push(tab);
  }
  for (const f of topFields.value) {
    if (f.isForm === 0) continue;
    const tab = f.tabName || TAB_BASE;
    if (tab === TAB_BASE || names.includes(tab)) continue;
    names.push(tab);
  }
  return names;
});

/** 默认打开页签（布局设计页配置；基础信息→base 固定页签，自定义→c-前缀） */
function resolveInitialTab(): string {
  const def = model.value?.formDefaultTab;
  if (!def) return 'base';
  return def === TAB_BASE ? 'base' : `c-${def}`;
}

function fieldsOfTab(tab: string): ModelFieldRow[] {
  return topFields.value.filter(
    (f) => (f.tabName || TAB_BASE) === tab && f.isForm !== 0,
  );
}

function childrenOf(f: ModelFieldRow): ModelFieldRow[] {
  return fields.value.filter((cf) => cf.parentId === f.id);
}

/** P1 条件显隐求值（与后端 isVisible 同口径；解析失败按显示处理） */
function isVisible(f: ModelFieldRow): boolean {
  if (!f.visibleWhen) return true;
  try {
    const cond = JSON.parse(f.visibleWhen);
    const actual = values[cond.field];
    const present =
      actual !== undefined &&
      actual !== null &&
      actual !== '' &&
      !(Array.isArray(actual) && actual.length === 0);
    const expected = cond.value ?? '';
    switch (cond.op) {
      case 'eq': {
        return present && String(actual).trim() === String(expected);
      }
      case 'neq': {
        return !present || String(actual).trim() !== String(expected);
      }
      case 'in': {
        return (
          present && String(expected).split(',').includes(String(actual).trim())
        );
      }
      case 'notin': {
        return (
          !present ||
          !String(expected).split(',').includes(String(actual).trim())
        );
      }
      case 'empty': {
        return !present;
      }
      case 'notempty': {
        return present;
      }
      default: {
        return true;
      }
    }
  } catch {
    return true;
  }
}

function pick(row: Record<string, any>, name: string) {
  if (row[name] !== undefined && row[name] !== null) return row[name];
  const camel = name
    .split('_')
    .map((p, i) => (i === 0 ? p : p.charAt(0).toUpperCase() + p.slice(1)))
    .join('');
  return row[camel];
}

// /////////// E1/P2 关联字段（relate / relates / m2a）候选项 ///////////

/** 目标模型 code 缓存（避免每个字段重复拉列表） */
let modelCode2Id: Record<string, string> = {};

async function loadRelationOptions(fieldsList: ModelFieldRow[]) {
  const relFields = fieldsList.filter(
    (f) =>
      (['relate', 'relates'].includes(f.fieldType) && f.relateModel) ||
      f.fieldType === 'm2a',
  );
  if (relFields.length === 0) return;
  if (Object.keys(modelCode2Id).length === 0) {
    try {
      const res = await getModelListApi({ p: 1 });
      modelCode2Id = Object.fromEntries(
        (res.list ?? []).map((m) => [m.code, String(m.id)]),
      );
    } catch {
      modelCode2Id = {};
    }
  }
  for (const f of relFields) {
    // P2 m2a：候选项 = 全部模型的内容聚合，value 用 "model:id" 复合串
    if (f.fieldType === 'm2a') {
      const opts: { label: string; value: string }[] = [];
      try {
        const res = await getModelListApi({ p: 1 });
        for (const m of res.list ?? []) {
          try {
            const r = await getDataListApi(m.id, { p: 1, rows: 100 });
            for (const item of r.list ?? []) {
              opts.push({
                label: `${m.name} / ${item.title ?? item.id}`,
                value: `${m.code}:${item.id}`,
              });
            }
          } catch {
            // 单个模型拉取失败跳过
          }
        }
      } catch {
        // 模型列表失败
      }
      optionMap.value[f.fieldName] = opts;
      continue;
    }
    const targetId = modelCode2Id[f.relateModel as string];
    if (!targetId) {
      optionMap.value[f.fieldName] = [];
      continue;
    }
    try {
      const res = await getDataListApi(targetId, { p: 1, rows: 200 });
      optionMap.value[f.fieldName] = (res.list ?? []).map((r) => ({
        label: String(r.title ?? r.id),
        value: String(r.id),
      }));
    } catch {
      optionMap.value[f.fieldName] = [];
    }
  }
}

// /////////// G22 AI 内容助手（输出仅回填表单，人工审核后保存） ///////////

/** 当前正在跑的 AI 任务（此前三按钮共用一个 aiBusy，点一个三个全转圈） */
const aiTask = ref<'' | 'keywords' | 'summary' | 'title'>('');
const aiBusy = computed(() => aiTask.value !== '');
const aiConfigured = ref(false);
const titleSuggestions = ref<string[]>([]);

async function checkAi() {
  try {
    const res = await getAiStatusApi();
    aiConfigured.value = Boolean((res as any)?.configured);
  } catch {
    aiConfigured.value = false;
  }
}

function adoptTitle(t: string) {
  values.title = t;
  titleSuggestions.value = [];
  message.success('已采用');
}

async function runAi(task: 'keywords' | 'summary' | 'title') {
  if (aiBusy.value) return;
  const content = values.content ?? '';
  const title = values.title ?? '';
  if (!content && !title) {
    message.warning('请先填写正文或标题');
    return;
  }
  aiTask.value = task;
  try {
    const res = (await aiGenerateApi({ content, task, title })) as any;
    if (typeof res !== 'string' || !res.trim()) {
      message.warning('AI 未返回内容');
      return;
    }
    if (task === 'summary') {
      values.description = res.trim();
      message.success('摘要已填入「描述」（SEO 页签可修改）');
    } else if (task === 'keywords') {
      values.keywords = res.trim();
      message.success('关键词已填入「关键词」');
    } else {
      titleSuggestions.value = res
        .split('\n')
        .map((x: string) => x.trim().replace(/^\d+[.、)]\s*/, ''))
        .filter(Boolean);
      if (titleSuggestions.value.length === 0) {
        message.warning('AI 未返回标题建议');
      }
    }
  } catch (e: any) {
    message.error(e?.message ?? 'AI 调用失败');
  } finally {
    aiTask.value = '';
  }
}

// /////////// B1 字段绑定数据源：user 候选 / category 分类树 ///////////

/** 各绑定模型的分类树：key = 模型 code（本模型也入表） */
const categoryTrees = ref<Record<string, any[]>>({});

/** 平铺分类 → 树（fatherId 归组；游离节点挂根） */
function buildCategoryTree(
  rows: { fatherId: string; id: string; name: string }[],
): any[] {
  const nodes = new Map<string, any>();
  for (const r of rows) {
    nodes.set(r.id, { value: r.id, label: r.name, children: [] });
  }
  const roots: any[] = [];
  for (const r of rows) {
    const node = nodes.get(r.id);
    const parent = nodes.get(r.fatherId);
    if (parent && r.fatherId !== r.id) {
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  }
  return roots;
}

function stripTree(nodes: any[]): any[] {
  return (nodes ?? []).map((n) => ({
    value: n.value,
    label: n.label,
    children: n.children?.length ? stripTree(n.children) : undefined,
  }));
}

/** 本模型分类树（固定「分类」TreeSelect 与绑本模型的 category 字段共用） */
function ownTree(): any[] {
  const code = model.value?.code ?? '';
  if (code && !categoryTrees.value[code]) {
    categoryTrees.value[code] = stripTree(
      buildCategoryTree((categories.value ?? []) as any),
    );
  }
  return code ? (categoryTrees.value[code] ?? []) : [];
}

async function loadBoundOptions(fieldsList: ModelFieldRow[], ownCode: string) {
  ownTree();
  // user 字段候选：一次拉满（管理端量级可用）
  if (fieldsList.some((f) => f.fieldType === 'user')) {
    try {
      const opts = ((await getUserOptionsApi()) ?? []).map((o) => ({
        label: o.label,
        value: o.id,
      }));
      for (const f of fieldsList) {
        if (f.fieldType === 'user') optionMap.value[f.fieldName] = opts;
      }
    } catch {
      // 用户候选失败不阻断
    }
  }
  // category 字段：按绑定模型取分类（本模型复用 categories props）
  const needCodes = new Set<string>();
  for (const f of fieldsList) {
    if (f.fieldType !== 'category') continue;
    needCodes.add(f.relateModel || ownCode);
  }
  for (const code of needCodes) {
    if (categoryTrees.value[code]) continue;
    if (model.value && code === model.value.code) {
      continue; // 已在 ownTree 里
    }
    try {
      const rows = (await getCategoryOptionsApi(code)) ?? [];
      categoryTrees.value[code] = stripTree(buildCategoryTree(rows as any));
    } catch {
      categoryTrees.value[code] = [];
    }
  }
  // 分类树按字段登记（子字段也要能拿到）
  for (const f of fieldsList) {
    if (f.fieldType !== 'category') continue;
    const code = f.relateModel || ownCode;
    treeDataMap.value[f.fieldName] = categoryTrees.value[code] ?? [];
  }
}

// /////////// 字典绑定字段（若依式 dictType）候选项 ///////////

async function loadDictOptions(fieldsList: ModelFieldRow[]) {
  const keys = new Set<string>();
  for (const f of fieldsList) {
    if (f.dictType) keys.add(f.dictType);
  }
  for (const key of keys) {
    if (dictOptionMap.value[key]) continue;
    try {
      const rows = (await getDictDataByTypeApi(key)) ?? [];
      dictOptionMap.value[key] = rows.map((r) => ({
        label: r.dictLabel,
        value: r.dictValue,
      }));
    } catch {
      dictOptionMap.value[key] = [];
    }
  }
}

// /////////// P7 内容模板指派（§8.3，对应 WP 后台 Page Template 下拉） ///////////

/** 当前内容被指派的详情模板（undefined = 未指派，走层级默认） */
const contentTemplate = ref<undefined | string>();
/** 可选的 detail-* 模板槽位（来自派生清单） */
const templateOptions = ref<{ label: string; value: string }[]>([]);

async function loadTemplateAssign() {
  if (!record.value || !model.value) return;
  try {
    const [assignRes, slotRes] = await Promise.all([
      getAssignListApi('CONTENT'),
      getDeriveTargetsApi(),
    ]);
    const mine = (assignRes.list ?? []).find(
      (r) =>
        r.pageType === 'DETAIL' &&
        r.targetId === String(record.value?.id ?? ''),
    );
    contentTemplate.value = mine?.template;
    const seen = new Set<string>();
    const options: { label: string; value: string }[] = [];
    for (const s of slotRes.slots ?? []) {
      if (!s.target.startsWith('detail-') || seen.has(s.target)) continue;
      seen.add(s.target);
      options.push({ label: `${s.label} · ${s.target}`, value: s.target });
    }
    templateOptions.value = options;
  } catch {
    templateOptions.value = [];
  }
}

async function onTemplateChange(value: unknown) {
  if (!record.value) return;
  const tpl = typeof value === 'string' && value ? value : undefined;
  const targetId = String(record.value.id);
  try {
    if (tpl) {
      await assignTemplateApi('CONTENT', targetId, 'DETAIL', tpl);
      message.success(`已指派：该内容详情页使用 ${tpl}`);
    } else {
      await unassignTemplateApi('CONTENT', targetId, 'DETAIL');
      message.success('已取消指派，恢复层级默认模板');
    }
  } catch {
    message.error('指派失败，请重试');
  }
}

// /////////// 保存 ///////////

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    const firstBadTab = validate();
    if (firstBadTab) {
      activeTab.value = firstBadTab;
      message.warning('请先修正标红的字段');
      return;
    }
    modalApi.lock();
    try {
      const payload: Record<string, any> = {
        modelId: model.value?.id,
        title: values.title,
        status: values.status ?? '0',
      };
      const fixedKeys = [
        'categoryId',
        'thumbnail',
        'content',
        'keywords',
        'description',
      ];
      for (const key of fixedKeys) {
        if (values[key]) payload[key] = values[key];
      }
      // U3 模型级页签关闭时，不提交对应固定列（避免以载入值覆盖）
      if (model.value?.useContent === 0) {
        delete payload.content;
      }
      if (model.value?.useSeo === 0) {
        delete payload.keywords;
        delete payload.description;
      }
      for (const f of topFields.value) {
        if (!isVisible(f)) continue; // P1 隐藏字段不提交
        if (f.isForm === 0) continue; // U3 表单隐藏字段不提交
        if (f.fieldType === 'formula') continue; // E6 公式字段不提交
        const v = values[f.fieldName];
        // P2 m2a：显式提交（含空数组 = 清空关系），把 "model:id" 拆回 [{model,id}]
        if (f.fieldType === 'm2a') {
          if (Array.isArray(v)) {
            payload[f.fieldName] = JSON.stringify(
              v.map((s: string) => {
                const idx = String(s).indexOf(':');
                return {
                  model: String(s).slice(0, idx),
                  id: String(s).slice(idx + 1),
                };
              }),
            );
          }
          continue;
        }
        if (v === undefined || v === null || v === '') continue;
        payload[f.fieldName] =
          Array.isArray(v) || typeof v === 'object' ? JSON.stringify(v) : String(v);
      }
      if (values.publish_time) payload.publish_time = values.publish_time;
      if (record.value) {
        await updateDataApi({ ...payload, id: record.value.id });
      } else {
        await saveDataApi(payload);
      }
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '内容',
});

onMounted(async () => {
  const data = modalApi.getData() as
    | {
        categories?: ModelCategoryRow[];
        fields?: ModelFieldRow[];
        model?: ModelRow;
        onSaved?: () => void;
        preview?: boolean;
        record?: Record<string, any>;
      }
    | undefined;
  onSaved = data?.onSaved;
  preview.value = Boolean(data?.preview);
  if (data?.fields) fields.value = data.fields;
  if (data?.categories) categories.value = data.categories;
  if (data?.model) model.value = data.model;
  // E1/P2：关联字段候选项（目标模型内容列表），必须在回显取值之前就绪
  await loadRelationOptions(fields.value);

  if (data?.record) {
    record.value = data.record;
    // 编辑：详情接口给的是动态表原始行
    let row: Record<string, any> = {};
    try {
      row = (await getDataDetailApi(model.value!.id, data.record.id)) ?? {};
    } catch (e: any) {
      // 此前失败时静默降级到列表行（投影列，不含正文/长字段）→ 保存即丢内容。
      // 现在明确报错，宁可不打开也不让用户以为内容还在。
      message.error(e?.message ?? '内容详情加载失败，已取消编辑');
      modalApi.close();
      return;
    }
    modalApi.setState({ title: `编辑${contentNoun(model.value?.titleLabel)}` });
    values.title = pick(row, 'title') ?? '';
    values.content = pick(row, 'content') ?? '';
    values.keywords = pick(row, 'keywords') ?? '';
    values.description = pick(row, 'description') ?? '';
    values.categoryId = pick(row, 'category_id') || undefined;
    values.thumbnail = pick(row, 'thumbnail') || '';
    values.status = String(row.status ?? '0');
    for (const f of fields.value) {
      let v = pick(row, f.fieldName) ?? '';
      if (f.fieldType === 'checkbox' || f.fieldType === 'relates') {
        try {
          v = v ? JSON.parse(String(v)) : [];
        } catch {
          v = v ? String(v).split(',') : [];
        }
      } else if (f.fieldType === 'group') {
        try {
          v = v ? JSON.parse(String(v)) : {};
        } catch {
          v = {};
        }
      } else if (f.fieldType === 'repeater') {
        try {
          v = v ? JSON.parse(String(v)) : [];
        } catch {
          v = [];
        }
      } else if (f.fieldType === 'm2a') {
        const list = row[`${f.fieldName}List`];
        v = Array.isArray(list)
          ? list.map((x: any) => `${x.model}:${x.id}`)
          : [];
      } else if (f.fieldType === 'number' || f.fieldType === 'decimal') {
        v = v === '' || v === null ? undefined : Number(v);
      } else if (
        (f.fieldType === 'image' || f.fieldType === 'file') &&
        typeof v === 'object'
      ) {
        v = '';
      } else if (
        f.fieldType === 'relate' ||
        f.fieldType === 'user' ||
        f.fieldType === 'category'
      ) {
        // E1 单值关联 / B1 绑定实体：Select/TreeSelect 的 value 统一为字符串，避免雪花 ID 精度问题
        v = v === '' || v === null ? undefined : String(v);
      } else if (f.fieldType === 'switch') {
        v = String(v) === '1' || String(v).toLowerCase() === 'true';
      } else if (f.fieldType === 'rating') {
        v = v === '' || v === null ? undefined : Number(v);
      }
      values[f.fieldName] = v;
    }
  } else {
    modalApi.setState({
      title: preview.value
        ? `预览录入界面（${model.value?.name || ''}）`
        : `添加${contentNoun(model.value?.titleLabel)}`,
    });
    // 新建默认状态跟随站点的「先审后发」开关：
    // 关着开关时后台亲手新建的内容还要再手动改成「已发布」才可见，此前是纯浪费操作
    try {
      values.status = Number(await getAuditSwitchApi()) === 1 ? '0' : '1';
    } catch {
      values.status = '0';
    }
    for (const f of fields.value) {
      if (f.fieldType === 'repeater' || f.fieldType === 'm2a') {
        values[f.fieldName] = [];
      } else if (f.fieldType === 'group') {
        const obj: Record<string, any> = {};
        for (const child of childrenOf(f)) obj[child.fieldName] = undefined;
        values[f.fieldName] = obj;
      } else {
        values[f.fieldName] =
          f.fieldType === 'checkbox'
            ? []
            : (f.defaultValue ??
              (f.fieldType === 'number' || f.fieldType === 'decimal'
                ? undefined
                : ''));
      }
    }
  }
  activeTab.value = resolveInitialTab();
  loadBoundOptions(fields.value, model.value?.code ?? '');
  loadDictOptions(fields.value);
  checkAi();
  loadTemplateAssign();
});
</script>

<template>
  <Modal>
    <Tabs v-model:active-key="activeTab">
      <TabPane key="base">
        <template #tab>
          <span>
            {{ TAB_BASE }}
            <span
              v-if="tabErrorCount('base')"
              class="ml-1 inline-block h-2 w-2 rounded-full bg-red-500 align-middle"
              :title="`${tabErrorCount('base')} 项待修正`"
            />
          </span>
        </template>
        <div class="grid grid-cols-2 gap-x-5 gap-y-3">
          <div class="col-span-2">
            <label class="mb-1 block text-sm">
              <span class="text-red-500">*</span>
              {{ model?.titleLabel || '标题' }}
            </label>
            <Input
              v-model:value="values.title"
              :maxlength="250"
              :status="errors.title ? 'error' : undefined"
              @update:value="clearError('title')"
            />
            <div v-if="errors.title" class="mt-0.5 text-xs text-red-500">
              {{ errors.title }}
            </div>
            <div v-if="aiConfigured" class="mt-1 flex flex-wrap items-center gap-2">
              <Button
                :loading="aiTask === 'title'"
                size="small"
                @click="runAi('title')"
              >
                AI 标题建议
              </Button>
            </div>
          </div>

          <div v-if="categories.length > 0">
            <label class="mb-1 block text-sm">分类</label>
            <TreeSelect
              v-model:value="values.categoryId"
              allow-clear
              class="w-full"
              :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
              :placeholder="`选择${model?.name || ''}分类`"
              show-search
              :tree-data="ownTree()"
              tree-default-expand-all
              tree-node-filter-prop="label"
            />
          </div>

          <div>
            <label class="mb-1 block text-sm">状态</label>
            <Select
              v-model:value="values.status"
              class="w-full"
              :options="statusOptionsFor(!!record)"
              @change="clearError('publish_time')"
            />
          </div>

          <div v-if="values.status === '4'" class="col-span-2">
            <label class="mb-1 block text-sm">
              <span class="text-red-500">*</span>
              定时发布时间（到点自动发布）
            </label>
            <!-- 此前是手填 "yyyy-MM-dd HH:mm:ss"：写错格式不报错，定时任务扫不到直接静默失效 -->
            <DatePicker
              v-model:value="values.publish_time"
              class="w-full"
              :disabled-date="(d: any) => d && d.valueOf() < Date.now() - 60_000"
              format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择发布时间（须晚于当前时间）"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              @change="clearError('publish_time')"
            />
            <div v-if="errors.publish_time" class="mt-0.5 text-xs text-red-500">
              {{ errors.publish_time }}
            </div>
            <div v-else class="mt-0.5 text-xs text-gray-400">
              保存后由定时任务到点自动转「已发布」
            </div>
          </div>

          <div class="col-span-2">
            <label class="mb-1 block text-sm">封面图</label>
            <DynamicFieldInput
              v-model="values.thumbnail"
              :field="{
                fieldName: 'thumbnail',
                fieldLabel: '封面图',
                fieldType: 'image',
              } as any"
              :options="[]"
            />
          </div>

          <div v-if="record" class="col-span-2">
            <label class="mb-1 block text-sm">内容模板（详情页版式）</label>
            <Select
              v-model:value="contentTemplate"
              allow-clear
              class="w-full"
              :options="templateOptions"
              placeholder="不选 = 按模板层级自动匹配"
              @change="onTemplateChange"
            />
            <div class="mt-0.5 text-xs text-gray-400">
              指派优先级最高；清空即恢复 list/detail 层级默认（§5）
            </div>
          </div>
        </div>

        <!-- tabName=基础信息 的自定义字段并入本页签（与自定义页签网格同构） -->
        <div
          v-if="fieldsOfTab(TAB_BASE).some((x) => x.fieldType !== 'formula')"
          class="mt-4 grid grid-cols-2 gap-x-5 gap-y-3 border-t border-gray-100 pt-4"
        >
          <ModelFieldItem
            v-for="f in fieldsOfTab(TAB_BASE).filter((x) => x.fieldType !== 'formula')"
            v-show="isVisible(f)"
            :key="f.id"
            v-model="values[f.fieldName]"
            :child-errors="childErrors[f.fieldName]"
            :children="childrenOf(f)"
            :dict-option-map="dictOptionMap"
            :error="errors[f.fieldName]"
            :field="f"
            :option-map="optionMap"
            :tree-data-map="treeDataMap"
          />
        </div>
      </TabPane>

      <TabPane v-if="model?.useContent !== 0" key="content">
        <template #tab>
          {{ TAB_CONTENT }}
        </template>
        <div class="mb-1 text-xs text-gray-400">
          正文（富文本，编辑器图片上传走附件通道）
        </div>
        <RichtextEditor v-model="values.content" />
        <div v-if="aiConfigured" class="mt-2 flex flex-wrap items-center gap-2">
          <Button
            :loading="aiTask === 'summary'"
            size="small"
            @click="runAi('summary')"
          >
            AI 生成摘要
          </Button>
          <Button
            :loading="aiTask === 'keywords'"
            size="small"
            @click="runAi('keywords')"
          >
            AI 提取关键词
          </Button>
          <span class="text-xs text-gray-400">
            生成结果回填到「SEO 设置」页签
          </span>
        </div>
      </TabPane>

      <TabPane v-for="tab in customTabs" :key="`c-${tab}`">
        <template #tab>
          <span>
            {{ tab }}
            <span
              v-if="tabErrorCount(`c-${tab}`)"
              class="ml-1 inline-block h-2 w-2 rounded-full bg-red-500 align-middle"
              :title="`${tabErrorCount(`c-${tab}`)} 项待修正`"
            />
          </span>
        </template>
        <div class="grid grid-cols-2 gap-x-5 gap-y-3">
          <ModelFieldItem
            v-for="f in fieldsOfTab(tab).filter((x) => x.fieldType !== 'formula')"
            v-show="isVisible(f)"
            :key="f.id"
            v-model="values[f.fieldName]"
            :child-errors="childErrors[f.fieldName]"
            :children="childrenOf(f)"
            :dict-option-map="dictOptionMap"
            :error="errors[f.fieldName]"
            :field="f"
            :option-map="optionMap"
            :tree-data-map="treeDataMap"
          />
        </div>
      </TabPane>

      <TabPane v-if="model?.useSeo !== 0" key="seo">
        <template #tab>
          {{ TAB_SEO }}
        </template>
        <div class="grid grid-cols-1 gap-y-3">
          <div>
            <label class="mb-1 block text-sm">关键词（SEO）</label>
            <Input
              v-model:value="values.keywords"
              placeholder="英文逗号分隔"
            />
          </div>
          <div>
            <label class="mb-1 block text-sm">描述（SEO）</label>
            <Textarea v-model:value="values.description" :rows="3" />
          </div>
        </div>
      </TabPane>
    </Tabs>
  </Modal>

  <!-- G22 AI 标题建议 -->
  <AntModal
    :footer="null"
    :open="titleSuggestions.length > 0"
    title="AI 标题建议（点击采用）"
    @cancel="titleSuggestions = []"
  >
    <div class="space-y-2">
      <div
        v-for="(t, i) in titleSuggestions"
        :key="i"
        class="cursor-pointer rounded border p-2 hover:bg-blue-50"
        @click="adoptTitle(t)"
      >
        {{ t }}
      </div>
    </div>
  </AntModal>
</template>
