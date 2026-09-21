<script lang="ts" setup>
import type { BlockItemRow, BlockRow } from '#/api/core/block';

import { onMounted, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  message,
  Modal,
  Pagination,
  Switch,
  Table,
  Textarea,
} from 'ant-design-vue';

import {
  deleteBlockApi,
  getBlockItemsApi,
  getBlockListApi,
  saveBlockApi,
} from '#/api/core/block';

import BlockModal from './block-modal.vue';
import ItemModal from './item-modal.vue';

defineOptions({ name: 'SystemBlock' });

const { hasAccessByCodes } = useAccess();

const rows = ref<BlockRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);

const [BlockModalComp, blockModalApi] = useVbenModal({
  connectedComponent: BlockModal,
  destroyOnClose: true,
});

const [ItemModalComp, itemModalApi] = useVbenModal({
  connectedComponent: ItemModal,
  destroyOnClose: true,
});

const TYPE_LABEL: Record<number, string> = {
  0: '富文本',
  1: '图片',
  2: '推荐位列表',
  3: '模板碎片',
};

const columns = [
  { title: '名称', dataIndex: 'blockName', key: 'blockName' },
  { title: '调用键', dataIndex: 'blockKey', key: 'blockKey' },
  { title: '类型', dataIndex: 'blockType', key: 'blockType', width: 110 },
  { title: '条数', dataIndex: 'itemCount', key: 'itemCount', width: 70 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  {
    title: '显示',
    dataIndex: 'status',
    key: 'status',
    width: 80,
  },
  { title: '操作', key: 'action', width: 260 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getBlockListApi({ p: page.value });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

function openAdd() {
  blockModalApi.setData({ onSaved: load }).open();
}

function openEdit(row: BlockRow) {
  blockModalApi.setData({ onSaved: load, record: row }).open();
}

function openItems(row: BlockRow) {
  itemModalApi.setData({ block: row }).open();
}

function onDelete(row: BlockRow) {
  Modal.confirm({
    content: `删除碎片位「${row.blockName}」及其全部条目？`,
    onOk: async () => {
      await deleteBlockApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

async function onToggle(row: BlockRow, checked: boolean) {
  await saveBlockApi({ ...row, status: checked ? 1 : 0 });
  message.success(checked ? '已显示' : '已隐藏');
  load();
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mb-3 flex items-center gap-2">
      <span class="text-sm text-gray-400">
        模板用 &lt;@fly_block key="调用键"&gt;…&lt;/@fly_block&gt; 输出，变量 block.content / block.items
      </span>
      <Button
        v-if="hasAccessByCodes(['/api/system/block/save'])"
        class="ml-auto"
        type="primary"
        @click="openAdd"
      >
        新增碎片位
      </Button>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'blockType'">
          {{ TYPE_LABEL[record.blockType] }}
        </template>
        <template v-else-if="column.key === 'blockKey'">
          <code class="text-xs">{{ record.blockKey }}</code>
        </template>
        <template v-else-if="column.key === 'status'">
          <Switch
            :checked="Number(record.status) === 1"
            :disabled="!hasAccessByCodes(['/api/system/block/save'])"
            size="small"
            @change="(checked: any) => onToggle(record, Boolean(checked))"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="record.blockType === 2"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="openItems(record)"
          >
            条目管理
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/block/save'])"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="openEdit(record)"
          >
            编辑
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/block/delete'])"
            class="px-2"
            danger
            size="small"
            type="link"
            @click="onDelete(record)"
          >
            删除
          </Button>
        </template>
      </template>
    </Table>
    <div class="mt-3 flex justify-end">
      <Pagination
        v-model:current="page"
        :page-size="20"
        :show-total="(t: number) => `共 ${t} 条`"
        :total="total"
        size="small"
        @change="load"
      />
    </div>
    <BlockModalComp />
    <ItemModalComp />
  </Page>
</template>
