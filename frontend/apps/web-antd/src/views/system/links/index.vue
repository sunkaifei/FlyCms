<script lang="ts" setup>
import type { LinkRow } from '#/api/core/links';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, Input, message, Modal, Pagination, Select, Switch, Table } from 'ant-design-vue';

import {
  deleteLinkApi,
  getLinksPageApi,
  updateLinkStatusApi,
} from '#/api/core/links';

import LinkModal from './link-modal.vue';

defineOptions({ name: 'SystemLinks' });

const { hasAccessByCodes } = useAccess();

const FILTER_OPTIONS = [
  { label: '全部', value: -1 },
  { label: '显示中', value: 1 },
  { label: '已隐藏', value: 0 },
];

const rows = ref<LinkRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const keyword = ref('');
const isShow = ref(-1);

const [LinkModalComp, linkModalApi] = useEditDrawer({
  connectedComponent: LinkModal,
  destroyOnClose: true,
});

const columns = [
  { title: '网站名称', dataIndex: 'linkName', key: 'linkName' },
  { title: '网址', dataIndex: 'linkUrl', key: 'linkUrl' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '显示', dataIndex: 'isShow', key: 'isShow', width: 80 },
  { title: '操作', key: 'action', width: 140 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getLinksPageApi({
      isShow: isShow.value >= 0 ? isShow.value : undefined,
      keyword: keyword.value || undefined,
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
  linkModalApi.setData({ onSaved: load }).open();
}

function openEdit(row: any) {
  linkModalApi.setData({ onSaved: load, record: row }).open();
}

function onDelete(row: any) {
  Modal.confirm({
    content: `确认删除友链「${row.linkName}」？`,
    onOk: async () => {
      await deleteLinkApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

async function onToggle(row: any, checked: boolean) {
  await updateLinkStatusApi(String(row.id), checked ? 1 : 0);
  message.success(checked ? '已显示' : '已隐藏');
  load();
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mb-3 flex items-center gap-2">
      <Select
        v-model:value="isShow"
        :options="FILTER_OPTIONS"
        class="w-32"
        @change="onSearch"
      />
      <Input
        v-model:value="keyword"
        allow-clear
        class="w-56"
        placeholder="网站名称"
        @press-enter="onSearch"
      />
      <Button @click="onSearch">搜索</Button>
      <Button
        v-if="hasAccessByCodes(['/api/system/links/save'])"
        class="ml-auto"
        type="primary"
        @click="openAdd"
      >
        新增友链
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
        <template v-if="column.key === 'linkUrl'">
          <a
            :href="record.linkUrl"
            class="text-xs"
            target="_blank"
            rel="noopener"
          >
            {{ record.linkUrl }}
          </a>
        </template>
        <template v-else-if="column.key === 'type'">
          {{ record.type === 1 ? 'LOGO' : '文字' }}
        </template>
        <template v-else-if="column.key === 'isShow'">
          <Switch
            :checked="Number(record.isShow) === 1"
            :disabled="!hasAccessByCodes(['/api/system/links/save'])"
            size="small"
            @change="(checked: any) => onToggle(record, Boolean(checked))"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="hasAccessByCodes(['/api/system/links/save'])"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="openEdit(record)"
          >
            编辑
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/links/delete'])"
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
    <LinkModalComp />
  </Page>
</template>
