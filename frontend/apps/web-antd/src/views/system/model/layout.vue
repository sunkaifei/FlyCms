<script lang="ts" setup>
import type { ModelFieldRow, ModelRow } from '#/api/core/model';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';
import { useAccessStore } from '@vben/stores';

import {
  Button,
  Input,
  message,
  Modal,
  Select,
  Switch,
  Tag,
  Tooltip,
} from 'ant-design-vue';
import { useRoute, useRouter } from 'vue-router';

import {
  getFormMetaApi,
  updateFieldApi,
  updateModelApi,
} from '#/api/core/model';

/**
 * 发布页面布局设计（每模型一个设计页）：
 * - 页签管理：新增/重命名/排序/删除页签，设置发布表单默认打开的页签；
 * - 字段编排：字段在页签间的归属（下拉移动）、同页签内顺序（↑↓）、表单显隐；
 * - 保存 = 变更字段逐个整行回传（updateField 部分列无条件更新）+ 模型页签配置。
 * 字段的新增/删除/类型仍走「字段管理」。
 */
const BASE_TAB = '基础信息';

const route = useRoute();
const router = useRouter();
const { hasAccessByCodes } = useAccess();
const accessStore = useAccessStore();
const accessCodes = computed(() => accessStore.accessCodes as string[]);

/** 通配权限码匹配（modelField 节点是 /api/system/modelField/* 尾星号，精确匹配会漏判） */
function canCode(need: string): boolean {
  return hasAccessByCodes([need]) || accessCodes.value.some(
    (c) => c === need || (c.endsWith('*') && need.startsWith(c.slice(0, -1))),
  );
}
const canSave = canCode('/api/system/modelField/save') && canCode('/api/system/model/update');

const model = ref<null | ModelRow>(null);
/** 顶层字段（有序） */
const fields = ref<ModelFieldRow[]>([]);
/** 自定义页签（有序，不含固定基础页签） */
const tabs = ref<string[]>([]);
/** 默认打开的页签名（基础信息 或 自定义页签名） */
const defaultTab = ref<string>(BASE_TAB);
const loading = ref(false);
const saving = ref(false);
/** 原始快照（变更检测 + 整行回传） */
const originals = ref<Map<number, ModelFieldRow>>(new Map());
const newTabName = ref('');

const modelId = computed(() => String(route.params.modelId ?? ''));

/** 页签卡片视图：固定基础页签在最前，其后按 tabs 顺序 */
const groups = computed(() => {
  const out: { name: string; fixed: boolean; rows: ModelFieldRow[] }[] = [
    { name: BASE_TAB, fixed: true, rows: [] },
  ];
  for (const t of tabs.value) {
    out.push({ name: t, fixed: false, rows: [] });
  }
  for (const f of fields.value) {
    const tab = f.tabName || BASE_TAB;
    const g = out.find((x) => x.name === tab);
    if (g) {
      g.rows.push(f);
    } else {
      // 保险：字段页签不在列表里（并发修改等），落到基础信息
      out[0]!.rows.push(f);
    }
  }
  return out;
});

const allTabOptions = computed(() => [
  { label: BASE_TAB, value: BASE_TAB },
  ...tabs.value.map((t) => ({ label: t, value: t })),
]);

function snapshot() {
  originals.value = new Map(fields.value.map((f) => [f.id, { ...f }]));
}

async function load() {
  if (!modelId.value) return;
  loading.value = true;
  try {
    const meta = await getFormMetaApi(modelId.value);
    model.value = meta.model ?? null;
    fields.value = (meta.fields ?? [])
      .filter((f) => !f.parentId || Number(f.parentId) === 0)
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
      .map((f) => ({ ...f, tabName: f.tabName || BASE_TAB }));
    // 页签顺序：模型配置优先；未登记的字段页签补在后面（首次使用零配置可用）
    let configured: string[] = [];
    try {
      configured = model.value?.formTabs ? JSON.parse(model.value.formTabs) : [];
    } catch {
      configured = [];
    }
    const seen = new Set<string>();
    tabs.value = configured
      .filter((t) => t && t !== BASE_TAB && !seen.has(t) && (seen.add(t), true))
      .concat(
        fields.value
          .map((f) => f.tabName || BASE_TAB)
          .filter((t) => t !== BASE_TAB && !seen.has(t) && (seen.add(t), true)),
      );
    defaultTab.value = model.value?.formDefaultTab || BASE_TAB;
    snapshot();
  } finally {
    loading.value = false;
  }
}

// /////////////////// 页签操作 ///////////////////

function addTab() {
  const name = newTabName.value.trim();
  if (!name) {
    message.warning('请输入页签名称');
    return;
  }
  if (name === BASE_TAB || tabs.value.includes(name)) {
    message.warning('页签已存在');
    return;
  }
  tabs.value.push(name);
  newTabName.value = '';
}

