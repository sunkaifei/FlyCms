<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { GroupRow } from '#/api/core/system';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, message, Modal } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteGroupApi, getGroupListApi } from '#/api/core/system';

import AssignModal from './assign-modal.vue';
import EditModal from './edit-modal.vue';

defineOptions({ name: 'SystemGroup' });

const { hasAccessByCodes } = useAccess();

const [EditModalComp, editModalApi] = useEditDrawer({
  connectedComponent: EditModal,
  destroyOnClose: true,
});

const [AssignModalComp, assignModalApi] = useEditDrawer({
  connectedComponent: AssignModal,
  destroyOnClose: true,
});

const gridOptions: VxeTableGridOptions<GroupRow> = {
  columns: [
    { title: '序号', type: 'seq', width: 60 },
    { field: 'id', title: 'ID', width: 220 },
    { field: 'name', title: '角色组名' },
    { field: 'createAt', formatter: 'formatDateTime', title: '创建时间' },
    { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 240 },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async () => {
        const list = (await getGroupListApi()) ?? [];
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
  editModalApi.setData({ onSaved: () => gridApi.query() }).open();
}

function openEdit(row: GroupRow) {
  editModalApi.setData({ onSaved: () => gridApi.query(), record: row }).open();
}

function openAssign(row: GroupRow) {
  assignModalApi
    .setData({ group: row, onSaved: () => gridApi.query() })
    .open();
}

function onDelete(row: GroupRow) {
  if (row.id === 1) {
    message.warning('超级管理员组不能删除');
    return;
  }
  Modal.confirm({
    content: `确定删除角色组「${row.name}」吗？其权限绑定会一并清除。`,
    onOk: async () => {
      await deleteGroupApi(row.id);
      message.success('删除成功');
      gridApi.query();
    },
    title: '删除确认',
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="角色组列表">
      <template #toolbar-tools>
        <Button
          v-if="hasAccessByCodes(['/system/admin/add_group_save'])"
          class="mr-2"
          type="primary"
          @click="openAdd"
        >
          新增角色组
        </Button>
      </template>
      <template #action="{ row }">
        <Button
          v-if="hasAccessByCodes(['/system/admin/group_assignPermissions/*'])"
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openAssign(row)"
        >
          分配权限
        </Button>
        <Button
          v-if="hasAccessByCodes(['/system/admin/update_group_save'])"
          class="mr-2 px-2"
          size="small"
          type="link"
          @click="openEdit(row)"
        >
          编辑
        </Button>
        <Button
          v-if="hasAccessByCodes(['/system/admin/group_del'])"
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
    <AssignModalComp />
  </Page>
</template>
