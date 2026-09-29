<script lang="ts" setup>
import type { CommentRow } from '#/api/core/comment';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Table,
} from 'ant-design-vue';

import {
  auditCommentApi,
  batchCommentApi,
  deleteCommentApi,
  getCommentPageApi,
} from '#/api/core/comment';

defineOptions({ name: 'SystemComment' });

const { hasAccessByCodes } = useAccess();

const STATUS_OPTIONS = [
  { label: '全部（不含删除）', value: -1 },
  { label: '未审核', value: 0 },
  { label: '正常', value: 1 },
  { label: '未通过', value: 2 },
  { label: '已删除', value: 3 },
];

const STATUS_TAG: Record<number, string> = {
  0: '未审核',
  1: '正常',
  2: '未通过',
  3: '已删除',
};

const rows = ref<CommentRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const selectedKeys = ref<string[]>([]);

const query = reactive<{
  keyword?: string;
  status: number;
}>({ keyword: '', status: -1 });

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 160 },
  { title: '评论内容', dataIndex: 'content', key: 'content' },
  { title: '所属内容', dataIndex: 'targetTitle', key: 'targetTitle', width: 220 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 160 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getCommentPageApi({
      keyword: query.keyword || undefined,
      p: page.value,
      rows: 20,
      status: query.status >= 0 ? query.status : undefined,
    });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  page.value = 1;
  load();
}

async function onAudit(row: any, status: number) {
  await auditCommentApi(String(row.id), status);
  message.success(status === 1 ? '已通过' : '已驳回');
  load();
}

function onDelete(row: any) {
  Modal.confirm({
    content: '确认删除该评论？',
    onOk: async () => {
      await deleteCommentApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

function onBatch(status: number) {
  if (selectedKeys.value.length === 0) {
    message.warning('请先勾选评论');
    return;
  }
  const tip =
    status === 3 ? `确认删除选中的 ${selectedKeys.value.length} 条评论？` : '';
  const run = async () => {
    await batchCommentApi(selectedKeys.value, status);
    message.success('操作成功');
    selectedKeys.value = [];
    load();
  };
  if (tip) {
    Modal.confirm({ content: tip, onOk: run, title: '批量操作' });
  } else {
    run();
  }
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="query.status"
        :options="STATUS_OPTIONS"
        class="w-40"
        @change="onSearch"
      />
      <Input
        v-model:value="query.keyword"
        allow-clear
        class="w-56"
        placeholder="评论内容关键词"
        @press-enter="onSearch"
      />
      <Button @click="onSearch">搜索</Button>

      <div v-if="hasAccessByCodes(['/api/system/comment/batch'])" class="ml-auto flex gap-2">
        <Button :disabled="!selectedKeys.length" size="small" @click="onBatch(1)">
          批量通过
        </Button>
        <Button :disabled="!selectedKeys.length" size="small" @click="onBatch(2)">
          批量驳回
        </Button>
        <Button
          :disabled="!selectedKeys.length"
          danger
          size="small"
          @click="onBatch(3)"
        >
          批量删除
        </Button>
      </div>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      :row-selection="{ selectedRowKeys: selectedKeys, onChange: (keys: any[]) => (selectedKeys = keys) }"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'content'">
          <div class="line-clamp-2">{{ record.content }}</div>
        </template>
        <template v-else-if="column.key === 'articleTitle'">
          <span class="text-xs text-gray-500">{{ record.targetTitle || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <span :class="record.status === 1 ? 'text-green-600' : 'text-orange-500'">
            {{ STATUS_TAG[record.status] }}
          </span>
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="hasAccessByCodes(['/api/system/comment/audit']) && record.status !== 1"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="onAudit(record, 1)"
          >
            通过
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/comment/audit']) && record.status !== 2"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="onAudit(record, 2)"
          >
            驳回
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/comment/delete'])"
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
  </Page>
</template>