function renameTab(oldName: string, newName: string) {
  const name = newName.trim();
  if (!name || name === oldName) return;
  if (name === BASE_TAB || tabs.value.includes(name)) {
    message.warning('页签名已存在');
    // 还原输入框（重渲染由 tabs 驱动）
    const idx = tabs.value.indexOf(oldName);
    if (idx >= 0) tabs.value.splice(idx, 1, oldName);
    return;
  }
  const idx = tabs.value.indexOf(oldName);
  if (idx >= 0) tabs.value.splice(idx, 1, name);
  for (const f of fields.value) {
    if (f.tabName === oldName) f.tabName = name;
  }
  if (defaultTab.value === oldName) defaultTab.value = name;
}

function moveTab(name: string, dir: -1 | 1) {
  const idx = tabs.value.indexOf(name);
  const target = idx + dir;
  if (idx < 0 || target < 0 || target >= tabs.value.length) return;
  [tabs.value[idx], tabs.value[target]] = [tabs.value[target]!, tabs.value[idx]!];
}

function removeTab(name: string) {
  const idx = tabs.value.indexOf(name);
  const fallback = idx > 0 ? tabs.value[idx - 1]! : BASE_TAB;
  Modal.confirm({
    content: `删除页签「${name}」？其中的 ${groups.value.find((g) => g.name === name)?.rows.length ?? 0} 个字段将移入「${fallback}」。`,
    onOk: () => {
      for (const f of fields.value) {
        if (f.tabName === name) f.tabName = fallback;
      }
      tabs.value.splice(idx, 1);
      if (defaultTab.value === name) defaultTab.value = fallback;
    },
    title: '删除页签',
  });
}

function setDefault(name: string) {
  defaultTab.value = name;
}

// /////////////////// 字段操作 ///////////////////

/** 同页签内相邻字段交换 sort（顺序只在页签内比较） */
function moveField(field: ModelFieldRow, dir: -1 | 1) {
  const tab = field.tabName || BASE_TAB;
  const peers = fields.value.filter((f) => (f.tabName || BASE_TAB) === tab);
  const idx = peers.findIndex((f) => f.id === field.id);
  const target = peers[idx + dir];
  if (!target || idx < 0) return;
  const a = fields.value.find((f) => f.id === field.id)!;
  const b = fields.value.find((f) => f.id === target.id)!;
  [a.sort, b.sort] = [b.sort ?? 0, a.sort ?? 0];
}

function moveFieldToTab(field: ModelFieldRow, tabName: string) {
  field.tabName = tabName;
  // 排到目标页签末尾：取该页签最大 sort + 1
  const peers = fields.value.filter((f) => (f.tabName || BASE_TAB) === tabName && f.id !== field.id);
  field.sort = peers.length ? Math.max(...peers.map((p) => p.sort ?? 0)) + 1 : 0;
}

// /////////////////// 保存 ///////////////////

async function onSave() {
  if (saving.value) return;
  const changed = fields.value.filter((row) => {
    const orig = originals.value.get(row.id);
    if (!orig) return false;
    return (
      row.sort !== orig.sort ||
      row.tabName !== orig.tabName ||
      row.isForm !== orig.isForm
    );
  });
  saving.value = true;
  try {
    for (const row of changed) {
      const orig = originals.value.get(row.id)!;
      await updateFieldApi({
        ...orig,
        sort: row.sort,
        tabName: row.tabName,
        isForm: row.isForm,
      });
    }
    await updateModelApi({
      formTabs: JSON.stringify(tabs.value),
      formDefaultTab: defaultTab.value,
      id: String(model.value?.id ?? ''),
      name: model.value?.name ?? '',
    });
    message.success(
      changed.length > 0
        ? `布局已保存（更新 ${changed.length} 个字段与页签配置）`
        : '页签配置已保存',
    );
    snapshot();
  } finally {
    saving.value = false;
  }
}

function goBack() {
  router.push('/system/model');
}

onMounted(load);
</script>

