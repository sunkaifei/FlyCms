<script lang="ts" setup>
import type { DbBackupFile } from '#/api/core/tools';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, message, Modal, Table, Tag } from 'ant-design-vue';

import {
  createDbBackupApi,
  dbBackupDownloadUrl,
  deleteDbBackupApi,
  getDbBackupListApi,
} from '#/api/core/tools';

defineOptions({ name: 'SystemDbBackup' });

const { hasAccessByCodes } = useAccess();
const canBackup = hasAccessByCodes(['/api/system/tools/db/backup']);
const canDownload = hasAccessByCodes(['/api/system/tools/db/download']);
const canDelete = hasAccessByCodes(['/api/system/tools/db/delete']);

const rows = ref<DbBackupFile[]>([]);
const loading = ref(false);
const backingUp = ref(false);

const columns = [
  { title: '备份文件', dataIndex: 'name', key: 'name' },
  { title: '大小', key: 'size', width: 110 },
  { title: '备份时间', key: 'time', width: 180 },
  { title: '操作', key: 'action', width: 170 },
];

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
}

function formatTime(ms: number) {
  if (!ms) return '-';
  const d = new Date(ms);
  const p = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ` +
    `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  );
}

async function load() {
  loading.value = true;
  try {
    rows.value = (await getDbBackupListApi()) ?? [];
  } finally {
    loading.value = false;
  }
}

function onBackup() {
  Modal.confirm({
    content: '将导出全部表结构与数据为 SQL 文件，数据量大时需要一些时间，继续？',
    onOk: async () => {
      backingUp.value = true;
      try {
        const file = await createDbBackupApi();
        message.success(`备份完成：${file?.name ?? ''}`);
        await load();
      } finally {
        backingUp.value = false;
      }
    },
    title: '执行备份',
  });
}

function onDownload(row: DbBackupFile) {
  window.open(dbBackupDownloadUrl(row.name));
}

function onDelete(row: DbBackupFile) {
  Modal.confirm({
    content: `删除备份「${row.name}」？删除后不可恢复。`,
    onOk: async () => {
      await deleteDbBackupApi(row.name);
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}

onMounted(load);
</script>

<template>
  <Page title="数据库备份" description="全库结构 + 数据导出为 SQL 文件（存储于服务器 backup/ 目录）；还原请下载后使用 mysql 客户端导入。">
    <div class="mb-3 flex items-center gap-2">
      <Button
        v-if="canBackup"
        :loading="backingUp"
        type="primary"
        @click="onBackup"
      >
        执行备份
      </Button>
      <Button @click="load">刷新</Button>
      <Tag v-if="!canBackup" color="orange">无备份权限，仅可查看</Tag>
    </div>

    <Table
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      row-key="name"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'size'">
          {{ formatSize((record as DbBackupFile).sizeBytes) }}
        </template>
        <template v-else-if="column.key === 'time'">
          {{ formatTime((record as DbBackupFile).backupTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="canDownload"
            size="small"
            type="link"
            @click="onDownload(record as DbBackupFile)"
          >
            下载
          </Button>
          <Button
            v-if="canDelete"
            danger
            size="small"
            type="link"
            @click="onDelete(record as DbBackupFile)"
          >
            删除
          </Button>
        </template>
      </template>
    </Table>
  </Page>
</template>
