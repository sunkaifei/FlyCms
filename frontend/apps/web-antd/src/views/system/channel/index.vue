<script lang="ts" setup>
import type { ChannelModelOption, ChannelRow } from '#/api/core/channel';

import { computed, h, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import {
  Button,
  message,
  Modal,
  Radio,
  Select,
  Switch,
  Table,
} from 'ant-design-vue';

import {
  deleteChannelApi,
  getChannelModelsApi,
  getChannelTreeApi,
  moveChannelApi,
  moveToChannelApi,
  statusChannelApi,
} from '#/api/core/channel';

import ChannelModal from './channel-modal.vue';

defineOptions({ name: 'SystemChannel' });

const { hasAccessByCodes } = useAccess();

const rows = ref<ChannelRow[]>([]);
const models = ref<ChannelModelOption[]>([]);
const loading = ref(false);

const [ChannelModalComp, channelModalApi] = useEditDrawer({
  connectedComponent: ChannelModal,
  destroyOnClose: true,
});

const TYPE_LABEL: Record<number, string> = {
  0: '列表',
  1: '单页',
  2: '外链',
  3: '聚合',
};

const modelName = computed(() => {
  const map = new Map<number, string>();
  for (const m of models.value) {
    map.set(m.id, m.modelName);
  }
  return (id: number) => (id ? (map.get(id) ?? `#${id}`) : '—');
});

const columns = [
  { title: '栏目名称', dataIndex: 'channelName', key: 'channelName' },
  { title: '目录 / URL', dataIndex: 'channelDir', key: 'channelDir', width: 180 },
  { title: '类型', dataIndex: 'channelType', key: 'channelType', width: 80 },
  { title: '绑定模型', dataIndex: 'modelId', key: 'modelId', width: 180 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 130 },
  { title: '显示', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 280 },
];

async function load() {
  loading.value = true;
  try {
    rows.value = (await getChannelTreeApi()) ?? [];
  } finally {
    loading.value = false;
  }
}

async function loadModels() {
  try {
    models.value = (await getChannelModelsApi()) ?? [];
  } catch {
    models.value = [];
  }
}

function openAdd(parent?: any) {
  channelModalApi
    .setData({
      onSaved: load,
      parentId: parent?.id ?? 0,
      parentName: parent?.channelName ?? '',
    })
    .open();
}

function openEdit(row: any) {
  channelModalApi.setData({ onSaved: load, record: row }).open();
}

/**
 * 删除栏目：内容数据一律不动（§6.5 红线），
 * 但有子栏目时必须显式选择"上提到父级"还是"保留并隐藏"——避免把子栏目连带埋掉。
 */
function onDelete(row: any) {
  const children: number = row.children?.length ?? 0;
  const mode = ref<'hide' | 'promote'>('promote');
  Modal.confirm({
    content: () =>
      children
        ? h('div', {}, [
            h('div', `删除「${row.channelName}」？只删栏目行，内容数据不受影响。`),
            h(
              'div',
              { class: 'mt-2 text-gray-500' },
              `存在 ${children} 个子栏目，请选择处置方式：`,
            ),
            h(
              Radio.Group,
              {
                class: 'mt-2',
                'onUpdate:value': (v: unknown) => {
                  mode.value = v as 'hide' | 'promote';
                },
                value: mode.value,
              },
              {
                default: () => [
                  h(Radio, { value: 'promote' }, { default: () => '上提到父级' }),
                  h(Radio, { value: 'hide' }, { default: () => '保留并隐藏' }),
                ],
              },
            ),
          ])
        : `删除「${row.channelName}」？只删栏目行，内容数据不受影响。`,
    okText: '确认删除',
    onOk: async () => {
      await deleteChannelApi(String(row.id), mode.value);
      message.success('已删除');
      load();
    },
    title: '删除栏目',
  });
}

async function onToggle(row: any, checked: boolean) {
  await statusChannelApi(String(row.id), checked ? 1 : 0);
  message.success(checked ? '已显示' : '已隐藏');
  load();
}

async function onMove(row: any, up: boolean) {
  await moveChannelApi(String(row.id), up);
  load();
}

/** 移动到某个父栏目下（0=顶层）；后端会拦「移到自己后代下」这类成环操作 */
async function onMoveTo(row: any, fatherId: number) {
  await moveToChannelApi(String(row.id), fatherId);
  message.success('已移动');
  load();
}

const flatOptions = computed(() => {
  const list: { label: string; value: number }[] = [{ label: '（顶层）', value: 0 }];
  const walk = (nodes: ChannelRow[], prefix = '') => {
    for (const n of nodes) {
      list.push({ label: prefix + n.channelName, value: Number(n.id) });
      if (n.children?.length) {
        walk(n.children, `${prefix}${n.channelName} / `);
      }
    }
  };
  walk(rows.value);
  return list;
});

onMounted(() => {
  load();
  loadModels();
});
</script>

<template>
  <Page title="栏目管理">
    <div class="mb-2 flex items-center gap-3">
      <Button
        v-if="hasAccessByCodes(['/api/system/channel/save'])"
        type="primary"
        @click="() => openAdd()"
      >
        新增根栏目
      </Button>
      <span class="text-sm text-gray-400">
        栏目是 URL 归属层：<code>/{dir}/</code>
        。它可以绑定模型，也可以是单页/外链/聚合 —— 不必与模型绑死。
      </span>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      default-expand-all-rows
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'channelName'">
          <span class="font-medium">{{ record.channelName }}</span>
        </template>
        <template v-else-if="column.key === 'channelDir'">
          <code class="text-xs">/{{ record.channelDir }}/</code>
        </template>
        <template v-else-if="column.key === 'channelType'">
          {{ TYPE_LABEL[record.channelType] }}
        </template>
        <template v-else-if="column.key === 'modelId'">
          {{ modelName(record.modelId) }}
        </template>
        <template v-else-if="column.key === 'sort'">
          <Button
            class="px-2"
            size="small"
            type="link"
            @click="() => onMove(record, true)"
          >
            ↑
          </Button>
          <Button
            class="px-2"
            size="small"
            type="link"
            @click="() => onMove(record, false)"
          >
            ↓
          </Button>
          <span class="text-xs text-gray-400">{{ record.sort }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <Switch
            :checked="Number(record.status) === 1"
            :disabled="!hasAccessByCodes(['/api/system/channel/save'])"
            size="small"
            @change="(checked: any) => onToggle(record, Boolean(checked))"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="hasAccessByCodes(['/api/system/channel/save'])"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="() => openAdd(record)"
          >
            新增子栏目
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/channel/save'])"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="() => openEdit(record)"
          >
            编辑
          </Button>
          <Select
            class="mr-1 w-28 align-middle"
            size="small"
            placeholder="移动到…"
            :options="flatOptions"
            @change="(v: any) => onMoveTo(record, Number(v))"
          />
          <Button
            v-if="hasAccessByCodes(['/api/system/channel/delete'])"
            class="px-2"
            danger
            size="small"
            type="link"
            @click="() => onDelete(record)"
          >
            删除
          </Button>
        </template>
      </template>
    </Table>
    <ChannelModalComp />
  </Page>
</template>
