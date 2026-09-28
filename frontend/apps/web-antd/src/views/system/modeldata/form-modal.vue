<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Button, Input, message, Select, TabPane, Tabs, Textarea } from 'ant-design-vue';

import {
  getDataDetailApi,
  getDataListApi,
  getModelListApi,
  saveDataApi,
  updateDataApi,
  type ModelCategoryRow,
  type ModelFieldRow,
  type ModelRow,
} from '#/api/core/model';
import {
  assignTemplateApi,
  getAssignListApi,
  getDeriveTargetsApi,
  unassignTemplateApi,
} from '#/api/core/template';

import DynamicFieldInput from './dynamic-field-input.vue';
import RichtextEditor from './richtext-editor.vue';

/**
 * 动态内容表单（元数据驱动，全部模型共用，无需新增 Vue 页面）。
 * - 选项卡：自定义字段按 tabName 分组（字段管理里配置），组内按 sort 排序；
 * - P1 结构层：GROUP 子字段组（JSON 对象）、REPEATER 重复行（可增删行）、
 *   条件显隐（visibleWhen 命中隐藏的字段不渲染、不校验、不提交）；
 * - P2 关系层：m2a 任意关联（"model:id" 复合值，候选项聚合全部模型内容）；
 * - 预览模式（preview: true）：只渲染表单不提交，供「建模后即时预览录入界面」。
 */
const TAB_BASE = '基础信息';
const TAB_CONTENT = '详细内容';
const TAB_SEO = 'SEO 设置';

const model = ref<null | ModelRow>(null);
const fields = ref<ModelFieldRow[]>([]);
const categories = ref<ModelCategoryRow[]>([]);
const record = ref<null | Record<string, any>>(null);
const activeTab = ref(TAB_BASE);
const values = reactive<Record<string, any>>({ status: '0' });
let onSaved: (() => void) | undefined;
/** 预览模式（field.vue「预览表单」按钮打开） */
const preview = ref(false);

/** 顶层字段（P1：parentId 为空或 0） */
const topFields = computed(() =>
  fields.value.filter((f) => !f.parentId || f.parentId === 0),
);

/** 自定义字段的选项卡名（保持首次出现顺序） */
const customTabs = computed(() => {
  const names: string[] = [];
  for (const f of topFields.value) {
    const tab = f.tabName || TAB_BASE;
    if (!names.includes(tab)) names.push(tab);
  }
  return names;
});

