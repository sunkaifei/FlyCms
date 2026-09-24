<script lang="ts" setup>
import type { GuideRow } from '#/api/core/guide';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, Input, message, Modal, Pagination, Switch, Table } from 'ant-design-vue';

import {
  deleteGuideApi,
  getGuidePageApi,
  updateGuideStatusApi,
} from '#/api/core/guide';

import GuideModal from './guide-modal.vue';

defineOptions({ name: 'SystemGuide' });

const { hasAccessByCodes } = useAccess();

const rows = ref<GuideRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const keyword = ref('');

const [GuideModalComp, guideModalApi] = useEditDrawer({
  connectedComponent: GuideModal,
  destroyOnClose: true,
});

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '链接地址', dataIndex: 'link', key: 'link' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '显示', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 140 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getGuidePageApi({
      name: keyword.value || undefined,
      p: page.value,
      rows: 20,
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

function openAdd() {
  guideModalApi.setData({ onSaved: load }).open();
}

function openEdit(row: any) {
  guideModalApi.setData({ onSaved: load, record: row }).open();
}

function onDelete(row: any) {
  Modal.confirm({
    content: `确认删除导航「${row.name}」？`,
    onOk: async () => {
      await deleteGuideApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

async function onToggle(row: any, checked: boolean) {
  await updateGuideStatusApi(String(row.id), checked ? 1 : 0);
  message.success(checked ? '已显示' : '已隐藏');
  load();
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mb-3 flex items-center gap-2">
      <Input
        v-model:value="keyword"
        allow-clear
        class="w-56"
        placeholder="导航名称"
        @press-enter="onSearch"
      />
      <Button @click="onSearch">搜索</Button>
      <Button
        v-if="hasAccessByCodes(['/api/system/guide/save'])"
        class="ml-auto"
        type="primary"
        @click="openAdd"
      >
        新增导航
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
        <template v-if="column.key === 'link'">
          <code class="text-xs">{{ record.link }}</code>
        </template>
        <template v-else-if="column.key === 'status'">
          <Switch
            :checked="Number(record.status) === 1"
            :disabled="!hasAccessByCodes(['/api/system/guide/save'])"
            size="small"
            @change="(checked: any) => onToggle(record, Boolean(checked))"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="hasAccessByCodes(['/api/system/guide/save'])"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="openEdit(record)"
          >
            编辑
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/guide/delete'])"
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
    <GuideModalComp />
  </Page>
</template>
