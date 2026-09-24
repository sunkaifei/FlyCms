<script lang="ts" setup>
import type { FormDataRow } from '#/api/core/form';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import {
  Button,
  Descriptions,
  Input,
  message,
  Modal,
  Pagination,
  Select,
  Table,
} from 'ant-design-vue';

import {
  auditFormDataApi,
  deleteFormDataApi,
  exportFormDataUrl,
  getFormDataDetailApi,
  getFormDataPageApi,
  getFormPageApi,
} from '#/api/core/form';

defineOptions({ name: 'SystemFormData' });

const { hasAccessByCodes } = useAccess();

const STATUS_OPTIONS = [
  { label: '全部', value: -1 },
  { label: '待处理', value: 1 },
  { label: '已处理', value: 2 },
  { label: '未通过', value: 0 },
];

const STATUS_TAG: Record<number, string> = {
  0: '未通过',
  1: '待处理',
  2: '已处理',
};

const rows = ref<FormDataRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const formId = ref('');
const status = ref(-1);
const createTime = ref('');
const formOptions = ref<{ label: string; value: string }[]>([]);

const detailVisible = ref(false);
const detail = ref<Record<string, unknown>>({});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 170 },
  { title: '所属表单', dataIndex: 'formName', key: 'formName', width: 160 },
  { title: '提交IP', dataIndex: 'ip', key: 'ip', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 200 },
];

async function loadForms() {
  const res = await getFormPageApi({ p: 1, rows: 100 });
  formOptions.value = (res.list ?? []).map((f) => ({
    label: f.formName,
    value: String(f.id),
  }));
}

async function load() {
  loading.value = true;
  try {
    const res = await getFormDataPageApi({
      createTime: createTime.value || undefined,
      formId: formId.value || undefined,
      p: page.value,
      rows: 20,
      status: status.value >= 0 ? status.value : undefined,
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

async function onView(row: any) {
  detail.value = await getFormDataDetailApi(String(row.id));
  detailVisible.value = true;
}

async function onAudit(row: any, s: number) {
  await auditFormDataApi(String(row.id), s);
  message.success(s === 2 ? '已标记处理' : '已标记未通过');
  load();
}

function onDelete(row: any) {
  Modal.confirm({
    content: '确认删除该条提交数据？',
    onOk: async () => {
      await deleteFormDataApi(String(row.id));
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

function onExport() {
  window.open(
    exportFormDataUrl({
      createTime: createTime.value || undefined,
      formId: formId.value || undefined,
      status: status.value >= 0 ? status.value : undefined,
    }),
    '_blank',
  );
}

onMounted(async () => {
  await loadForms();
  load();
});
</script>

<template>
  <Page>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="formId"
        :options="formOptions"
        allow-clear
        class="w-48"
        placeholder="全部表单"
        @change="onSearch"
      />
      <Select
        v-model:value="status"
        :options="STATUS_OPTIONS"
        class="w-32"
        @change="onSearch"
      />
      <Input
        v-model:value="createTime"
        allow-clear
        class="w-40"
        placeholder="日期如 2026-09-24"
        @press-enter="onSearch"
      />
      <Button @click="onSearch">搜索</Button>
      <Button
        v-if="hasAccessByCodes(['/api/system/formData/export'])"
        class="ml-auto"
        size="small"
        @click="onExport"
      >
        导出 CSV
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
        <template v-if="column.key === 'status'">
          <span :class="record.status === 2 ? 'text-green-600' : 'text-orange-500'">
            {{ STATUS_TAG[record.status] ?? record.status }}
          </span>
        </template>
        <template v-else-if="column.key === 'action'">
          <Button class="mr-1 px-2" size="small" type="link" @click="onView(record)">
            查看
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/formData/audit'])"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="onAudit(record, 2)"
          >
            标记已处理
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/formData/delete'])"
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

    <Modal v-model:open="detailVisible" :footer="null" title="提交详情" width="720px">
      <Descriptions :column="2" bordered size="small" class="mb-3">
        <Descriptions-item label="所属表单">
          {{ detail.formName }}
        </Descriptions-item>
        <Descriptions-item label="提交IP">{{ detail.ip }}</Descriptions-item>
        <Descriptions-item label="提交时间">
          {{ detail.createTime }}
        </Descriptions-item>
        <Descriptions-item label="状态">
          {{ STATUS_TAG[detail.status as number] ?? detail.status }}
        </Descriptions-item>
      </Descriptions>
      <Descriptions :column="1" bordered size="small">
        <Descriptions-item
          v-for="(v, k) in (detail.values as Record<string, unknown>) || {}"
          :key="k"
          :label="String(k)"
        >
          {{ v }}
        </Descriptions-item>
      </Descriptions>
    </Modal>
  </Page>
</template>
