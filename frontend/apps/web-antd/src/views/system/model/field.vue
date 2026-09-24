<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { ModelFieldRow } from '#/api/core/model';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, message } from 'ant-design-vue';
import { useRoute } from 'vue-router';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteFieldApi, getFieldListApi } from '#/api/core/model';

import FieldModal from './field-modal.vue';

defineOptions({ name: 'SystemModelField' });

const { hasAccessByCodes } = useAccess();
const route = useRoute();
// 雪花 ID 必须保持字符串（Number 化会丢末位精度，导致字段列表查询落空）
const modelId = route.params.modelId as string;

const [FieldModalComp, fieldModalApi] = useEditDrawer({
  connectedComponent: FieldModal,
  destroyOnClose: true,
});

const gridOptions: VxeTableGridOptions<ModelFieldRow> = {
  columns: [
    { title: '序号', type: 'seq', width: 60 },
    { field: 'fieldName', title: '字段名' },
    { field: 'fieldLabel', title: '显示名' },
    { field: 'fieldType', title: '类型', width: 100 },
    { field: 'columnType', title: '列定义' },
    { field: 'tabName', title: '表单选项卡', width: 100 },
    {
      field: 'isRequired',
      formatter: ({ cellValue }) => (cellValue === 1 ? '是' : '否'),
      title: '必填',
      width: 70,
    },
    {
      field: 'isList',
      formatter: ({ cellValue }) => (cellValue === 1 ? '是' : '否'),
      title: '列表显示',
      width: 90,
    },
    {
      field: 'isFilter',
      formatter: ({ cellValue }) => (cellValue === 1 ? '是' : '否'),
      title: '筛选',
      width: 70,
    },
    { field: 'sort', title: '排序', width: 70 },
    { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 140 },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async () => {
        const list = (await getFieldListApi(modelId)) ?? [];
        return { items: list, total: list.length };
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
  fieldModalApi
    .setData({ modelId, onSaved: () => gridApi.query() })
    .open();
}

function openEdit(row: ModelFieldRow) {
  fieldModalApi
    .setData({ onSaved: () => gridApi.query(), record: row })
    .open();
}

async function onDelete(row: ModelFieldRow) {
  await deleteFieldApi(row.id);
  message.success('字段已删除，数据列已移除');
  gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="模型字段（字段名即数据表列名，创建后不可改）">
      <template #toolbar-tools>
        <Button
          v-if="hasAccessByCodes(['/api/system/modelField/*'])"
          class="mr-2"
          type="primary"
          @click="openAdd"
        >
          新增字段
        </Button>
      </template>
      <template #action="{ row }">
        <Button
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-if="hasAccessByCodes(['/api/system/modelField/*'])"
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
    <FieldModalComp />
  </Page>
</template>