function fieldsOfTab(tab: string): ModelFieldRow[] {
  return topFields.value.filter((f) => (f.tabName || TAB_BASE) === tab);
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
          present &&
          String(expected)
            .split(',')
            .includes(String(actual).trim())
        );
      }
      case 'notin': {
        return (
          !present ||
          !String(expected)
            .split(',')
            .includes(String(actual).trim())
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

/** fieldName → 候选选项（目标模型的内容列表） */
const relationOptions = ref<Record<string, { label: string; value: string }[]>>({});
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
      relationOptions.value[f.fieldName] = opts;
      continue;
    }
    const targetId = modelCode2Id[f.relateModel as string];
    if (!targetId) {
      relationOptions.value[f.fieldName] = [];
      continue;
    }
    try {
      const res = await getDataListApi(targetId, { p: 1, rows: 200 });
      relationOptions.value[f.fieldName] = (res.list ?? []).map((r) => ({
        label: String(r.title ?? r.id),
        value: String(r.id),
      }));
    } catch {
      relationOptions.value[f.fieldName] = [];
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

const [Modal, modalApi] = useEditDrawer({
  async onConfirm() {
    if (!String(values.title ?? '').trim()) {
      message.warning(`${model.value?.titleLabel || '标题'}不能为空`);
      return;
    }
    if (values.status === '4' && !values.publish_time) {
      message.warning('定时发布必须填写发布时间');
      return;
    }
    for (const f of topFields.value) {
      if (!isVisible(f)) continue; // P1 隐藏字段不校验
      const v = values[f.fieldName];
      const blank =
        v === undefined ||
        v === null ||
        v === '' ||
        (Array.isArray(v) && v.length === 0) ||
        (f.fieldType === 'repeater' && Array.isArray(v) && v.length === 0);
      if (f.isRequired === 1 && blank) {
        message.warning(`${f.fieldLabel}不能为空`);
        return;
      }
      if (!blank && f.regex && !String(v).match(new RegExp(f.regex))) {
        message.warning(`${f.fieldLabel}格式不正确`);
        return;
      }
    }
    modalApi.lock();
    try {
      const payload: Record<string, any> = {
        modelId: model.value?.id,
        title: values.title,
        status: values.status ?? '0',
      };
      for (const key of [
        'categoryId',
        'thumbnail',
        'content',
        'keywords',
        'description',
      ]) {
        if (values[key]) payload[key] = values[key];
      }
      for (const f of topFields.value) {
        if (!isVisible(f)) continue; // P1 隐藏字段不提交
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

/** REPEATER 加一行 */
function addRepeaterRow(f: ModelFieldRow) {
  if (!Array.isArray(values[f.fieldName])) values[f.fieldName] = [];
  const row: Record<string, any> = {};
  for (const child of childrenOf(f)) row[child.fieldName] = undefined;
  values[f.fieldName].push(row);
}

function removeRepeaterRow(f: ModelFieldRow, index: number) {
  if (Array.isArray(values[f.fieldName])) values[f.fieldName].splice(index, 1);
}

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
    } catch {
      row = data.record; // 列表行兜底
    }
    modalApi.setState({ title: `编辑${model.value?.titleLabel || '内容'}` });
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
        // P1 字段组：JSON 对象
        try {
          v = v ? JSON.parse(String(v)) : {};
        } catch {
          v = {};
        }
      } else if (f.fieldType === 'repeater') {
        // P1 重复行：JSON 数组
        try {
          v = v ? JSON.parse(String(v)) : [];
        } catch {
          v = [];
        }
      } else if (f.fieldType === 'm2a') {
        // P2 任意关联：读展开的 {field}List，转 "model:id" 复合值
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
      } else if (f.fieldType === 'relate') {
        // E1 单值关联：Select 的 value 统一为字符串，避免雪花 ID 精度问题
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
        : `添加${model.value?.titleLabel || '内容'}`,
    });
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
  loadTemplateAssign();
});
</script>

<template>
  <Modal>
    <Tabs v-model:active-key="activeTab">
      <TabPane :tab="TAB_BASE" key="base">
        <div class="grid grid-cols-2 gap-x-5 gap-y-3">
          <div class="col-span-2">
            <div class="mb-1 text-sm">
              <span class="text-red-500">*</span>
              {{ model?.titleLabel || '标题' }}
            </div>
            <Input v-model:value="values.title" :maxlength="250" />
          </div>
          <div v-if="categories.length > 0">
            <div class="mb-1 text-sm">分类</div>
            <Select
              v-model:value="values.categoryId"
              allow-clear
              class="w-full"
              :options="categories.map((c) => ({ label: c.name, value: c.id }))"
              placeholder="选择分类"
            />
          </div>
          <div>
            <div class="mb-1 text-sm">状态</div>
            <Select
              v-model:value="values.status"
              class="w-full"
              :options="[
                { label: '待审核', value: '0' },
                { label: '发布', value: '1' },
                { label: '未通过', value: '2' },
                { label: '定时发布', value: '4' },
              ]"
            />
          </div>
          <div class="col-span-2">
            <div class="mb-1 text-sm">内容模板（详情页版式）</div>
            <Select
              v-if="record"
              v-model:value="contentTemplate"
              allow-clear
              class="w-full"
              :loading="false"
              :options="templateOptions"
              placeholder="不选 = 按模板层级自动匹配"
              @change="onTemplateChange"
            />
            <div v-else class="text-xs text-gray-400">
              保存内容后可在这里指派专属模板（WP Page Template 式，立即生效）
            </div>
            <div class="mt-0.5 text-xs text-gray-400">
              指派优先级最高；清空即恢复 list/detail 层级默认（§5）
            </div>
          </div>
          <div class="col-span-2">
            <div class="mb-1 text-sm">封面图</div>
            <DynamicFieldInput
              v-model="values.thumbnail"
              :field="{ fieldName: 'thumbnail', fieldLabel: '封面图', fieldType: 'image' } as any"
            />
          </div>
          <div class="col-span-2" v-if="values.status === '4'">
            <div class="mb-1 text-sm">
              <span class="text-red-500">*</span>
              定时发布时间（到点自动发布）
            </div>
            <Input v-model:value="values.publish_time" placeholder="yyyy-MM-dd HH:mm:ss" />
          </div>
        </div>
      </TabPane>

      <TabPane :tab="TAB_CONTENT" key="content">
        <div class="mb-1 text-xs text-gray-400">
          正文（富文本，编辑器图片上传走附件通道）
        </div>
        <RichtextEditor v-model:model-value="values.content" />
        <div class="mt-3">
          <Button v-if="!values.content" @click="() => (values.content = ' ')">
            初始化编辑器
          </Button>
        </div>
      </TabPane>

      <TabPane
        v-for="tab in customTabs"
        :key="`c-${tab}`"
        :tab="tab"
      >
        <div class="grid grid-cols-2 gap-x-5 gap-y-3">
          <div
            v-for="f in fieldsOfTab(tab)"
            v-show="isVisible(f)"
            :key="f.id"
            :class="['textarea', 'images', 'files', 'relates', 'image_url', 'repeater', 'm2a'].includes(f.fieldType) ? 'col-span-2' : ''"
          >
            <div class="mb-1 text-sm">
              <span v-if="f.isRequired === 1" class="text-red-500">*</span>
              {{ f.fieldLabel }}
              <span class="ml-1 text-xs text-gray-400">{{ f.fieldName }}</span>
            </div>

            <!-- P1 GROUP 字段组：子字段网格 -->
            <div
              v-if="f.fieldType === 'group'"
              class="rounded border border-gray-200 p-3"
            >
              <div class="grid grid-cols-2 gap-x-4 gap-y-3">
                <div v-for="child in childrenOf(f)" :key="child.id">
                  <div class="mb-1 text-xs text-gray-500">
                    <span v-if="child.isRequired === 1" class="text-red-500">*</span>
                    {{ child.fieldLabel }}
                    <span class="ml-1 text-gray-300">{{ child.fieldName }}</span>
                  </div>
                  <DynamicFieldInput
                    v-model="values[f.fieldName][child.fieldName]"
                    :field="child"
                  />
                </div>
                <div v-if="childrenOf(f).length === 0" class="text-xs text-gray-400">
                  该字段组还没有子字段（在「字段管理」里点「子字段」添加）
                </div>
              </div>
            </div>

            <!-- P1 REPEATER 重复行：可增删行 -->
            <div v-else-if="f.fieldType === 'repeater'" class="space-y-3">
              <div
                v-for="(rowItem, rowIndex) in values[f.fieldName] || []"
                :key="rowIndex"
                class="relative rounded border border-gray-200 p-3"
              >
                <Button
                  class="absolute right-2 top-2"
                  danger
                  size="small"
                  type="link"
                  @click="removeRepeaterRow(f, Number(rowIndex))"
                >
                  删除本行
                </Button>
                <div class="grid grid-cols-2 gap-x-4 gap-y-3">
                  <div v-for="child in childrenOf(f)" :key="child.id">
                    <div class="mb-1 text-xs text-gray-500">
                      <span v-if="child.isRequired === 1" class="text-red-500">*</span>
                      {{ child.fieldLabel }}
                      <span class="ml-1 text-gray-300">{{ child.fieldName }}</span>
                    </div>
                    <DynamicFieldInput
                      v-model="rowItem[child.fieldName]"
                      :field="child"
                    />
                  </div>
                </div>
              </div>
              <Button class="w-full" size="small" @click="addRepeaterRow(f)">
                + 添加一行（{{ (values[f.fieldName] || []).length }} 行）
              </Button>
            </div>

            <!-- 其余类型走通用控件（relate/relates/m2a 候选项由父组件传入） -->
            <DynamicFieldInput
              v-else
              v-model="values[f.fieldName]"
              :field="f"
              :options="relationOptions[f.fieldName] ?? []"
            />
            <div v-if="f.tips" class="mt-0.5 text-xs text-gray-400">
              {{ f.tips }}
            </div>
          </div>
        </div>
      </TabPane>

      <TabPane :tab="TAB_SEO" key="seo">
        <div class="grid grid-cols-1 gap-y-3">
          <div>
            <div class="mb-1 text-sm">关键词（SEO）</div>
            <Input v-model:value="values.keywords" placeholder="英文逗号分隔" />
          </div>
          <div>
            <div class="mb-1 text-sm">描述（SEO）</div>
            <Textarea v-model:value="values.description" :rows="3" />
          </div>
        </div>
      </TabPane>
    </Tabs>
  </Modal>
</template>
