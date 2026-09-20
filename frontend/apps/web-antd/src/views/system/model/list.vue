<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { ModelRow } from '#/api/core/model';

import { Page, useVbenModal } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, message, Modal } from 'ant-design-vue';
import { useRouter } from 'vue-router';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteModelApi, getModelListApi } from '#/api/core/model';

import EditModal from './edit-modal.vue';

defineOptions({ name: 'SystemModel' });

const { hasAccessByCodes } = useAccess();
const router = useRouter();

const [EditModalComp, editModalApi] = useVbenModal({
  connectedComponent: EditModal,
  destroyOnClose: true,
});

const gridOptions: VxeTableGridOptions<ModelRow> = {
  columns: [
    { title: '序号', type: 'seq', width: 60 },
    { field: 'name', title: '模型名称' },
    { field: 'code', title: '标识' },
    { field: 'titleLabel', title: '标题字段名' },
    { field: 'sort', title: '排序', width: 80 },
    {
      field: 'isSystem',
      formatter: ({ cellValue }) => (cellValue === 1 ? '内置' : '自定义'),
      title: '类型',
      width: 90,
    },
    {
      field: 'status',
      formatter: ({ cellValue }) => (cellValue === 1 ? '启用' : '禁用'),
      title: '状态',
      width: 80,
    },
    { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 320 },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        const res = await getModelListApi({ p: page.currentPage });
        return { items: res.list ?? [], total: res.count ?? 0 };
      },
    },
  },
  toolbarConfig: {
    refresh: true,
    zoom: true,
  },
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions });

function openAdd() {
  editModalApi.setData({ onSaved: () => gridApi.query() }).open();
}

function openEdit(row: ModelRow) {
  editModalApi.setData({ onSaved: () => gridApi.query(), record: row }).open();
}

function openFields(row: ModelRow) {
  router.push('/system/model/field/' + row.id);
}

function openData(row: ModelRow) {
  router.push('/modelData/' + row.code);
}

function onDelete(row: ModelRow) {
  Modal.confirm({
    content: `删除模型「${row.name}」将级联删除其数据表与全部字段定义，且不可恢复！`,
    onOk: async () => {
      await deleteModelApi(row.id);
      message.success('已删除');
      gridApi.query();
    },
    title: '删除确认',
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="内容模型">
      <template #toolbar-tools>
        <Button
          v-if="hasAccessByCodes(['/api/system/model/save'])"
          class="mr-2"
          type="primary"
          @click="openAdd"
        >
          新增模型
        </Button>
      </template>
      <template #action="{ row }">
        <Button class="mr-2 px-2" size="small" type="link" @click="openFields(row)">
          字段管理
        </Button>
        <Button class="mr-2 px-2" size="small" type="link" @click="openData(row)">
          内容管理
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/model/update'])"
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/model/del']) && row.isSystem !== 1"
          class="px-2"
          danger
          size="small"
          type="link"
          @click="onDelete(row)"
        >
          删除
        </Button>
      </template>
    </Grid>
    <EditModalComp />
  </Page>
</template>
