<script lang="ts" setup>
/**
 * 布局管理（P10 区域编排 V2 / 规划 §7.3 §8.4）
 *
 * 把「页面哪一块显示什么」从模板代码变成后台数据：
 * 模板里只写 <@fly_area name="content_top"/> 占位，具体放碎片 / 内容列表 / 自定义 HTML
 * 由这里编排。主题在 theme.json 的 supports.regions 里声明可用区域。
 */
import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Input,
  message,
  Modal,
  Select,
  Switch,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import {
  deleteAreaBlockApi,
  getAreaListApi,
  getThemeListApi,
  previewAreaApi,
  reorderAreaBlockApi,
  saveAreaBlockApi,
  toggleAreaBlockApi,
} from '#/api/core/template';
import { getAreaUsageApi } from '#/api/core/template';
import { getBlockListApi } from '#/api/core/block';
import type {
  AreaBlockRow,
  AreaBlockType,
  AreaRegion,
} from '#/api/core/template';

defineOptions({ name: 'SystemArea' });

const { hasAccessByCodes } = useAccess();
const canManage = computed(() => hasAccessByCodes(['/api/system/area/save']));

const loading = ref(false);
const theme = ref('');
const themeOptions = ref<{ label: string; value: string }[]>([]);
const regions = ref<AreaRegion[]>([]);
const blocks = ref<Record<string, AreaBlockRow[]>>({});

const TYPE_OPTIONS: { label: string; value: AreaBlockType }[] = [
  { label: '碎片（fly_block key）', value: 'BLOCK' },
  { label: '标签聚合（fly_tag_list tag）', value: 'TAG' },
  { label: '自定义 HTML', value: 'HTML' },
];
const TYPE_LABEL: Record<string, string> = {
  BLOCK: '碎片',
  HTML: '自定义HTML',
  TAG: '标签聚合',
};
const TYPE_COLOR: Record<string, string> = {
  BLOCK: 'blue',
  HTML: 'purple',
  TAG: 'green',
};

const columns = [
  { dataIndex: 'sort', key: 'sort', title: '排序', width: 70 },
  { dataIndex: 'blockType', key: 'blockType', title: '类型', width: 110 },
  { dataIndex: 'blockTitle', key: 'blockTitle', title: '标题' },
  { dataIndex: 'blockRef', key: 'blockRef', title: '引用内容' },
  { dataIndex: 'status', key: 'status', title: '启用', width: 80 },
  { key: 'action', title: '操作', width: 300 },
];

// 编辑弹窗
const editVisible = ref(false);
const editing = ref<AreaBlockRow>(blankBlock());
const saving = ref(false);

// 预览弹窗
const previewVisible = ref(false);
const previewHtml = ref('');
const previewAreaName = ref('');

// P-4 区块化编辑器：页面映射 / 碎片素材库 / 行拖拽
const usage = ref<Record<string, string[]>>({});
const blockOptions = ref<{ label: string; value: string }[]>([]);
const dragIndex = ref(-1);
const overIndex = ref(-1);

function blankBlock(areaName = ''): AreaBlockRow {
  return {
    areaName,
    blockRef: '',
    blockTitle: '',
    blockType: 'BLOCK',
    wrapperClass: '',
    sort: 0,
    status: 1,
  };
}

async function loadThemes() {
  try {
    const res = await getThemeListApi();
    const current = res.current || '';
    themeOptions.value = (res.themes || []).map((t) => ({
      label: t.name ? `${t.name}（${t.code}）` : t.code,
      value: t.code,
    }));
    if (!theme.value) {
      theme.value = current;
    }
  } catch {
    themeOptions.value = [];
  }
}

async function load() {
  loading.value = true;
  try {
    const res = await getAreaListApi(theme.value || undefined);
    regions.value = res.regions ?? [];
    blocks.value = res.blocks ?? {};
    theme.value = res.theme || theme.value;
    getAreaUsageApi(theme.value || undefined)
      .then((u) => (usage.value = u ?? {}))
      .catch(() => (usage.value = {}));
  } catch {
    message.error('加载布局数据失败');
  } finally {
    loading.value = false;
  }
}

function rowsOf(area: string): AreaBlockRow[] {
  return blocks.value[area] ?? [];
}

