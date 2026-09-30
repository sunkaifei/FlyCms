<script lang="ts" setup>
import type { MenuNode } from '#/api/core/menu-manage';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useEditDrawer } from '#/utils/edit-drawer';
import { useAccess } from '@vben/access';

import { Button, message, Modal, Table } from 'ant-design-vue';

import { deleteMenuApi, getMenuListApi } from '#/api/core/menu-manage';

import MenuModal from './menu-modal.vue';

defineOptions({ name: 'SystemMenu' });

const { hasAccessByCodes } = useAccess();

const [MenuModalComp, menuModalApi] = useEditDrawer({
  connectedComponent: MenuModal,
  destroyOnClose: true,
});

interface TreeRow extends MenuNode {
  children?: TreeRow[];
}

const nodes = ref<MenuNode[]>([]);

const TYPE_LABEL: Record<string, string> = {
  C: '菜单',
  F: '按钮',
  M: '目录',
};

const treeData = computed<TreeRow[]>(() => {
  const map = new Map<number, TreeRow>();
  for (const n of nodes.value) {
    map.set(n.id, { ...n });
  }
  const roots: TreeRow[] = [];
  for (const row of map.values()) {
    const parent = row.parentId ? map.get(row.parentId) : undefined;
    if (parent) {
      (parent.children ??= []).push(row);
    } else {
      roots.push(row);
    }
  }
  return roots;
});

async function load() {
  nodes.value = (await getMenuListApi()) ?? [];
}

onMounted(load);

function openAdd(row?: any) {
  menuModalApi
    .setData({ parentId: row?.id ?? 0, onSaved: load })
    .open();
}

function openEdit(row: any) {
  menuModalApi.setData({ onSaved: load, record: row }).open();
}

function onDelete(row: any) {
  Modal.confirm({
    content: `删除「${row.menuName || row.actionKey}」？其角色绑定会一并移除。`,
    onOk: async () => {
      await deleteMenuApi(row.id);
      message.success('已删除');
      load();
    },
    title: '删除确认',
  });
}
</script>

<template>
  <Page>
    <div class="mb-2 flex items-center gap-2">
      <Button
        v-if="hasAccessByCodes(['/api/system/menu/save'])"
        type="primary"
        @click="() => openAdd()"
      >
        新增根目录
      </Button>
      <span class="text-sm text-gray-400">
        M=目录 / C=菜单（挂授权锚点）/ F=按钮与接口（权限码）。F 节点由"同步权限"自动登记，可在此编排层级。
      </span>
    </div>
    <Table
      :columns="[
        { title: '名称', dataIndex: 'menuName', key: 'menuName', width: 220 },
        { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 80 },
        { title: '权限标识 actionKey', dataIndex: 'actionKey', key: 'actionKey' },
        { title: '路由路径', dataIndex: 'path', key: 'path' },
        { title: '组件', dataIndex: 'component', key: 'component' },
        { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
        { title: '操作', key: 'action', width: 220 },
      ]"
      :data-source="treeData"
      :pagination="false"
      default-expand-all-rows
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'menuName'">
          <span class="font-medium">{{ record.menuName || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'menuType'">
          {{ TYPE_LABEL[record.menuType] }}
        </template>
        <template v-else-if="column.key === 'actionKey'">
          <code v-if="record.actionKey" class="text-xs">{{ record.actionKey }}</code>
          <span v-else class="text-gray-300">-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <Button
            v-if="record.menuType !== 'F'"
            class="mr-1 px-2"
            size="small"
            type="link"
            @click="() => openAdd(record)"
          >
            添加下级
          </Button>
          <Button class="mr-1 px-2" size="small" type="link" @click="() => openEdit(record)">
            编辑
          </Button>
          <Button
            v-if="hasAccessByCodes(['/api/system/menu/del'])"
            class="px-2"
            danger
            size="small"
            type="link"
            @click="() => onDelete(record)"
          >
            删除
          </Button>
        </template>
        <template v-else>
          {{ record[column.key as string] || '-' }}
        </template>
      </template>
    </Table>
    <MenuModalComp />
  </Page>
</template>