<template>
  <Page title="发布页面布局设计">
    <template #extra>
      <div class="flex items-center gap-2">
        <Button @click="goBack">返回</Button>
        <Button
          v-if="canSave"
          :loading="saving"
          type="primary"
          @click="onSave"
        >
          保存布局
        </Button>
      </div>
    </template>

    <div class="mb-3 rounded bg-blue-50 px-3 py-2 text-xs text-blue-600">
      这里设计内容发布表单的样子：页签的增删改与排序、字段归属哪个页签、页签内顺序、
      字段是否出现在表单、以及打开发布表单时默认停留在哪个页签。保存后发布/编辑表单立即生效；
      字段的新增与删除请到「字段管理」。
    </div>

    <div
      v-if="canSave"
      class="mb-4 flex items-center gap-2 rounded border border-gray-100 p-2"
    >
      <span class="text-sm">新增页签：</span>
      <Input
        v-model:value="newTabName"
        class="!w-56"
        placeholder="页签名称，如：扩展信息"
        size="small"
        @press-enter="addTab"
      />
      <Button size="small" type="primary" @click="addTab">添加</Button>
      <span class="ml-4 text-xs text-gray-400">
        固定「{{ BASE_TAB }}」页签在最前（含标题/分类等固定项），不可删除；
        打开发布表单时默认停留在选了「默认」的页签。
      </span>
    </div>

    <div class="space-y-4">
      <div
        v-for="group in groups"
        :key="group.name"
        class="rounded border"
        :class="defaultTab === group.name ? 'border-blue-300' : 'border-gray-200'"
      >
        <div class="flex items-center gap-2 border-b border-gray-100 px-3 py-2">
          <Tag :color="defaultTab === group.name ? 'blue' : 'default'">
            {{ defaultTab === group.name ? '默认打开' : '页签' }}
          </Tag>
          <Input
            v-if="!group.fixed"
            v-model:value="tabs[tabs.indexOf(group.name)]"
            class="!w-44"
            size="small"
            @change="(e: any) => renameTab(group.name, e.target?.value ?? '')"
          />
          <span v-else class="text-sm font-medium">{{ group.name }}</span>
          <span class="text-xs text-gray-400">{{ group.rows.length }} 个自定义字段</span>
          <div class="ml-auto flex items-center gap-1">
            <Button
              v-if="defaultTab !== group.name"
              size="small"
              type="link"
              @click="setDefault(group.name)"
            >
              设为默认
            </Button>
            <template v-if="!group.fixed">
              <Tooltip title="页签前移">
                <Button
                  size="small"
                  type="text"
                  @click="moveTab(group.name, -1)"
                >
                  ↑
                </Button>
              </Tooltip>
              <Tooltip title="页签后移">
                <Button
                  size="small"
                  type="text"
                  @click="moveTab(group.name, 1)"
                >
                  ↓
                </Button>
              </Tooltip>
              <Button danger size="small" type="text" @click="removeTab(group.name)">
                删除页签
              </Button>
            </template>
          </div>
        </div>

        <div class="space-y-1.5 p-3">
          <div
            v-if="group.rows.length === 0"
            class="py-3 text-center text-xs text-gray-400"
          >
            暂无字段（把字段「移动到」本页签，或在「字段管理」里新增）
          </div>
          <div
            v-for="(row, idx) in group.rows"
            :key="row.id"
            class="flex items-center gap-2 rounded border border-gray-100 px-2 py-1.5"
            :class="row.fieldType === 'formula' || row.fieldType === 'rollup' ? 'bg-gray-50' : ''"
          >
            <Tooltip title="上移（本页签内）">
              <Button
                :disabled="idx === 0"
                size="small"
                type="text"
                @click="moveField(row, -1)"
              >
                ↑
              </Button>
            </Tooltip>
            <Tooltip title="下移（本页签内）">
              <Button
                :disabled="idx === group.rows.length - 1"
                size="small"
                type="text"
                @click="moveField(row, 1)"
              >
                ↓
              </Button>
            </Tooltip>
            <div class="w-44 shrink-0">
              <div class="text-sm leading-tight">
                <span v-if="row.isRequired === 1" class="text-red-500">*</span>
                {{ row.fieldLabel }}
                <span class="ml-1 text-xs text-gray-300">{{ row.fieldName }}</span>
              </div>
            </div>
            <Tag class="shrink-0" color="blue">{{ row.fieldType }}</Tag>
            <div class="flex flex-1 items-center gap-2">
              <span class="shrink-0 text-xs text-gray-400">所属页签</span>
              <Select
                :value="row.tabName || BASE_TAB"
                class="!w-36"
                :options="allTabOptions"
                size="small"
                @change="(v: any) => moveFieldToTab(row, String(v))"
              />
            </div>
            <div
              v-if="row.fieldType !== 'formula' && row.fieldType !== 'rollup'"
              class="flex shrink-0 items-center gap-1"
            >
              <span class="text-xs text-gray-400">表单</span>
              <Switch
                :checked="row.isForm !== 0"
                size="small"
                @change="(v: any) => (row.isForm = v ? 1 : 0)"
              />
            </div>
            <Tag v-else class="shrink-0" color="default">自动计算</Tag>
          </div>
        </div>
      </div>
    </div>
  </Page>
</template>