/** Table 插槽的 record 是 Record<string, any>，这里做一次窄化 */
type LooseRow = AreaBlockRow | Record<string, any>;

function asRow(row: LooseRow): AreaBlockRow {
  return row as AreaBlockRow;
}

function openAdd(area: string) {
  editing.value = blankBlock(area);
  editVisible.value = true;
}

function openEdit(area: string, row: LooseRow) {
  editing.value = { ...asRow(row), areaName: area };
  editVisible.value = true;
}

async function doSave() {
  const e = editing.value;
  if (!e.areaName) {
    message.warning('请选择区域');
    return;
  }
  if (!e.blockRef?.trim()) {
    message.warning('引用内容不能为空（碎片填调用键 / 标签填关键词 / HTML 填正文）');
    return;
  }
  saving.value = true;
  try {
    const r = await saveAreaBlockApi(e);
    if (r.code === 0) {
      message.success('已保存');
      editVisible.value = false;
      await load();
      refreshPreviewIfOpen(e.areaName);
    } else {
      message.error(r.msg || '保存失败');
    }
  } finally {
    saving.value = false;
  }
}

async function doDelete(area: string, raw: LooseRow) {
  const row = asRow(raw);
  if (!row.id) return;
  Modal.confirm({
    content: `删除区域「${area}」下的区块「${row.blockTitle || row.blockRef}」？`,
    okButtonProps: { danger: true },
    okText: '删除',
    title: '确认删除',
    onOk: async () => {
      const r = await deleteAreaBlockApi(row.id!);
      if (r.code === 0) {
        message.success('已删除');
        await load();
      } else {
        message.error(r.msg || '删除失败');
      }
    },
  });
}

async function doToggle(raw: LooseRow, checked: boolean) {
  const row = asRow(raw);
  if (!row.id) return;
  const r = await toggleAreaBlockApi(row.id, checked ? 1 : 0);
  if (r.code === 0) {
    await load();
  } else {
    message.error(r.msg || '操作失败');
  }
}

/** 上移/下移：本地交换后整表提交顺序（后端按数组下标重写 sort） */
async function doMove(area: string, index: number, delta: number) {
  const list = [...rowsOf(area)];
  const target = index + delta;
  if (target < 0 || target >= list.length) return;
  const [item] = list.splice(index, 1);
  list.splice(target, 0, item!);
  const ids = list.map((r) => r.id).filter(Boolean) as string[];
  if (ids.length !== list.length) {
    message.warning('存在未落库的区块，请先刷新');
    return;
  }
  const r = await reorderAreaBlockApi(ids);
  if (r.code === 0) {
    await load();
  } else {
    message.error(r.msg || '排序失败');
  }
}

/** P-4 拖拽排序：拖到目标位后按新顺序整表提交 */
async function onRowDrop(area: string, target: number) {
  if (dragIndex.value < 0 || dragIndex.value === target) {
    dragIndex.value = -1;
    overIndex.value = -1;
    return;
  }
  const list = [...rowsOf(area)];
  const [moved] = list.splice(dragIndex.value, 1);
  if (!moved) return;
  list.splice(target, 0, moved);
  dragIndex.value = -1;
  overIndex.value = -1;
  const ids = list.map((r) => r.id).filter(Boolean) as string[];
  if (ids.length !== list.length) {
    message.warning('存在未落库的区块，请先刷新');
    return;
  }
  const r = await reorderAreaBlockApi(ids);
  if (r.code === 0) {
    await load();
    refreshPreviewIfOpen(area);
  } else {
    message.error(r.msg || '排序失败');
  }
}

/** Q2 区块复制：复制标题/类型/引用/状态，排序插到原区块之后 */
async function doCopy(area: string, index: number) {
  const list = [...rowsOf(area)];
  const src = list[index];
  if (!src) return;
  const copy = {
    areaName: src.areaName,
    blockType: src.blockType,
    blockRef: src.blockRef,
    blockTitle: (src.blockTitle || '区块') + ' 副本',
    sort: (src.sort || 0) + 1,
    status: src.status,
  };
  const r = await saveAreaBlockApi(copy);
  if (r.code === 0) {
    message.success('已复制');
    await load();
  } else {
    message.error(r.msg || '复制失败');
  }
}

