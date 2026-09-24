<script lang="ts" setup>
import type { TemplateVersionRow } from '#/api/core/template';

import { onMounted, ref } from 'vue';

import { useEditDrawer } from '#/utils/edit-drawer';

import { Button, message, Popconfirm, Table } from 'ant-design-vue';

import { getTemplateVersionsApi, restoreTemplateApi } from '#/api/core/template';

/**
 * D1 版本历史与回滚。
 *
 * 每次保存都会先快照再落盘，所以"改坏模板"永远有退路：
 * 回滚不是删除历史，而是把历史内容作为新版本写入 —— 可以反复来回。
 */
let onRestored: (() => void) | undefined;
let file = '';

const [Modal, modalApi] = useEditDrawer({
  class: 'w-[60%]',
  title: '版本历史',
});

const rows = ref<TemplateVersionRow[]>([]);
const total = ref(0);
const loading = ref(false);

const columns = [
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '编辑者', dataIndex: 'editorId', key: 'editorId', width: 120 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 100 },
];

async function load() {
  loading.value = true;
  try {
    const res = await getTemplateVersionsApi(file);
    rows.value = res.versions ?? [];
    total.value = res.total ?? rows.value.length;
  } finally {
    loading.value = false;
  }
}

async function onRestore(row: any) {
  await restoreTemplateApi(file, row.version);
  message.success(`已回滚到版本 ${row.version}（历史未丢失）`);
  onRestored?.();
  modalApi.close();
}

onMounted(() => {
  const data = modalApi.getData() as
    | { file?: string; onRestored?: () => void }
    | undefined;
  file = data?.file ?? '';
  onRestored = data?.onRestored;
  modalApi.setState({ title: `版本历史 · ${file}` });
  load();
});
</script>

<template>
  <Modal>
    <div class="mb-2 text-sm text-gray-400">
      共 {{ total }} 个版本。回滚会把该版本内容作为<b>新版本</b>写入，历史记录不会被删除。
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
        <template v-if="column.key === 'version'"> v{{ record.version }} </template>
        <template v-else-if="column.key === 'remark'">
          {{ record.remark || '-' }}
        </template>
        <template v-else-if="column.key === 'editorId'">
          {{ record.editorId || '-' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <Popconfirm
            :title="`回滚到 v${record.version}？`"
            ok-text="回滚"
            @confirm="() => onRestore(record)"
          >
            <Button size="small" type="link">回滚</Button>
          </Popconfirm>
        </template>
      </template>
    </Table>
  </Modal>
</template>
