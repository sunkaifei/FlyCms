<script lang="ts" setup>
import type { VbenFormProps } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { AdminRow } from '#/api/core/system';

import { Page, useVbenModal } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, message, Modal } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteAdminApi, getAdminListApi } from '#/api/core/system';

import EditModal from './edit-modal.vue';

defineOptions({ name: 'SystemAdmin' });

const { hasAccessByCodes } = useAccess();

const [EditModalComp, editModalApi] = useVbenModal({
  connectedComponent: EditModal,
  destroyOnClose: true,
});

const formOptions: VbenFormProps = {
  schema: [
    {
      component: 'Input',
      componentProps: { placeholder: '按账号搜索' },
      fieldName: 'adminName',
      label: '账号',
    },
  ],
  submitOnChange: false,
  submitOnEnter: true,
};

const gridOptions: VxeTableGridOptions<AdminRow> = {
  columns: [
    { title: '序号', type: 'seq', width: 60 },
    { field: 'adminName', title: '账号' },
    { field: 'nickName', title: '昵称' },
    { field: 'mobile', title: '手机号' },
    { field: 'email', title: '邮箱' },
    { field: 'lastLoginTime', formatter: 'formatDateTime', title: '最后登录' },
    { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 160 },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues) => {
        const res = await getAdminListApi({
          adminName: formValues?.adminName,
          p: page.currentPage,
        });
        return { items: res.list ?? [], total: res.count ?? 0 };
      },
    },
  },
  toolbarConfig: {
    refresh: true,
    zoom: true,
  },
};

const [Grid, gridApi] = useVbenVxeGrid({ formOptions, gridOptions });

function openAdd() {
  editModalApi.setData({}).open();
}

function openEdit(row: AdminRow) {
  editModalApi.setData({ record: row }).open();
}

function onDelete(row: AdminRow) {
  if (row.id === 1) {
    message.warning('超级管理员不能删除');
    return;
  }
  Modal.confirm({
    content: `确定删除管理员「${row.adminName}」吗？`,
    onOk: async () => {
      await deleteAdminApi(row.id);
      message.success('删除成功');
      gridApi.query();
    },
    title: '删除确认',
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="管理员列表">
      <template #toolbar-tools>
        <Button
          v-if="hasAccessByCodes(['/system/admin/admin_save'])"
          class="mr-2"
          type="primary"
          @click="openAdd"
        >
          新增管理员
        </Button>
      </template>
      <template #action="{ row }">
        <Button
          v-if="hasAccessByCodes(['/system/admin/admin_act'])"
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-if="hasAccessByCodes(['/system/admin/delAdmin'])"
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