/** 预览抽屉开着时，变更后自动刷新（实时预览） */
function refreshPreviewIfOpen(area: string) {
  if (previewVisible.value && previewAreaName.value === area) {
    doPreview(area);
  }
}

async function doPreview(area: string) {
  try {
    const res = await previewAreaApi(area, theme.value || undefined);
    previewAreaName.value = area;
    previewHtml.value = res.html || '<!-- 该区域暂无启用的区块 -->';
    previewVisible.value = true;
  } catch {
    message.error('预览失败');
  }
}

async function onThemeChange() {
  await load();
}

onMounted(async () => {
  await loadThemes();
  await load();
  try {
    const res = await getBlockListApi({ p: 1 });
    blockOptions.value = (res.list ?? []).map((b: any) => ({
      label: b.blockTitle || b.blockKey || b.key || b.name,
      value: b.blockKey || b.key || b.name,
    }));
  } catch {
    blockOptions.value = [];
  }
});
</script>

<template>
  <Page
    title="布局管理"
    description="模板里只留 @fly_area 占位，这一块放什么由这里决定"
  >
    <template #extra>
      <div class="flex items-center gap-2">
        <Select
          v-model:value="theme"
          class="w-56"
          :options="themeOptions"
          placeholder="选择主题"
          @change="onThemeChange"
        />
        <Button :loading="loading" @click="load">刷新</Button>
      </div>
    </template>

    <div v-if="loading" class="p-4 text-center text-gray-400">加载中…</div>

    <template v-else>
      <div v-if="!regions.length" class="rounded border border-dashed p-8 text-center text-gray-400">
        当前主题没有声明任何区域。请在主题的 <code>theme.json</code> 里
        的 <code>supports.regions</code> 增加区域名（例如 content_top / sidebar / content_bottom）。
      </div>

      <div
        v-for="area in regions"
        :key="area.name"
        class="mb-5 rounded-lg border border-gray-200 p-4"
      >
        <div class="mb-3 flex items-center gap-2">
          <span class="text-base font-semibold">{{ area.name }}</span>
          <Tag>{{ rowsOf(area.name).length }} 个区块</Tag>
          <Tooltip :title="(usage[area.name] || []).join('、')">
            <Tag v-if="(usage[area.name] || []).length" color="geekblue">
              被 {{ (usage[area.name] || []).length }} 个页面引用
            </Tag>
            <Tag v-else color="default">未被模板引用</Tag>
          </Tooltip>
          <Tag v-if="!area.declared" color="orange">
            未在 theme.json 声明
          </Tag>
          <div class="ml-auto flex gap-2">
            <Button size="small" @click="doPreview(area.name)">预览区域</Button>
            <Button
              v-if="canManage"
              type="primary"
              size="small"
              @click="openAdd(area.name)"
            >
              新增区块
            </Button>
          </div>
        </div>

        <Table
          :columns="columns"
          :data-source="rowsOf(area.name)"
          :pagination="false"
          row-key="id"
          size="small"
          :custom-row="
            (_record: any, index: number | undefined) => ({
              draggable: canManage && index !== undefined,
              onDragstart: () => (dragIndex = index ?? -1),
              onDragover: (e: DragEvent) => {
                if (dragIndex >= 0 && index !== undefined) {
                  e.preventDefault();
                  overIndex = index;
                }
              },
              onDrop: () => onRowDrop(area.name, overIndex),
              style:
                overIndex === index && dragIndex >= 0 && dragIndex !== index
                  ? 'border-top:2px solid #3a6fd8'
                  : '',
            })
          "
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'blockType'">
              <Tag :color="TYPE_COLOR[record.blockType] || 'default'">
                {{ TYPE_LABEL[record.blockType] || record.blockType }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'blockTitle'">
              <span>{{ record.blockTitle || '—' }}</span>
            </template>
            <template v-else-if="column.key === 'blockRef'">
              <div class="max-w-[420px] truncate font-mono text-xs" :title="record.blockRef">
                {{ record.blockRef }}
              </div>
            </template>
            <template v-else-if="column.key === 'status'">
              <Switch
                :checked="record.status === 1"
                :disabled="!canManage"
                size="small"
                @change="(v: any) => doToggle(record, !!v)"
              />
            </template>
            <template v-else-if="column.key === 'action'">
              <div class="flex flex-wrap gap-1">
                <Button
                  size="small"
                  :disabled="index === 0"
                  @click="doMove(area.name, index, -1)"
                >
                  上移
                </Button>
                <Button
                  size="small"
                  :disabled="index === rowsOf(area.name).length - 1"
                  @click="doMove(area.name, index, 1)"
                >
                  下移
                </Button>
                <Button
                  v-if="canManage"
                  size="small"
                  @click="openEdit(area.name, record)"
                >
                  编辑
                </Button>
                <Button
                  v-if="canManage"
                  size="small"
                  @click="doCopy(area.name, index)"
                >
                  复制
                </Button>
                <Button
                  v-if="canManage"
                  danger
                  size="small"
                  @click="doDelete(area.name, record)"
                >
                  删除
                </Button>
              </div>
            </template>
          </template>
        </Table>
      </div>
    </template>

    <Modal
      v-model:open="editVisible"
      :confirm-loading="saving"
      :title="editing.id ? '编辑区块' : '新增区块'"
      ok-text="保存"
      @ok="doSave"
    >
      <div class="space-y-3">
        <div>
          <div class="mb-1 text-sm">所属区域</div>
          <Input v-model:value="editing.areaName" disabled />
        </div>
        <div>
          <div class="mb-1 text-sm">区块类型</div>
          <Select
            v-model:value="editing.blockType"
            class="w-full"
            :options="TYPE_OPTIONS"
          />
        </div>
        <div>
          <div class="mb-1 text-sm">包装器 CSS 类（S1-a：外层 div 附加类，多个空格分隔）</div>
          <Input v-model:value="editing.wrapperClass" placeholder="如 card shadow-lg" />
        </div>
        <div v-if="editing.blockType === 'BLOCK'" class="mb-1">
          <div class="mb-1 text-xs text-gray-400">从碎片库选择（自动填调用键）</div>
          <Select
            allow-clear
            class="w-full"
            option-filter-prop="label"
            :options="blockOptions"
            placeholder="选择碎片"
            show-search
            @change="
              (v: any) => {
                if (v) editing.blockRef = String(v);
              }
            "
          />
        </div>
        <div>
          <div class="mb-1 text-sm">标题（仅后台标识用）</div>
          <Input v-model:value="editing.blockTitle" placeholder="如 首页焦点图" />
        </div>
        <div>
          <div class="mb-1 text-sm">
            <template v-if="editing.blockType === 'BLOCK'">碎片调用键（如 home_focus）</template>
            <template v-else-if="editing.blockType === 'TAG'">标签关键词（如 Spring）</template>
            <template v-else>自定义 HTML（会交给 FreeMarker 渲染，可用标签）</template>
          </div>
          <Input.TextArea
            v-model:value="editing.blockRef"
            :auto-size="{ maxRows: 12, minRows: editing.blockType === 'HTML' ? 6 : 1 }"
            :placeholder="
              editing.blockType === 'BLOCK'
                ? 'home_focus'
                : editing.blockType === 'TAG'
                  ? 'Spring'
                  : '<div class=\&quot;banner\&quot;>…</div>'
            "
          />
        </div>
        <div class="flex gap-3">
          <div class="flex-1">
            <div class="mb-1 text-sm">排序（越小越前）</div>
            <Input
              :value="editing.sort"
              type="number"
              @change="
                (e: any) => (editing.sort = Number(e.target.value) || 0)
              "
            />
          </div>
          <div class="flex-1">
            <div class="mb-1 text-sm">启用</div>
            <Switch
              :checked="editing.status === 1"
              @change="(v: any) => (editing.status = v ? 1 : 0)"
            />
          </div>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="previewVisible"
      :footer="null"
      :title="`区域预览：${previewAreaName}`"
      width="80%"
    >
      <iframe
        :srcdoc="previewHtml"
        class="h-[60vh] w-full rounded border"
        sandbox=""
      ></iframe>
    </Modal>
  </Page>
</template>
