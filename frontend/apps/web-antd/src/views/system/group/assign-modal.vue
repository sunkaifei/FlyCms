<script lang="ts" setup>
import type { MenuNode } from '#/api/core/menu-manage';

import { computed, onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Empty, message, Spin, Tree } from 'ant-design-vue';

import { assignGroupPermissionsApi, getGroupPermissionIdsApi } from '#/api/core/system';
import { getMenuListApi } from '#/api/core/menu-manage';

/**
 * 角色组分配权限（若依式）：级联勾选菜单树（M/C/F）。
 * 提交 = 勾选节点 + 半选父节点 的 permission id 全量保存，后端做增删差量。
 * 超级管理员组（id=1）后端硬编码保护，前端直接禁用提交。
 */
const groupId = ref(0);
const groupName = ref('');
const loading = ref(false);
const expandedKeys = ref<number[]>([]);
const checkedKeys = ref<number[]>([]);
const halfCheckedKeys = ref<number[]>([]);
let allNodes: MenuNode[] = [];
let onSaved: (() => void) | undefined;

interface TreeRow extends MenuNode {
  children?: TreeRow[];
  key: number;
  title: string;
}

const treeData = computed<TreeRow[]>(() => {
  const map = new Map<number, TreeRow>();
  for (const n of allNodes) {
    map.set(n.id, {
      ...n,
      children: undefined,
      key: n.id,
      title:
        (n.menuName || n.actionKey || n.id) +
        (n.menuType === 'F' && n.actionKey ? `（${n.actionKey}）` : ''),
    });
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

const isSuperGroup = computed(() => groupId.value === 1);

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    if (isSuperGroup.value) {
      message.warning('超级管理员组权限不能修改');
      return;
    }
    modalApi.lock();
    try {
      const all = [...checkedKeys.value, ...halfCheckedKeys.value];
      await assignGroupPermissionsApi(groupId.value, all);
      message.success('保存成功');
      onSaved?.();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  title: '分配权限（菜单树）',
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function onCheck(_checked: any, info: any) {
  checkedKeys.value = (info.checked ?? []) as number[];
  halfCheckedKeys.value = (info.halfChecked ?? []) as number[];
}

onMounted(async () => {
  const data = modalApi.getData() as
    | { group: { id: number; name: string }; onSaved?: () => void }
    | undefined;
  onSaved = data?.onSaved;
  groupId.value = data?.group?.id ?? 0;
  groupName.value = data?.group?.name ?? '';

  loading.value = true;
  try {
    const [nodes, ids] = await Promise.all([
      getMenuListApi(),
      getGroupPermissionIdsApi(groupId.value),
    ]);
    allNodes = nodes ?? [];
    const childIds = new Set(allNodes.map((n) => n.parentId));
    expandedKeys.value = allNodes
      .filter((n) => n.menuType === 'M' && n.visible === 1)
      .map((n) => n.id);
    // 初始只勾叶子（父节点状态由树级联推导）
    checkedKeys.value = (ids ?? []).filter(
      (id) => !childIds.has(id) && allNodes.some((n) => n.id === id),
    );
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <Modal>
    <Spin :spinning="loading">
      <p class="mb-2 text-sm">
        为「{{ groupName }}」勾选可用的菜单与操作（按钮节点即前端权限码，未勾选则接口 403）：
      </p>
      <Empty v-if="!loading && treeData.length === 0" description="暂无菜单节点" />
      <div v-else class="max-h-[60vh] overflow-y-auto">
        <Tree
          v-model:checkedKeys="checkedKeys"
          :expanded-keys="expandedKeys"
          :tree-data="treeData"
          checkable
          default-expand-all
          @check="onCheck"
        />
      </div>
    </Spin>
  </Modal>
</template>
