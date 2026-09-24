<script lang="ts" setup>
import type { BlockItemRow, BlockRow } from '#/api/core/block';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Button, Input, message, Modal as AntModal, Textarea } from 'ant-design-vue';

import {
  deleteBlockItemApi,
  getBlockItemsApi,
  saveBlockItemApi,
} from '#/api/core/block';

/**
 * 碎片条目管理弹窗（推荐位条目，支持定时上下线）
 */
const block = ref<null | BlockRow>(null);
const items = ref<BlockItemRow[]>([]);

const editOpen = ref(false);
const editId = ref('');
const editTitle = ref('');
const editImage = ref('');
const editUrl = ref('');
const editSummary = ref('');
const editStart = ref('');
const editEnd = ref('');
const editSort = ref(0);

async function load() {
  if (!block.value) return;
  items.value = (await getBlockItemsApi(String(block.value.id))) ?? [];
}

function openAdd() {
  editId.value = '';
  editTitle.value = '';
  editImage.value = '';
  editUrl.value = '';
  editSummary.value = '';
  editStart.value = '';
  editEnd.value = '';
  editSort.value = 0;
  editOpen.value = true;
}

function openEdit(row: BlockItemRow) {
  editId.value = String(row.id);
  editTitle.value = row.title || '';
  editImage.value = row.image || '';
  editUrl.value = row.url || '';
  editSummary.value = row.summary || '';
  editStart.value = (row.startTime || '').replace('T', ' ').slice(0, 19);
  editEnd.value = (row.endTime || '').replace('T', ' ').slice(0, 19);
  editSort.value = row.sort;
  editOpen.value = true;
}

async function onSave() {
  if (!editTitle.value.trim()) {
    message.warning('条目标题不能为空');
    return;
  }
  await saveBlockItemApi({
    blockId: block.value?.id,
    endTime: editEnd.value,
    id: editId.value || undefined,
    image: editImage.value,
    sort: editSort.value,
    startTime: editStart.value,
    summary: editSummary.value,
    title: editTitle.value,
    url: editUrl.value,
  });
  message.success('已保存');
  editOpen.value = false;
  load();
}

function onDelete(row: BlockItemRow) {
  AntModal.confirm({
    content: `删除条目「${row.title}」？`,
    onOk: async () => {
      await deleteBlockItemApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

const [Modal, modalApi] = useEditDrawer({ title: '碎片条目' });

onMounted(async () => {
  const data = modalApi.getData() as
    | { block?: BlockRow; onSaved?: () => void }
    | undefined;
  block.value = data?.block ?? null;
  await load();
});

// 供模板点击条目管理后的刷新绑定
defineExpose({ load });
</script>

<template>
  <Modal>
    <div class="mb-2 flex items-center justify-between">
      <span class="text-sm text-gray-400">
        {{ block?.blockName }} · 条目按排序输出，起止时间内自动上下线
      </span>
      <Button size="small" type="primary" @click="openAdd">新增条目</Button>
    </div>
    <Table
      :columns="[
        { title: '标题', dataIndex: 'title', key: 'title' },
        { title: '链接', dataIndex: 'url', key: 'url' },
        { title: '上线', dataIndex: 'startTime', key: 'startTime', width: 150 },
        { title: '下线', dataIndex: 'endTime', key: 'endTime', width: 150 },
        { title: '排序', dataIndex: 'sort', key: 'sort', width: 60 },
        { title: '操作', key: 'action', width: 130 },
      ]"
      :data-source="items"
      :pagination="false"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <Button
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="() => openEdit(record)"
          >
            编辑
          </Button>
          <Button
            class="px-2"
            danger
            size="small"
            type="link"
            @click="() => onDelete(record)"
          >
            删除
          </Button>
        </template>
        <template v-else-if="column.key === 'startTime' || column.key === 'endTime'">
          {{ (record[column.key] || '-').replace('T', ' ').slice(0, 16) }}
        </template>
        <template v-else>
          {{ record[column.key as string] || '-' }}
        </template>
      </template>
    </Table>

    <AntModal v-model:open="editOpen" :title="editId ? '编辑条目' : '新增条目'" @ok="onSave">
      <div class="grid grid-cols-1 gap-y-3">
        <div>
          <div class="mb-1 text-sm">标题</div>
          <Input v-model:value="editTitle" :maxlength="200" />
        </div>
        <div>
          <div class="mb-1 text-sm">链接</div>
          <Input v-model:value="editUrl" placeholder="https://... 或站内路径" />
        </div>
        <div>
          <div class="mb-1 text-sm">图片地址</div>
          <Input v-model:value="editImage" placeholder="图片 URL 或 /uploadfiles/... 路径" />
        </div>
        <div>
          <div class="mb-1 text-sm">摘要</div>
          <Textarea v-model:value="editSummary" :rows="2" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <div class="mb-1 text-sm">上线时间（空=立即）</div>
            <Input v-model:value="editStart" placeholder="yyyy-MM-dd HH:mm:ss" />
          </div>
          <div>
            <div class="mb-1 text-sm">下线时间（空=长期）</div>
            <Input v-model:value="editEnd" placeholder="yyyy-MM-dd HH:mm:ss" />
          </div>
          <div>
            <div class="mb-1 text-sm">排序</div>
            <Input v-model:value="editSort" type="number" />
          </div>
        </div>
      </div>
    </AntModal>
  </Modal>
</template>
