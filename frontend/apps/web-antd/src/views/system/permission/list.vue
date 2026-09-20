<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { PermissionRow } from '#/api/core/system';

import { Page } from '@vben/common-ui';
import { useAccess } from '@vben/access';

import { Button, message, Modal } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deletePermissionApi,
  getPermissionListApi,
  syncPermissionsApi,
} from '#/api/core/system';

defineOptions({ name: 'SystemPermission' });

const { hasAccessByCodes } = useAccess();

const gridOptions: VxeTableGridOptions<PermissionRow> = {
  columns: [
    { title: '序号', type: 'seq', width: 60 },
    { field: 'actionKey', title: '权限节点（action_key）' },
    { field: 'controller', title: '所属控制器' },
    { field: 'remark', title: '备注' },
    { field: 'action', fixed: 'right', slots: { default: 'action' }, title: '操作', width: 100 },
  ],
  height: 'auto',
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        const res = await getPermissionListApi({ p: page.currentPage });
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

async function onSync() {
  await syncPermissionsApi();
  message.success('同步完成');
  gridApi.query();
}

function onDelete(row: PermissionRow) {
  Modal.confirm({
    content: `确定删除权限节点「${row.actionKey}」吗？已勾选该权限的角色组将失去对应访问。`,
    onOk: async () => {
      await deletePermissionApi(row.id);
      message.success('删除成功');
      gridApi.query();
    },
    title: '删除确认',
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="权限节点列表">
      <template #toolbar-tools>
        <Button
          v-if="hasAccessByCodes(['/system/admin/permission_sync'])"
          class="mr-2"
          type="primary"
          @click="onSync"
        >
          同步权限
        </Button>
      </template>
      <template #action="{ row }">
        <Button
          v-if="hasAccessByCodes(['/system/admin/permission_del'])"
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
  </Page>
</template>
