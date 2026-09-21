<script lang="ts" setup>
import type { AdminLogRow } from '#/api/core/adminlog';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { Button, Input, Pagination, Table } from 'ant-design-vue';

import { getAdminLogApi } from '#/api/core/adminlog';

defineOptions({ name: 'SystemAdminLog' });

const rows = ref<AdminLogRow[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);

const adminName = ref('');
const path = ref('');
const startTime = ref('');
const endTime = ref('');

const columns = [
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '管理员', dataIndex: 'adminName', key: 'adminName', width: 110 },
  { title: '方法', dataIndex: 'method', key: 'method', width: 70 },
  { title: '路径', dataIndex: 'path', key: 'path' },
  { title: 'IP', dataIndex: 'ip', key: 'ip', width: 130 },
  { title: '响应码', dataIndex: 'status', key: 'status', width: 80 },
  { title: '耗时(ms)', dataIndex: 'costMs', key: 'costMs', width: 90 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getAdminLogApi({
      adminName: adminName.value || undefined,
      endTime: endTime.value || undefined,
      p: page.value,
      path: path.value || undefined,
      startTime: startTime.value || undefined,
    });
    rows.value = res.list ?? [];
    total.value = res.count ?? 0;
  } finally {
    loading.value = false;
  }
}

function reset() {
  adminName.value = '';
  path.value = '';
  startTime.value = '';
  endTime.value = '';
  page.value = 1;
  load();
}

onMounted(load);
</script>

<template>
  <Page>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Input
        v-model:value="adminName"
        allow-clear
        class="w-[150px]"
        placeholder="管理员"
      />
      <Input
        v-model:value="path"
        allow-clear
        class="w-[240px]"
        placeholder="路径包含，如 /api/system/model"
        @press-enter="
          () => {
            page = 1;
            load();
          }
        "
      />
      <Input
        v-model:value="startTime"
        class="w-[150px]"
        placeholder="开始日期 yyyy-MM-dd"
      />
      <Input
        v-model:value="endTime"
        class="w-[150px]"
        placeholder="结束日期 yyyy-MM-dd"
      />
      <Button
        type="primary"
        @click="
          () => {
            page = 1;
            load();
          }
        "
      >
        查询
      </Button>
      <Button @click="reset">重置</Button>
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
        <template v-if="column.key === 'path'">
          <code class="text-xs">{{ record.path }}</code>
          <div v-if="record.query" class="text-xs text-gray-400">
            {{ record.query }}
          </div>
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
